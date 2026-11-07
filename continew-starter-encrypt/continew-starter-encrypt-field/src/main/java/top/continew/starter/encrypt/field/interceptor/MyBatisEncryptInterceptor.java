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
import top.continew.starter.encrypt.field.exception.FieldEncryptException;
import top.continew.starter.encrypt.field.util.EncryptHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段加密拦截器
 *
 * <p>写入时对参数中的加密实体拷贝加密，加密结果只作用于落库参数，传入实体保持明文原值，
 * 支持实体、{@code et}/{@code ew}、集合、数组与自定义 {@code @Param} 别名等参数形态。</p>
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
     * 写入路径：参数加密后执行写入，加密作用于拷贝参数，调用方实体保持明文原值，
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
        CryptoSnapshot snapshot = new CryptoSnapshot();
        IdentityHashMap<Object, Object> copyCache = new IdentityHashMap<>();
        Object newParameter;
        try {
            newParameter = this.resolveValue(parameterObject, mappedStatement, snapshot, copyCache);
        } catch (Exception e) {
            snapshot.restore();
            throw e;
        }
        if (newParameter != parameterObject) {
            invocation.getArgs()[1] = newParameter;
        }
        try {
            return invocation.proceed();
        } finally {
            // Wrapper 的 paramNameValuePairs 在写入完成后恢复原值
            snapshot.restore();
        }
    }

    /**
     * 查询路径：加密查询参数（参数级注解的 String 值与实体参数），加密同样作用于拷贝参数
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
        if (!(parameterObject instanceof Map<?, ?> parameterMap)) {
            return invocation.proceed();
        }
        Map<String, FieldEncrypt> encryptParameterMap = super.getEncryptParameters(mappedStatement);
        Map<Object, Object> newParameterMap = new HashMap<>(parameterMap.size());
        IdentityHashMap<Object, Object> copyCache = new IdentityHashMap<>();
        for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
            newParameterMap.put(entry.getKey(), this.resolveQueryValue(entry.getKey(),
                entry.getValue(), encryptParameterMap, copyCache));
        }
        invocation.getArgs()[1] = newParameterMap;
        return invocation.proceed();
    }

    /**
     * 加密查询单个参数值：参数级注解的 String 值与直接实体替换为加密结果，其余原样保留
     *
     * @param parameterName       参数名
     * @param parameterValue      参数值
     * @param encryptParameterMap 参数级加密注解映射
     * @param copyCache           拷贝缓存
     * @return 处理后的参数值
     */
    private Object resolveQueryValue(Object parameterName,
        Object parameterValue,
        Map<String, FieldEncrypt> encryptParameterMap,
        IdentityHashMap<Object, Object> copyCache) {
        if (parameterValue instanceof String str) {
            FieldEncrypt fieldEncrypt = encryptParameterMap.get(String.valueOf(parameterName));
            if (fieldEncrypt != null) {
                return this.doEncrypt(str, fieldEncrypt, String.valueOf(parameterName));
            }
            return parameterValue;
        }
        if (parameterValue == null || ClassUtil.isBasicType(parameterValue
            .getClass()) || parameterValue instanceof AbstractWrapper) {
            return parameterValue;
        }
        List<Field> fieldList = super.getEncryptFields(parameterValue.getClass());
        if (fieldList.isEmpty()) {
            return parameterValue;
        }
        if (copyCache.containsKey(parameterValue)) {
            return copyCache.get(parameterValue);
        }
        Object copy = this.copyEntity(parameterValue);
        this.encryptEntity(fieldList, copy);
        copyCache.put(parameterValue, copy);
        return copy;
    }

    /**
     * 解析加密后的参数值：无替换对象时返回原值
     *
     * @param value           参数值
     * @param mappedStatement 映射语句
     * @param snapshot        快照（Wrapper 的 paramNameValuePairs 还原用）
     * @param copyCache       拷贝缓存（同一对象只拷贝与加密一次）
     * @return 加密后的参数值或原值
     */
    private Object resolveValue(Object value,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        Object processed = this.encryptWriteParameter(value, mappedStatement, snapshot, copyCache);
        return processed != null ? processed : value;
    }

    /**
     * 按参数形态加密：实体拷贝加密后返回新对象，Wrapper 就地加密（快照还原）返回 null，
     * Map、集合与数组替换为新容器，基础类型直接跳过
     *
     * @param value           参数值
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @param copyCache       拷贝缓存（防同一次处理中重复加密与自引用循环）
     * @return 加密后的新参数对象，无需替换时返回 null
     */
    private Object encryptWriteParameter(Object value,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        if (ObjectUtil.isNull(value)) {
            return null;
        }
        if (copyCache.containsKey(value)) {
            return copyCache.get(value);
        }
        if (value instanceof AbstractWrapper wrapper) {
            this.encryptWrapper(wrapper, mappedStatement, snapshot);
            return null;
        }
        if (ClassUtil.isBasicType(value.getClass()) || value instanceof String) {
            return null;
        }
        if (value instanceof Map<?, ?> parameterMap) {
            Map<Object, Object> newParameterMap = new HashMap<>(parameterMap.size());
            // 先登记新容器再遍历，自引用参数可命中缓存而不会无限递归
            copyCache.put(value, newParameterMap);
            for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
                newParameterMap.put(entry.getKey(), this.resolveValue(entry.getValue(),
                    mappedStatement, snapshot, copyCache));
            }
            return newParameterMap;
        }
        if (value instanceof Collection<?> collection) {
            List<Object> newCollection = new ArrayList<>(collection.size());
            copyCache.put(value, newCollection);
            for (Object element : collection) {
                newCollection.add(this.resolveValue(element, mappedStatement, snapshot, copyCache));
            }
            return newCollection;
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray() && !valueClass.getComponentType().isPrimitive()) {
            Object[] sourceArray = (Object[]) value;
            Object[] newArray = Arrays.copyOf(sourceArray, sourceArray.length);
            copyCache.put(value, newArray);
            for (int i = 0; i < newArray.length; i++) {
                Object processed = this.resolveValue(newArray[i], mappedStatement, snapshot,
                    copyCache);
                if (processed != newArray[i]) {
                    newArray[i] = processed;
                }
            }
            return newArray;
        }
        List<Field> fieldList = super.getEncryptFields(valueClass);
        if (fieldList.isEmpty()) {
            return null;
        }
        Object copy = this.copyEntity(value);
        this.encryptEntity(fieldList, copy);
        copyCache.put(value, copy);
        return copy;
    }

    /**
     * 拷贝实体：按字段复制到新实例（不依赖 getter/setter），拷贝失败时抛出异常，避免静默改写调用方实体
     *
     * @param entity 实体
     * @return 实体拷贝
     */
    private Object copyEntity(Object entity) {
        try {
            Object copy = ReflectUtil.newInstance(entity.getClass());
            for (Field field : ReflectUtil.getFields(entity.getClass())) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                ReflectUtil.setFieldValue(copy, field, ReflectUtil.getFieldValue(entity, field));
            }
            return copy;
        } catch (Exception e) {
            throw new FieldEncryptException(String.format("实体 [%s] 拷贝失败，无法安全加密写入参数",
                entity.getClass().getName()), e);
        }
    }

    /**
     * 加密实体字段（作用于拷贝实体）
     *
     * @param fieldList 加密字段列表
     * @param entity    实体拷贝
     */
    private void encryptEntity(List<Field> fieldList, Object entity) {
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
     * 处理加密，空串与空白串保持原值返回，避免被替换为 null
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
            return parameterValue;
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
