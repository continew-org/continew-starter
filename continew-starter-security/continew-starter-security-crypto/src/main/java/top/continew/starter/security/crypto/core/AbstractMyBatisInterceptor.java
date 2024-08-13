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

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.security.crypto.annotation.FieldEncrypt;
import top.continew.starter.security.crypto.encryptor.IEncryptor;
import top.continew.starter.security.crypto.enums.Algorithm;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 字段解密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public abstract class AbstractMyBatisInterceptor implements Interceptor {

    private static final Map<String, Map<String, FieldEncrypt>> ENCRYPT_PARAM_CACHE = new ConcurrentHashMap<>();

    /**
     * 获取所有字符串类型、需要加/解密的、有值字段
     *
     * @param obj 对象
     * @return 字段列表
     */
    public List<Field> getEncryptFields(Object obj) {
        if (null == obj) {
            return Collections.emptyList();
        }
        return Arrays.stream(ReflectUtil.getFields(obj.getClass()))
            .filter(field -> String.class.equals(field.getType()))
            .filter(field -> null != field.getAnnotation(FieldEncrypt.class))
            .filter(field -> null != ReflectUtil.getFieldValue(obj, field))
            .toList();
    }

    /**
     * 获取字段加/解密处理器
     *
     * @param fieldEncrypt 字段加密注解
     * @return 加/解密处理器
     */
    public IEncryptor getEncryptor(FieldEncrypt fieldEncrypt) {
        Class<? extends IEncryptor> encryptorClass = fieldEncrypt.encryptor();
        // 使用预定义加/解密处理器
        if (encryptorClass == IEncryptor.class) {
            Algorithm algorithm = fieldEncrypt.value();
            return ReflectUtil.newInstance(algorithm.getEncryptor());
        }
        // 使用自定义加/解密处理器
        return SpringUtil.getBean(encryptorClass);
    }

    /**
     * 获取加密参数
     *
     * @param mappedStatement 映射语句
     * @return 加密参数
     */
    public Map<String, FieldEncrypt> getEncryptParams(MappedStatement mappedStatement) {
        return getEncryptParams(mappedStatement, null);
    }

    /**
     * 获取加密参数
     *
     * @param mappedStatement 映射语句
     * @param parameterCount  参数数量
     * @return 加密参数
     */
    public Map<String, FieldEncrypt> getEncryptParams(MappedStatement mappedStatement, Integer parameterCount) {
        String mappedStatementId = mappedStatement.getId();
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (SqlCommandType.UPDATE != sqlCommandType) {
            return ENCRYPT_PARAM_CACHE.computeIfAbsent(mappedStatementId, key -> this
                .getEncryptParams(mappedStatementId, parameterCount));
        } else {
            return this.getEncryptParams(mappedStatementId, parameterCount);
        }
    }

    /**
     * 获取参数名称
     *
     * @param parameter 参数
     * @return 参数名称
     */
    public String getParameterName(Parameter parameter) {
        Param param = parameter.getAnnotation(Param.class);
        return null != param ? param.value() : parameter.getName();
    }

    /**
     * 获取加密参数列表
     *
     * @param mappedStatementId 映射语句 ID
     * @param parameterCount    参数数量
     * @return 加密参数列表
     */
    private Map<String, FieldEncrypt> getEncryptParams(String mappedStatementId, Integer parameterCount) {
        Method method = this.getMethod(mappedStatementId, parameterCount);
        if (method == null) {
            return Collections.emptyMap();
        }
        return this.getEncryptParams(method);
    }

    /**
     * 获取映射方法
     *
     * @param mappedStatementId 映射语句 ID
     * @param parameterCount    参数数量
     * @return 映射方法
     */
    private Method getMethod(String mappedStatementId, Integer parameterCount) {
        try {
            String className = CharSequenceUtil.subBefore(mappedStatementId, StringConstants.DOT, true);
            String wrapperMethodName = CharSequenceUtil.subAfter(mappedStatementId, StringConstants.DOT, true);
            String methodName = Stream.of("_mpCount", "_COUNT")
                .filter(wrapperMethodName::endsWith)
                .findFirst()
                .map(suffix -> wrapperMethodName.substring(0, wrapperMethodName.length() - suffix.length()))
                .orElse(wrapperMethodName);
            // 获取真实方法
            Optional<Method> methodOptional = Arrays.stream(ReflectUtil.getMethods(Class.forName(className), m -> {
                if (parameterCount != null) {
                    return Objects.equals(m.getName(), methodName) && m.getParameterCount() == parameterCount;
                }
                return Objects.equals(m.getName(), methodName);
            })).findFirst();
            return methodOptional.orElse(null);
        } catch (ClassNotFoundException e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * 获取加密参数列表
     *
     * @param method 方法
     * @return 加密参数列表
     */
    private Map<String, FieldEncrypt> getEncryptParams(Method method) {
        // 获取方法中的加密参数
        Map<String, FieldEncrypt> map = MapUtil.newHashMap();
        Parameter[] parameterArr = method.getParameters();
        for (int i = 0; i < parameterArr.length; i++) {
            Parameter parameter = parameterArr[i];
            String parameterName = this.getParameterName(parameter);
            FieldEncrypt fieldEncrypt = parameter.getAnnotation(FieldEncrypt.class);
            if (null != fieldEncrypt) {
                map.put(parameterName, fieldEncrypt);
                if (String.class.equals(parameter.getType())) {
                    map.put("param" + (i + 1), fieldEncrypt);
                }
            } else if (parameterName.startsWith(Constants.ENTITY)) {
                map.put(parameterName, null);
            } else if (parameterName.startsWith(Constants.WRAPPER)) {
                map.put(parameterName, null);
            }
        }
        return map;
    }
}