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

package top.continew.starter.security.crypto.core;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.SimpleTypeRegistry;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.security.crypto.annotation.FieldEncrypt;
import top.continew.starter.security.crypto.autoconfigure.CryptoProperties;
import top.continew.starter.security.crypto.encryptor.IEncryptor;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * 字段加密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
@Intercepts({@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class}),
    @Signature(method = "query", type = Executor.class, args = {MappedStatement.class, Object.class, RowBounds.class,
        ResultHandler.class, CacheKey.class, BoundSql.class}),
    @Signature(method = "query", type = Executor.class, args = {MappedStatement.class, Object.class, RowBounds.class,
        ResultHandler.class})})
public class MyBatisEncryptInterceptor extends AbstractMyBatisInterceptor {

    private CryptoProperties properties;

    public MyBatisEncryptInterceptor(CryptoProperties properties) {
        this.properties = properties;
    }

    public MyBatisEncryptInterceptor() {
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement)args[0];
        Object parameter = args[1];
        if (!this.isEncryptRequired(parameter, mappedStatement.getSqlCommandType())) {
            return invocation.proceed();
        }
        // 使用 @Param 注解的场景
        if (parameter instanceof HashMap parameterMap) {
            this.encryptMap(parameterMap, mappedStatement);
        } else {
            this.doEncrypt(this.getEncryptFields(parameter), parameter);
        }
        return invocation.proceed();
    }

    /**
     * 是否需要加密处理
     *
     * @param parameter      参数
     * @param sqlCommandType SQL 类型
     * @return true：是；false：否
     */
    private boolean isEncryptRequired(Object parameter, SqlCommandType sqlCommandType) {
        if (ObjectUtil.isEmpty(parameter)) {
            return false;
        }
        if (!(SqlCommandType.UPDATE == sqlCommandType || SqlCommandType.INSERT == sqlCommandType || SqlCommandType.SELECT == sqlCommandType)) {
            return false;
        }
        return !SimpleTypeRegistry.isSimpleType(parameter.getClass());
    }

    /**
     * 加密 Map 类型数据（使用 @Param 注解的场景）
     *
     * @param parameterMap    参数
     * @param mappedStatement 映射语句
     * @throws Exception /
     */
    private void encryptMap(HashMap<String, Object> parameterMap, MappedStatement mappedStatement) throws Exception {
        Map<String, FieldEncrypt> encryptParamMap = super.getEncryptParams(mappedStatement.getId(), parameterMap
            .isEmpty() ? null : parameterMap.size() / 2);
        for (Map.Entry<String, FieldEncrypt> encryptParamEntry : encryptParamMap.entrySet()) {
            String parameterName = encryptParamEntry.getKey();
            if (parameterName.startsWith(Constants.ENTITY)) {
                // 兼容 MyBatis Plus 封装的 update 相关方法，updateById、update
                Object entity = parameterMap.getOrDefault(parameterName, null);
                this.doEncrypt(this.getEncryptFields(entity), entity);
            } else if (parameterName.startsWith(Constants.WRAPPER)) {
                Wrapper wrapper = (Wrapper)parameterMap.getOrDefault(parameterName, null);
                // 处理 wrapper 的情况
                handleWrapperEncrypt(wrapper, mappedStatement);
            } else {
                FieldEncrypt fieldEncrypt = encryptParamEntry.getValue();
                parameterMap.put(parameterName, this.doEncrypt(parameterMap.get(parameterName), fieldEncrypt));
            }
        }
    }

    /**
     * 处理加密
     *
     * @param parameterValue 参数值
     * @param fieldEncrypt   字段加密注解
     * @throws Exception /
     */
    private Object doEncrypt(Object parameterValue, FieldEncrypt fieldEncrypt) throws Exception {
        if (null == parameterValue) {
            return null;
        }
        IEncryptor encryptor = super.getEncryptor(fieldEncrypt);
        // 优先获取自定义对称加密算法密钥，获取不到时再获取全局配置
        String password = ObjectUtil.defaultIfBlank(fieldEncrypt.password(), properties.getPassword());
        return encryptor.encrypt(parameterValue.toString(), password, properties.getPublicKey());
    }

    /**
     * 处理加密
     *
     * @param fieldList 加密字段列表
     * @param entity    实体
     * @throws Exception /
     */
    private void doEncrypt(List<Field> fieldList, Object entity) throws Exception {
        for (Field field : fieldList) {
            IEncryptor encryptor = super.getEncryptor(field.getAnnotation(FieldEncrypt.class));
            Object fieldValue = ReflectUtil.getFieldValue(entity, field);
            // 优先获取自定义对称加密算法密钥，获取不到时再获取全局配置
            String password = ObjectUtil.defaultIfBlank(field.getAnnotation(FieldEncrypt.class).password(), properties
                .getPassword());
            String ciphertext = encryptor.encrypt(fieldValue.toString(), password, properties.getPublicKey());
            ReflectUtil.setFieldValue(entity, field, ciphertext);
        }
    }

    /**
     * 处理 wrapper 的加密情况
     *
     * @param wrapper         wrapper 对象
     * @param mappedStatement 映射语句
     * @throws Exception /
     */
    private void handleWrapperEncrypt(Wrapper wrapper, MappedStatement mappedStatement) throws Exception {
        if (wrapper instanceof AbstractWrapper abstractWrapper) {
            String sqlSet = abstractWrapper.getSqlSet();
            if (StringUtils.isEmpty(sqlSet)) {
                return;
            }
            String className = CharSequenceUtil.subBefore(mappedStatement.getId(), StringConstants.DOT, true);
            Class<?> mapperClass = Class.forName(className);
            Optional<Class> baseMapperGenerics = getDoByMapperClass(mapperClass, Optional.empty());
            // 获取不到泛型对象 则不进行下面的逻辑
            if (baseMapperGenerics.isEmpty()) {
                return;
            }
            TableInfo tableInfo = TableInfoHelper.getTableInfo(baseMapperGenerics.get());
            List<TableFieldInfo> fieldList = tableInfo.getFieldList();
            // 将 name=#{ew.paramNameValuePairs.xxx},age=#{ew.paramNameValuePairs.xxx} 切出来
            for (String sqlFragment : sqlSet.split(Constants.COMMA)) {
                String columnName = sqlFragment.split(Constants.EQUALS)[0];
                // 截取其中的 xxx 字符 ：#{ew.paramNameValuePairs.xxx}
                String paramNameVal = sqlFragment.split(Constants.EQUALS)[1].substring(25, sqlFragment
                    .split(Constants.EQUALS)[1].length() - 1);
                Optional<TableFieldInfo> fieldInfo = fieldList.stream()
                    .filter(f -> f.getColumn().equals(columnName))
                    .findAny();
                if (fieldInfo.isPresent()) {
                    TableFieldInfo tableFieldInfo = fieldInfo.get();
                    FieldEncrypt fieldEncrypt = tableFieldInfo.getField().getAnnotation(FieldEncrypt.class);
                    if (fieldEncrypt != null) {
                        Map<String, Object> paramNameValuePairs = abstractWrapper.getParamNameValuePairs();
                        Object o = paramNameValuePairs.get(paramNameVal);
                        paramNameValuePairs.put(paramNameVal, this.doEncrypt(o, fieldEncrypt));
                    }
                }
            }
        }
    }

    /**
     * 从 Mapper 获取泛型
     *
     * @param mapperClass      mapper class
     * @param tempResult 临时存储的泛型对象
     * @return domain 对象
     */
    private static Optional<Class> getDoByMapperClass(Class<?> mapperClass, Optional<Class> tempResult) {
        Type[] genericInterfaces = mapperClass.getGenericInterfaces();
        Optional<Class> result = tempResult;
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType parameterizedType) {
                Type rawType = parameterizedType.getRawType();
                Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                // 如果匹配上 BaseMapper 且泛型参数是 Class 类型，则直接返回
                if (rawType.equals(BaseMapper.class)) {
                    return actualTypeArguments[0] instanceof Class ?
                            Optional.of((Class) actualTypeArguments[0]) : result;
                } else if (rawType instanceof Class interfaceClass) {
                    // 如果泛型参数是 Class 类型，则传递给递归调用
                    if (actualTypeArguments[0] instanceof Class tempResultClass) {
                        result = Optional.of(tempResultClass);
                    }
                    // 递归调用，继续查找
                    Optional<Class> innerResult = getDoByMapperClass(interfaceClass, result);
                    if (innerResult.isPresent()) {
                        return innerResult;
                    }
                }
            }
        }
        // 如果没有找到，返回传递进来的 tempResult
        return Optional.empty();
    }
}
