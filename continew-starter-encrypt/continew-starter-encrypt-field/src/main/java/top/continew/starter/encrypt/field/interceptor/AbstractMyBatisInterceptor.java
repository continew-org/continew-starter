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
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.mapping.MappedStatement;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BaseException;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * 字段解密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public abstract class AbstractMyBatisInterceptor {

    private static final Map<Class<?>, List<Field>> CLASS_FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Map<String, FieldEncrypt>> ENCRYPT_PARAM_CACHE =
        new ConcurrentHashMap<>();
    private static final Map<Class<?>, Map<String, FieldEncrypt>> COLUMN_ENCRYPT_CACHE =
        new ConcurrentHashMap<>();
    private static final Map<String, Map<String, FieldEncrypt>> GLOBAL_COLUMN_ENCRYPT_CACHE =
        new ConcurrentHashMap<>();
    private static final String GLOBAL_COLUMN_CACHE_KEY = "global";

    /**
     * 获取实体声明的加密列映射：列名（小写）到加密注解，优先取 MyBatis-Plus TableInfo 的
     * 列名，TableInfo 未初始化时以字段名兜底
     *
     * @param entityClass 实体类
     * @return 列名到加密注解映射
     */
    protected Map<String, FieldEncrypt> getColumnEncryptAnnotations(Class<?> entityClass) {
        return COLUMN_ENCRYPT_CACHE.computeIfAbsent(entityClass, clazz -> {
            Map<String, FieldEncrypt> columnMap = new HashMap<>();
            TableInfo tableInfo = TableInfoHelper.getTableInfo(clazz);
            if (tableInfo == null) {
                for (Field field : ReflectUtil.getFields(clazz)) {
                    FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
                    if (fieldEncrypt != null) {
                        columnMap.put(field.getName().toLowerCase(), fieldEncrypt);
                    }
                }
                return columnMap;
            }
            for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
                Field field = fieldInfo.getField();
                FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
                if (fieldEncrypt != null) {
                    columnMap.put(fieldInfo.getColumn().toLowerCase(), fieldEncrypt);
                }
            }
            return columnMap;
        });
    }

    /**
     * 获取全局声明的加密列映射：已注册 TableInfo 实体的全部加密列（小写），用于
     * resultType=map 等无实体上下文的结果行解密
     *
     * @return 列名到加密注解映射
     */
    protected Map<String, FieldEncrypt> getGlobalColumnEncryptAnnotations() {
        return GLOBAL_COLUMN_ENCRYPT_CACHE.computeIfAbsent(GLOBAL_COLUMN_CACHE_KEY, key -> {
            Map<String, FieldEncrypt> columnMap = new HashMap<>();
            Collection<TableInfo> tableInfos = TableInfoHelper.getTableInfos();
            for (TableInfo tableInfo : tableInfos) {
                for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
                    Field field = fieldInfo.getField();
                    FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
                    if (fieldEncrypt != null) {
                        columnMap.putIfAbsent(fieldInfo.getColumn().toLowerCase(), fieldEncrypt);
                    }
                }
            }
            return columnMap;
        });
    }

    /**
     * 获取所有字符串类型、需要加/解密的、有值字段
     *
     * @param obj 对象
     * @return 字段列表
     */
    protected List<Field> getEncryptFields(Object obj) {
        if (obj == null) {
            return Collections.emptyList();
        }
        return this.getEncryptFields(obj.getClass());
    }

    /**
     * 获取所有字符串类型、需要加/解密的、有值字段
     *
     * @param clazz 类型对象
     * @return 字段列表
     */
    protected List<Field> getEncryptFields(Class<?> clazz) {
        return CLASS_FIELD_CACHE.computeIfAbsent(clazz,
            key -> Arrays.stream(ReflectUtil.getFields(clazz))
                .filter(field -> String.class.equals(field.getType()))
                .filter(field -> field.getAnnotation(FieldEncrypt.class) != null)
                .toList());
    }

    /**
     * 获取加密参数
     *
     * @param mappedStatement 映射语句
     * @return 获取加密参数
     */
    protected Map<String, FieldEncrypt> getEncryptParameters(MappedStatement mappedStatement) {
        String mappedStatementId = mappedStatement.getId();
        return ENCRYPT_PARAM_CACHE.computeIfAbsent(mappedStatementId, key -> {
            Method method = this.getMethod(mappedStatementId);
            if (method == null) {
                return Collections.emptyMap();
            }
            Map<String, FieldEncrypt> encryptMap = new HashMap<>();
            Parameter[] parameters = method.getParameters();
            for (int i = 0; i < parameters.length; i++) {
                Parameter parameter = parameters[i];
                FieldEncrypt fieldEncrypt = parameter.getAnnotation(FieldEncrypt.class);
                if (fieldEncrypt == null) {
                    continue;
                }
                String parameterName = this.getParameterName(parameter);
                encryptMap.put(parameterName, fieldEncrypt);
                if (String.class.equals(parameter.getType())) {
                    encryptMap.put("param" + (i + 1), fieldEncrypt);
                }
            }
            return encryptMap;
        });
    }

    /**
     * 获取映射方法
     *
     * @param mappedStatementId 映射语句 ID
     * @return 映射方法
     */
    private Method getMethod(String mappedStatementId) {
        String className = CharSequenceUtil.subBefore(mappedStatementId, StringConstants.DOT, true);
        String methodName = CharSequenceUtil.subAfter(mappedStatementId, StringConstants.DOT, true);
        try {
            Method[] methods = ReflectUtil.getMethods(Class.forName(className));
            return Stream.of(methods).filter(method -> method.getName().equals(methodName))
                .findFirst().orElse(null);
        } catch (ClassNotFoundException e) {
            throw new BaseException(e);
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
        return param != null ? param.value() : parameter.getName();
    }
}
