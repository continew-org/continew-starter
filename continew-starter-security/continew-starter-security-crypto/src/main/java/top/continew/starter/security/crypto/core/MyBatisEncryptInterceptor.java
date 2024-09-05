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
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BaseException;
import top.continew.starter.security.crypto.annotation.FieldEncrypt;
import top.continew.starter.security.crypto.autoconfigure.CryptoProperties;
import top.continew.starter.security.crypto.encryptor.IEncryptor;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字段加密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class MyBatisEncryptInterceptor extends AbstractMyBatisInterceptor implements InnerInterceptor {

    private static final Pattern PARAM_PAIRS_PATTERN = Pattern
        .compile("#\\{ew\\.paramNameValuePairs\\.(" + Constants.WRAPPER_PARAM + "\\d+)\\}");
    private final CryptoProperties properties;

    public MyBatisEncryptInterceptor(CryptoProperties properties) {
        this.properties = properties;
    }

    @Override
    public void beforeQuery(Executor executor,
                            MappedStatement mappedStatement,
                            Object parameterObject,
                            RowBounds rowBounds,
                            ResultHandler resultHandler,
                            BoundSql boundSql) {
        if (null == parameterObject) {
            return;
        }
        if (parameterObject instanceof Map parameterMap) {
            Set set = new HashSet<>(parameterMap.values());
            for (Object parameter : set) {
                if (parameter instanceof AbstractWrapper || parameter instanceof String) {
                    continue;
                }
                this.encryptEntity(super.getEncryptFields(parameter), parameter);
            }
        }
    }

    @Override
    public void beforeUpdate(Executor executor,
                             MappedStatement mappedStatement,
                             Object parameterObject) throws SQLException {
        if (null == parameterObject) {
            return;
        }
        if (parameterObject instanceof Map parameterMap) {
            // 带别名方法（使用 @Param 注解的场景）
            this.encryptMap(parameterMap, mappedStatement);
        } else {
            // 无别名方法（例如：MP insert 等方法）
            this.encryptEntity(super.getEncryptFields(parameterObject), parameterObject);
        }
    }

    /**
     * 加密 Map 类型数据（使用 @Param 注解的场景）
     *
     * @param parameterMap    参数
     * @param mappedStatement 映射语句
     */
    private void encryptMap(Map<String, Object> parameterMap, MappedStatement mappedStatement) {
        Object parameter;
        // 别名带有 et（针对 MP 的 updateById、update 等方法）
        if (parameterMap.containsKey(Constants.ENTITY) && null != (parameter = parameterMap.get(Constants.ENTITY))) {
            this.encryptEntity(super.getEncryptFields(parameter), parameter);
        }
        // 别名带有 ew（针对 MP 的 UpdateWrapper、LambdaUpdateWrapper 等参数）
        if (parameterMap.containsKey(Constants.WRAPPER) && null != (parameter = parameterMap.get(Constants.WRAPPER))) {
            this.encryptWrapper(parameter, mappedStatement);
        }
    }

    /**
     * 处理 Wrapper 类型参数加密（针对 MP 的 UpdateWrapper、LambdaUpdateWrapper 等参数）
     *
     * @param parameter       Wrapper 参数
     * @param mappedStatement 映射语句
     * @since 2.1.1
     * @author cary
     * @author wangshaopeng@talkweb.com.cn（<a
     *         href="https://blog.csdn.net/tianmaxingkonger/article/details/130986784">基于Mybatis-Plus拦截器实现MySQL数据加解密</a>）
     */
    private void encryptWrapper(Object parameter, MappedStatement mappedStatement) {
        if (parameter instanceof AbstractWrapper updateWrapper) {
            String sqlSet = updateWrapper.getSqlSet();
            if (CharSequenceUtil.isBlank(sqlSet)) {
                return;
            }
            // 将 name=#{ew.paramNameValuePairs.xxx},age=#{ew.paramNameValuePairs.xxx} 切出来
            String[] elArr = sqlSet.split(StringConstants.COMMA);
            Map<String, String> propMap = new HashMap<>(elArr.length);
            Arrays.stream(elArr).forEach(el -> {
                String[] elPart = el.split(StringConstants.EQUALS);
                propMap.put(elPart[0], elPart[1]);
            });
            // 获取加密字段
            Class<?> entityClass = mappedStatement.getParameterMap().getType();
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
                    Object value = updateWrapper.getParamNameValuePairs().get(valueKey);
                    Object ciphertext;
                    try {
                        ciphertext = this.doEncrypt(value, fieldEncrypt);
                    } catch (Exception e) {
                        throw new BaseException(e);
                    }
                    updateWrapper.getParamNameValuePairs().put(valueKey, ciphertext);
                }
            }
        }
    }

    /**
     * 处理实体加密
     *
     * @param fieldList 加密字段列表
     * @param entity    实体
     */
    private void encryptEntity(List<Field> fieldList, Object entity) {
        for (Field field : fieldList) {
            IEncryptor encryptor = super.getEncryptor(field.getAnnotation(FieldEncrypt.class));
            Object fieldValue = ReflectUtil.getFieldValue(entity, field);
            if (null == fieldValue) {
                continue;
            }
            // 优先获取自定义对称加密算法密钥，获取不到时再获取全局配置
            String password = ObjectUtil.defaultIfBlank(field.getAnnotation(FieldEncrypt.class).password(), properties
                .getPassword());
            String ciphertext;
            try {
                ciphertext = encryptor.encrypt(fieldValue.toString(), password, properties.getPublicKey());
            } catch (Exception e) {
                throw new BaseException(e);
            }
            ReflectUtil.setFieldValue(entity, field, ciphertext);
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
}
