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
import org.apache.ibatis.binding.MapperMethod;
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

    /**
     * SQL SET 片段中「列名=占位符」的配对
     */
    private static final Pattern SQL_SET_PAIR_PATTERN = Pattern
        .compile("([\\w.`]++)\\s*+=\\s*+#\\{ew\\.paramNameValuePairs\\.(\\w++)\\}");

    /**
     * 查询条件段中「列名 操作符 占位符」的配对
     */
    private static final Pattern QUERY_CONDITION_PAIR = Pattern
        .compile(
            "([\\w.`]++)\\s*+(?:NOT\\s++(?:LIKE|IN)|>=|<=|<>|!=|[=<>]|LIKE|IN)\\s*+\\(?\\s*+#\\{ew\\.paramNameValuePairs\\.(\\w++)\\}");

    /**
     * 查询条件段中全部 pairs 占位符键
     */
    private static final Pattern QUERY_PAIRS_KEYS = Pattern
        .compile("#\\{ew\\.paramNameValuePairs\\.(\\w++)\\}");

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
        try {
            Object newParameter = this.resolveValue(parameterObject, mappedStatement, snapshot,
                copyCache);
            if (newParameter != parameterObject) {
                invocation.getArgs()[1] = newParameter;
            }
            return invocation.proceed();
        } finally {
            // Wrapper 的 paramNameValuePairs 在写入完成后恢复原值，加密失败时不残留密文
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
        CryptoSnapshot snapshot = new CryptoSnapshot();
        Map<String, FieldEncrypt> encryptParameterMap = super.getEncryptParameters(mappedStatement);
        Map<Object, Object> newParameterMap = this.newParameterMap(parameterMap);
        IdentityHashMap<Object, Object> copyCache = new IdentityHashMap<>();
        try {
            for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
                newParameterMap.put(entry.getKey(), this.resolveQueryValue(entry.getKey(),
                    entry.getValue(), encryptParameterMap, mappedStatement, snapshot, copyCache));
            }
            invocation.getArgs()[1] = newParameterMap;
            return invocation.proceed();
        } finally {
            // Wrapper 的条件参数在查询完成后恢复原值，加密失败时不残留密文
            snapshot.restore();
        }
    }

    /**
     * 构造与原参数同语义的参数容器：ParamMap 保持缺键抛出 BindingException 的拼写检查语义，
     * 其余场景使用普通 HashMap
     *
     * @param parameterMap 原参数 Map
     * @return 新参数容器
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> newParameterMap(Map<?, ?> parameterMap) {
        if (parameterMap instanceof MapperMethod.ParamMap) {
            return new MapperMethod.ParamMap();
        }
        return new HashMap<>(parameterMap.size());
    }

    /**
     * 加密查询单个参数值：参数级注解的 String 值与直接实体替换为加密结果，Wrapper 的条件值
     * 就地加密（快照还原），其余原样保留
     *
     * @param parameterName       参数名
     * @param parameterValue      参数值
     * @param encryptParameterMap 参数级加密注解映射
     * @param mappedStatement     映射语句
     * @param snapshot            快照
     * @param copyCache           拷贝缓存
     * @return 处理后的参数值
     */
    private Object resolveQueryValue(Object parameterName,
        Object parameterValue,
        Map<String, FieldEncrypt> encryptParameterMap,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        if (parameterValue instanceof String str) {
            FieldEncrypt fieldEncrypt = encryptParameterMap.get(String.valueOf(parameterName));
            if (fieldEncrypt != null) {
                return this.doEncrypt(str, fieldEncrypt, String.valueOf(parameterName));
            }
            return parameterValue;
        }
        if (parameterValue instanceof AbstractWrapper wrapper) {
            this.encryptQueryWrapper(wrapper, mappedStatement, snapshot);
            return parameterValue;
        }
        if (parameterValue == null || ClassUtil.isBasicType(parameterValue.getClass())) {
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
     * 加密 Wrapper 查询条件的参数值（针对 QueryWrapper、LambdaQueryWrapper 等条件），
     * 按 AND/OR 切分条件段后配对列名与占位符，命中加密列的参数值就地加密并记录快照
     *
     * @param wrapper         Wrapper 参数
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     */
    private void encryptQueryWrapper(AbstractWrapper wrapper,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot) {
        Class<?> entityClass = this.getEntityClass(wrapper, mappedStatement);
        Map<String, FieldEncrypt> columnMap = super.getColumnEncryptAnnotations(entityClass);
        if (columnMap.isEmpty()) {
            return;
        }
        String sqlSegment = wrapper.getSqlSegment();
        if (CharSequenceUtil.isBlank(sqlSegment)) {
            return;
        }
        for (String segment : sqlSegment.split("\\s++(?:AND|OR)\\s++")) {
            this.encryptQueryCondition(wrapper, segment, columnMap, snapshot);
        }
    }

    /**
     * 加密单个查询条件段的参数值，IN 等多占位符条件段的全部参数值同属该列
     *
     * @param wrapper   Wrapper 参数
     * @param segment   条件段
     * @param columnMap 列名到加密注解映射
     * @param snapshot  快照
     */
    private void encryptQueryCondition(AbstractWrapper wrapper,
        String segment,
        Map<String, FieldEncrypt> columnMap,
        CryptoSnapshot snapshot) {
        Matcher pairMatcher = QUERY_CONDITION_PAIR.matcher(segment);
        if (!pairMatcher.find()) {
            return;
        }
        FieldEncrypt fieldEncrypt = columnMap.get(pairMatcher.group(1)
            .replace("`", "")
            .toLowerCase());
        if (fieldEncrypt == null) {
            return;
        }
        Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
        Matcher keysMatcher = QUERY_PAIRS_KEYS.matcher(segment);
        while (keysMatcher.find()) {
            String valueKey = keysMatcher.group(1);
            Object value = paramNameValuePairs.get(valueKey);
            if (ObjectUtil.isNull(value)) {
                continue;
            }
            snapshot.recordMapEntry(paramNameValuePairs, valueKey, value);
            paramNameValuePairs.put(valueKey, this.doEncrypt(value, fieldEncrypt, pairMatcher
                .group(1)));
        }
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
        return this.encryptParameterContainer(value, mappedStatement, snapshot, copyCache);
    }

    /**
     * 加密容器类参数（Map、集合、数组）或实体参数：前三者替换为新容器，实体拷贝加密
     *
     * @param value           参数值
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @param copyCache       拷贝缓存
     * @return 加密后的新参数对象，无需替换时返回 null
     */
    private Object encryptParameterContainer(Object value,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        if (value instanceof Map<?, ?> parameterMap) {
            return this.encryptParameterMap(parameterMap, mappedStatement, snapshot, copyCache);
        }
        if (value instanceof Collection<?> collection) {
            return this.encryptParameterCollection(collection, mappedStatement, snapshot,
                copyCache);
        }
        Class<?> valueClass = value.getClass();
        if (valueClass.isArray() && !valueClass.getComponentType().isPrimitive()) {
            return this.encryptParameterArray((Object[]) value, mappedStatement, snapshot,
                copyCache);
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
     * 加密 Map 参数：键值逐项解析，返回承载加密拷贝的新容器
     *
     * @param parameterMap    参数 Map
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @param copyCache       拷贝缓存
     * @return 新参数容器
     */
    private Map<Object, Object> encryptParameterMap(Map<?, ?> parameterMap,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        Map<Object, Object> newParameterMap = this.newParameterMap(parameterMap);
        // 先登记新容器再遍历，自引用参数可命中缓存而不会无限递归
        copyCache.put(parameterMap, newParameterMap);
        for (Map.Entry<?, ?> entry : parameterMap.entrySet()) {
            newParameterMap.put(entry.getKey(), this.resolveValue(entry.getValue(),
                mappedStatement, snapshot, copyCache));
        }
        return newParameterMap;
    }

    /**
     * 加密集合参数：元素逐项解析，返回承载加密拷贝的新集合
     *
     * @param collection      参数集合
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @param copyCache       拷贝缓存
     * @return 新参数集合
     */
    private List<Object> encryptParameterCollection(Collection<?> collection,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        List<Object> newCollection = new ArrayList<>(collection.size());
        copyCache.put(collection, newCollection);
        for (Object element : collection) {
            newCollection.add(this.resolveValue(element, mappedStatement, snapshot, copyCache));
        }
        return newCollection;
    }

    /**
     * 加密数组参数：元素逐项解析，返回承载加密拷贝的新数组
     *
     * @param sourceArray     参数数组
     * @param mappedStatement 映射语句
     * @param snapshot        快照
     * @param copyCache       拷贝缓存
     * @return 新参数数组
     */
    private Object[] encryptParameterArray(Object[] sourceArray,
        MappedStatement mappedStatement,
        CryptoSnapshot snapshot,
        IdentityHashMap<Object, Object> copyCache) {
        Object[] newArray = Arrays.copyOf(sourceArray, sourceArray.length);
        copyCache.put(sourceArray, newArray);
        for (int i = 0; i < newArray.length; i++) {
            Object processed = this.resolveValue(newArray[i], mappedStatement, snapshot, copyCache);
            if (processed != newArray[i]) {
                newArray[i] = processed;
            }
        }
        return newArray;
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
            String strValue = fieldValue == null ? null : String.valueOf(fieldValue);
            if (CharSequenceUtil.isBlank(strValue)) {
                continue;
            }
            FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
            ReflectUtil.setFieldValue(entity, field, EncryptHelper
                .encrypt(strValue, fieldEncrypt, field.getName()));
        }
    }

    /**
     * 加密 Wrapper 的 SET 参数（针对 MP 的 UpdateWrapper、LambdaUpdateWrapper 等参数），
     * 按正则配对「列名=占位符」解析，原样 SQL 片段（如 setSql）与含逗号的内容不会被误切
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
        Class<?> entityClass = this.getEntityClass(wrapper, mappedStatement);
        Map<String, FieldEncrypt> columnMap = super.getColumnEncryptAnnotations(entityClass);
        if (columnMap.isEmpty()) {
            return;
        }
        Matcher matcher = SQL_SET_PAIR_PATTERN.matcher(sqlSet);
        while (matcher.find()) {
            this.encryptWrapperPair(wrapper, matcher, columnMap, snapshot);
        }
    }

    /**
     * 加密单个「列名=占位符」配对的参数值，未命中加密列或占位符无值时跳过
     *
     * @param wrapper   Wrapper 参数
     * @param matcher   SET 片段配对匹配器
     * @param columnMap 列名到加密注解映射
     * @param snapshot  快照
     */
    private void encryptWrapperPair(AbstractWrapper wrapper,
        Matcher matcher,
        Map<String, FieldEncrypt> columnMap,
        CryptoSnapshot snapshot) {
        String columnName = matcher.group(1).replace("`", "");
        FieldEncrypt fieldEncrypt = columnMap.get(columnName.toLowerCase());
        if (fieldEncrypt == null) {
            return;
        }
        String valueKey = matcher.group(2);
        Map<String, Object> paramNameValuePairs = wrapper.getParamNameValuePairs();
        Object value = paramNameValuePairs.get(valueKey);
        if (ObjectUtil.isNull(value)) {
            return;
        }
        snapshot.recordMapEntry(paramNameValuePairs, valueKey, value);
        paramNameValuePairs.put(valueKey, this.doEncrypt(value, fieldEncrypt, columnName));
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
