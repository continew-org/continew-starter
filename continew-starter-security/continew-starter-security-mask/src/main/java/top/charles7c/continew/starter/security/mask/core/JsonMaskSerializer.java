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

package top.charles7c.continew.starter.security.mask.core;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.ContextualSerializer;
import top.charles7c.continew.starter.core.constant.StringConstants;
import top.charles7c.continew.starter.security.mask.annotation.JsonMask;
import top.charles7c.continew.starter.security.mask.enums.MaskType;

import java.io.IOException;
import java.util.Objects;

/**
 * JSON 脱敏序列化器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class JsonMaskSerializer extends JsonSerializer<String> implements ContextualSerializer {

    private JsonMask jsonMask;

    public JsonMaskSerializer(JsonMask jsonMask) {
        this.jsonMask = jsonMask;
    }

    public JsonMaskSerializer() {
    }

    @Override
    public void serialize(String str,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        if (CharSequenceUtil.isBlank(str)) {
            jsonGenerator.writeString(StringConstants.EMPTY);
            return;
        }
        MaskType maskType = jsonMask.value();
        jsonGenerator.writeString(maskType.mask(str, jsonMask.character(), jsonMask.left(), jsonMask.right()));
    }

    @Override
    public JsonSerializer<?> createContextual(SerializerProvider serializerProvider,
                                              BeanProperty beanProperty) throws JsonMappingException {
        if (null == beanProperty) {
            return serializerProvider.findNullValueSerializer(null);
        }
        if (!Objects.equals(beanProperty.getType().getRawClass(), String.class)) {
            return serializerProvider.findValueSerializer(beanProperty.getType(), beanProperty);
        }
        JsonMask jsonMaskAnnotation = ObjectUtil.defaultIfNull(beanProperty.getAnnotation(JsonMask.class), beanProperty
            .getContextAnnotation(JsonMask.class));
        if (null == jsonMaskAnnotation) {
            return serializerProvider.findValueSerializer(beanProperty.getType(), beanProperty);
        }
        return new JsonMaskSerializer(jsonMaskAnnotation);
    }
}
