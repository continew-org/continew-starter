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

package top.continew.starter.security.mask.core;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.security.mask.annotation.JsonMask;
import top.continew.starter.security.mask.strategy.IMaskStrategy;

import java.util.Objects;

/**
 * JSON 脱敏序列化器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class JsonMaskSerializer extends ValueSerializer<String> {

    private JsonMask jsonMask;

    public JsonMaskSerializer(JsonMask jsonMask) {
        this.jsonMask = jsonMask;
    }

    public JsonMaskSerializer() {
    }

    @Override
    public void serialize(String str, JsonGenerator jsonGenerator, SerializationContext context) {
        if (CharSequenceUtil.isBlank(str)) {
            jsonGenerator.writeString(StringConstants.EMPTY);
            return;
        }
        // 使用自定义脱敏策略
        Class<? extends IMaskStrategy> strategyClass = jsonMask.strategy();
        IMaskStrategy maskStrategy = strategyClass != IMaskStrategy.class
            ? SpringUtil.getBean(strategyClass)
            : jsonMask.value();
        jsonGenerator.writeString(maskStrategy
            .mask(str, jsonMask.character(), jsonMask.left(), jsonMask.right()));
    }

    @Override
    public ValueSerializer<?> createContextual(SerializationContext context,
        BeanProperty beanProperty) {
        if (beanProperty == null) {
            return context.findNullValueSerializer(beanProperty);
        }
        if (!Objects.equals(beanProperty.getType().getRawClass(), String.class)) {
            return context.findValueSerializer(beanProperty.getType());
        }
        JsonMask jsonMaskAnnotation =
            ObjectUtil.defaultIfNull(beanProperty.getAnnotation(JsonMask.class), beanProperty
                .getContextAnnotation(JsonMask.class));
        if (jsonMaskAnnotation == null) {
            return context.findValueSerializer(beanProperty.getType());
        }
        return new JsonMaskSerializer(jsonMaskAnnotation);
    }
}
