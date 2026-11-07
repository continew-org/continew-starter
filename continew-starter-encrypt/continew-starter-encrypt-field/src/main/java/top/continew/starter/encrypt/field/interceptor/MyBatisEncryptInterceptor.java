/*
 * Copyright (c) 2022-present Charles7c Authors. All Rights Reserved.
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package top.continew.starter.encrypt.field.interceptor;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.util.EncryptHelper;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段加密拦截器
 *
 * <p>写入时对参数中的加密实体就地加密，写入完成后恢复原值，支持实体、{@code et}/{@code ew}、
 * 集合、数组与自定义 {@code @Param} 别名等参数形态。</p>
 *
 * @author Charles7c
 * @author lishuyan
 * @since 1.4.0
 */
@Intercepts({@Signature(type = Executor.class, method = "update", args = {MappedStatement.class,
    Object.class}),
    @Signature(type = Executor.class, method = "query", args = {MappedStatement.class,
        Object.class, RowBounds.class, ResultHandler.class}),
    @Signature(type = Executor.class,
        method = "query",
        args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
            CacheKey.class, BoundSql.class})})
public class MyBatisEncryptInterceptor extends AbstractMyBatisInterceptor implements Interceptor {

    private static final Pattern PARAM_PAIRS_PATTERN = Pattern
        .compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
        Object parameterObject = invocation.getArgs()[1];
        if (parameterObject == null) {
            return invocation.proceed();
        }
        SqlCommandType commandType = mappedStatement.getSqlCommandType();
        if (commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE) {
            return this.interceptWrite(invocation, mappedStatement, parameterObject);
        }
        if (commandType == SqlCommandType.SELECT) {
            return this.interceptSelect(invocation, mappedStatement, parameterObject);
        }
        return invocation.proceed();
    }

    /**
     * 写入路径：参数加密后执行写入，写入完成后恢复参数原值，保证调用方实体保持明文，
     * 同一实体重复写入不会二次加密
     *
     * @param invocation      调用
     * @param mappedStatement 映射语句
     * @param parameterObject 参数
     * @return 写入结果
     * @throws Throwable 写入异常
     */
    private Object interceptWrite(Invocation invocation,
        MappedStatement mappedStatement,
        Object parameterObject) throws Throwable {
        CryptoSnapshot snapshot = this.encryptInParameter(parameterObject, mappedStatement);
        try {
            return invocation.proceed();
        } finally {
            snapshot.restore();
        }
    }

    /**
     * 查询路径：加密查询参数（参数级注解与实体参数场景），查询完成后恢复参数原值
     *
     * @param invocation      调用
     * @param mappedStatement 映射语句
     * @param parameterObject 参数
     * @return 查询结果
     * @throws Throwable 查询异常
     */
    private Object interceptSelect(Invocation invocation,
        MappedStatement mappedStatement,
        Object parameterObject) throws Throwable {
        if (parameterObject instanceof Map parameterMap) {
            CryptoSnapshot snapshot = this.encryptQueryParameter(parameterMap, mappedStatement);
            try {
                return invocation.proceed();
            } finally {
                snapshot.restore();
            }
        }
        return invocation.proceed();
    }

    /**
     * 加密写入参数（就地改写并记录快照），加密失败时恢复已改写部分后抛出
     *
     * @param parameterObject 参数
     * @param mappedStatement 映射语句
     * @return 快照
     */
    private CryptoSnapshot encryptInParameter(Object parameterObject,
        MappedStatement mappedStatement) {
        CryptoSnapshot snapshot = new CryptoSnapshot();
        Set<Object> processed = Collections.newSetFromMap(new IdentityHashMap<>());
        try {
            this.processWriteParameter(parameterObject, mappedStatement, snapshot, processed);
        } catch (Exception e) {
            snapshot.restore();
            throw e;
        }
        return snapshot;
    }

    /**
     * 按参数形态分发加密：Wrapper、集合、数组与实体，基础类型直接跳过
     *
     * @param value           参数值
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @param processed       已处理对象（防同一次处理中重复加密与自引用循环）
     */
    private void processWriteParameter(Object value,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        Set<Object> processed) {
        if (ObjectUtil.isNull(value) || !processed.add(value)) {
            return;
        }
        if (value instanceof Map<?, ?> parameterMap) {
            for (Object entryValue : parameterMap.values()) {
                this.processWriteParameter(entryValue, mappedStatement, snapshot, processed);
            }
            return;
        }
        if (value instanceof AbstractWrapper wrapper) {
            this.encryptWrapper(wrapper, mappedStatement, snapshot);
            return;
        }
        if (ClassUtil.isBasicType(value.getClass()) || value instanceof String) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object element : collection) {
                this.processWriteParameter(element, mappedStatement, snapshot, processed);
            }
            return;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray() && !valueClass.getComponentType().isPrimitive()) {
            for (Object element : (Object[]) value) {
                this.processWriteParameter(element, mappedStatement, snapshot, processed);
            }
            return;
        }
        List<Field> fieldList = super.getEncryptFields(valueClass);
        if (fieldList.isEmpty()) {
            return;
        }
        this.encryptEntity(fieldList, value, snapshot);
    }

    /**
     * 加密实体字段（就地改写并记录快照）
     *
     * @param fieldList 加密字段列表
     * @param entity    实体
     * @param snapshot  快照
     */
    private void encryptEntity(List<Field> fieldList, Object entity, CryptoSnapshot snapshot) {
        for (Field field : fieldList) {
            Object fieldValue = ReflectUtil.getFieldValue(entity, field);
            if (fieldValue == null) {
                continue;
            }
            String strValue = String.valueOf(fieldValue);
            if (CharSequenceUtil.isBlank(strValue)) {
                continue;
            }
            FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
            snapshot.recordField(entity, field, fieldValue);
            ReflectUtil.setFieldValue(entity, field, EncryptHelper
                .encrypt(strValue, fieldEncrypt, field.getName()));
        }
    }

    /**
     * 加密 Wrapper 的 SET 参数（针对 MP 的 UpdateWrapper、LambdaUpdateWrapper 等参数）
     *
     * @param wrapper         Wrapper 参数
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @author wangshaopeng@talkweb.com.cn（<a
     *         href="https://blog.csdn.net/tianmaxingkonger/article/details/130986784">基于Mybatis-Plus拦截器实现MySQL数据加解密</a>）
     * @since 2.1.1
     */
    private void encryptWrapper(AbstractWrapper wrapper,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot) {
        String sqlSet = wrapper.getSqlSet();
        if (CharSequenceUtil.isBlank(sqlSet)) {
            return;
        }
        // 将 name=#{ew.paramNameValuePairs.xxx},age=#{ew.paramNameValuePairs.xxx} 切出来
        String[] elArr = sqlSet.split(StringConstants.COMMA);
        Map<String, String> propMap = new HashMap<>(elArr.length);
        Arrays.stream(elArr).forEach(el -> {
            String[] elPart = el.split(StringConstants.EQUAL_SIGN);
            propMap.put(elPart[0], elPart[1]);
        });
        // 获取加密字段
        Class<?> entityClass = this.getEntityClass(wrapper, mappedStatement);
        List<Field> encryptFieldList = super.getEncryptFields(entityClass);
        for (Field field : encryptFieldList) {
            FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
            String el = propMap.get(field.getName());
            if (CharSequenceUtil.isBlank(el)) {
                continue;
            }
            Matcher matcher = PARAM_PAIRS_PATTERN.matcher(el);
            if (matcher.matches()) {
                String valueKey = matcher.group(1);
                Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
                Object value = paramNameValuePairs.get(valueKey);
                if (ObjectUtil.isNull(value)) {
                    continue;
                }
                snapshot.recordMapEntry(paramNameValuePairs, valueKey, value);
                paramNameValuePairs.put(valueKey, this.doEncrypt(value, fieldEncrypt, field
                    .getName()));
            }
        }
    }

    /**
     * 加密查询参数（针对 Map 类型参数），参数级注解的 String 值与实体参数均记录快照
     *
     * @param parameterMap    参数
     * @param mappedStatement 映射语句
     * @return 快照
     */
    private CryptoSnapshot encryptQueryParameter(Map<String, Object> parameterMap,
        MappedStatement mappedStatement) {
        CryptoSnapshot snapshot = new CryptoSnapshot();
        Map<String, FieldEncrypt> encryptParameterMap = super.getEncryptParameters(mappedStatement);
        for (Map.Entry<String, Object> parameterEntrySet : parameterMap.entrySet()) {
            String parameterName = parameterEntrySet.getKey();
            Object parameterValue = parameterEntrySet.getValue();
            if (parameterValue == null || ClassUtil.isBasicType(parameterValue
                .getClass()) || parameterValue instanceof AbstractWrapper) {
                continue;
            }
            if (parameterValue instanceof String str) {
                FieldEncrypt fieldEncrypt = encryptParameterMap.get(parameterName);
                if (fieldEncrypt != null) {
                    snapshot.recordMapEntry(parameterMap, parameterName, parameterValue);
                    parameterMap.put(parameterName,
                        this.doEncrypt(str, fieldEncrypt, parameterName));
                }
            } else {
                // 实体参数
                this.encryptEntity(super.getEncryptFields(parameterValue), parameterValue,
                    snapshot);
            }
        }
        return snapshot;
    }

    /**
     * 处理加密
     *
     * @param parameterValue 参数值
     * @param fieldEncrypt   字段加密注解
     * @param fieldName      字段名称，用于失败时定位
     * @return 加密后的值
     */
    private Object doEncrypt(Object parameterValue, FieldEncrypt fieldEncrypt, String fieldName) {
        if (ObjectUtil.isNull(parameterValue)) {
            return null;
        }
        String strValue = String.valueOf(parameterValue);
        if (CharSequenceUtil.isBlank(strValue)) {
            return null;
        }
        return EncryptHelper.encrypt(strValue, fieldEncrypt, fieldName);
    }

    /**
     * 获取实体类
     *
     * @param wrapper         查询或更新包装器
     * @param mappedStatement 映射语句
     * @return 实体类
     */
    private Class<?> getEntityClass(AbstractWrapper wrapper, MappedStatement mappedStatement) {
        // 尝试从 Wrapper 中获取实体类
        Class<?> entityClass = wrapper.getEntityClass();
        if (entityClass != null) {
            return entityClass;
        }
        // 从映射语句中获取实体类
        return mappedStatement.getParameterMap().getType();
    }
}
