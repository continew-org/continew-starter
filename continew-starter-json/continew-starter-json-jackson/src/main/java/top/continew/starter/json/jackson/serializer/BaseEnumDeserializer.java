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

package top.continew.starter.json.jackson.serializer;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import top.continew.starter.core.enums.BaseEnum;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 枚举接口 BaseEnum 反序列化器
 *
 * @see BaseEnum
 *
 * @author Charles7c
 * @since 2.4.0
 */
@JacksonStdImpl
public class BaseEnumDeserializer extends JsonDeserializer<BaseEnum>
    implements ContextualDeserializer {

    private final Class<? extends BaseEnum> fieldTypeClass;
    private final Map<String, BaseEnum> valueMap;

    public BaseEnumDeserializer() {
        this(null);
    }

    public BaseEnumDeserializer(Class<? extends BaseEnum> fieldTypeClass) {
        this.fieldTypeClass = fieldTypeClass;
        this.valueMap = this.initValueMap(fieldTypeClass);
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext deserializationContext,
        BeanProperty beanProperty) {
        // 获取字段类型
        Class<?> fieldType = null;
        if (beanProperty != null) {
            fieldType = beanProperty.getType().getRawClass();
        } else if (deserializationContext.getContextualType() != null) {
            fieldType = deserializationContext.getContextualType().getRawClass();
        }

        return fieldType != null && BaseEnum.class.isAssignableFrom(fieldType)
            ? new BaseEnumDeserializer((Class<? extends BaseEnum>) fieldType)
            : this;
    }

    @Override
    public BaseEnum deserialize(JsonParser jsonParser,
        DeserializationContext deserializationContext) throws IOException {
        if (fieldTypeClass == null || valueMap.isEmpty()) {
            return null;
        }

        String value = jsonParser.getValueAsString();
        if (StrUtil.isBlank(value)) {
            return null;
        }

        BaseEnum baseEnum = valueMap.get(value);
        if (baseEnum == null) {
            throw InvalidFormatException
                .from(jsonParser,
                    "Cannot deserialize value of type %s from %s: no matching enum constant"
                        .formatted(fieldTypeClass.getName(), value),
                    value, fieldTypeClass);
        }
        return baseEnum;
    }

    /**
     * 初始化 valueMap
     * <p>将枚举的 value 映射为枚举实例</p>
     *
     * @param enumClass 枚举类
     * @return 枚举值映射表
     */
    private Map<String, BaseEnum> initValueMap(Class<? extends BaseEnum> enumClass) {
        if (enumClass == null) {
            return Map.of();
        }
        BaseEnum[] enumConstants = enumClass.getEnumConstants();
        if (enumConstants == null || enumConstants.length == 0) {
            return Map.of();
        }
        // 将枚举的 value 转换为 String 作为 Key
        return Arrays.stream(enumConstants)
            .collect(Collectors.toMap(e -> String.valueOf(e.getValue()), e -> e, (k1, k2) -> k1));
    }
}
