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

package top.continew.starter.data.mp.handler;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.baomidou.mybatisplus.annotation.IEnum;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import org.apache.ibatis.reflection.DefaultReflectorFactory;
import org.apache.ibatis.reflection.MetaClass;
import org.apache.ibatis.reflection.ReflectorFactory;
import org.apache.ibatis.reflection.invoker.Invoker;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import top.continew.starter.core.enums.BaseEnum;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 枚举类型处理器（扩展 BaseEnum 支持）
 *
 * @author hubin（<a href="https://gitee.com/baomidou/mybatis-plus">MyBatis Plus</a>）
 * @author Charles7c
 * @see com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
 * @since 2.4.0
 */
public class MybatisBaseEnumTypeHandler<E extends Enum<E>> extends BaseTypeHandler<E> {

    private static final Map<String, String> TABLE_METHOD_OF_ENUM_TYPES = new ConcurrentHashMap<>();
    private static final ReflectorFactory REFLECTOR_FACTORY = new DefaultReflectorFactory();
    private final Class<E> enumClassType;
    private final Class<?> propertyType;
    private final Invoker getInvoker;

    public MybatisBaseEnumTypeHandler(Class<E> enumClassType) {
        if (enumClassType == null) {
            throw new IllegalArgumentException("Type argument cannot be null");
        }
        this.enumClassType = enumClassType;
        MetaClass metaClass = MetaClass.forClass(enumClassType, REFLECTOR_FACTORY);
        String name = "value";
        if (!BaseEnum.class.isAssignableFrom(enumClassType) && !IEnum.class.isAssignableFrom(enumClassType)) {
            name = findEnumValueFieldName(this.enumClassType).orElseThrow(() -> new IllegalArgumentException(String
                .format("Could not find @EnumValue in Class: %s.", this.enumClassType.getName())));
        }
        this.propertyType = ReflectionKit.resolvePrimitiveIfNecessary(metaClass.getGetterType(name));
        this.getInvoker = metaClass.getGetInvoker(name);
    }

    /**
     * 查找标记标记EnumValue字段
     *
     * @param clazz class
     * @return EnumValue字段
     */
    public static Optional<String> findEnumValueFieldName(Class<?> clazz) {
        if (clazz != null && clazz.isEnum()) {
            String className = clazz.getName();
            return Optional.ofNullable(CollectionUtils.computeIfAbsent(TABLE_METHOD_OF_ENUM_TYPES, className, key -> {
                Optional<Field> fieldOptional = findEnumValueAnnotationField(clazz);
                return fieldOptional.map(Field::getName).orElse(null);
            }));
        }
        return Optional.empty();
    }

    private static Optional<Field> findEnumValueAnnotationField(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(field -> field.isAnnotationPresent(EnumValue.class))
            .findFirst();
    }

    /**
     * 判断是否为MP枚举处理
     *
     * @param clazz class
     * @return 是否为MP枚举处理
     */
    public static boolean isMpEnums(Class<?> clazz) {
        return clazz != null && clazz.isEnum() && (BaseEnum.class.isAssignableFrom(clazz) || IEnum.class
            .isAssignableFrom(clazz) || findEnumValueFieldName(clazz).isPresent());
    }

    @SuppressWarnings("Duplicates")
    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, E parameter, JdbcType jdbcType) throws SQLException {
        if (jdbcType == null) {
            ps.setObject(i, this.getValue(parameter));
        } else {
            // see r3589
            ps.setObject(i, this.getValue(parameter), jdbcType.TYPE_CODE);
        }
    }

    @Override
    public E getNullableResult(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName, this.propertyType);
        if (null == value || rs.wasNull()) {
            return null;
        }
        return this.valueOf(value);
    }

    @Override
    public E getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        Object value = rs.getObject(columnIndex, this.propertyType);
        if (null == value || rs.wasNull()) {
            return null;
        }
        return this.valueOf(value);
    }

    @Override
    public E getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        Object value = cs.getObject(columnIndex, this.propertyType);
        if (null == value || cs.wasNull()) {
            return null;
        }
        return this.valueOf(value);
    }

    private E valueOf(Object value) {
        E[] es = this.enumClassType.getEnumConstants();
        return Arrays.stream(es).filter(e -> equalsValue(value, getValue(e))).findAny().orElse(null);
    }

    /**
     * 值比较
     *
     * @param sourceValue 数据库字段值
     * @param targetValue 当前枚举属性值
     * @return 是否匹配
     */
    private boolean equalsValue(Object sourceValue, Object targetValue) {
        String sValue = StringUtils.toStringTrim(sourceValue);
        String tValue = StringUtils.toStringTrim(targetValue);
        if (sourceValue instanceof Number && targetValue instanceof Number && new BigDecimal(sValue)
            .compareTo(new BigDecimal(tValue)) == 0) {
            return true;
        }
        return Objects.equals(sValue, tValue);
    }

    private Object getValue(Object object) {
        try {
            return this.getInvoker.invoke(object, new Object[0]);
        } catch (ReflectiveOperationException e) {
            throw ExceptionUtils.mpe(e);
        }
    }
}
