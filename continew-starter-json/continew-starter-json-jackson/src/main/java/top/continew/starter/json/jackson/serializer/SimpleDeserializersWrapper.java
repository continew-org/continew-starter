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

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.DeserializationConfig;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.type.ClassKey;

/**
 * 反序列化器包装类（重写 Jackson 反序列化枚举方法，参阅：FasterXML/jackson-databind#2842）
 *
 * <p>
 * 默认处理：<br>
 * 1. Jackson 会先查找指定枚举类型对应的反序列化器（例如：GenderEnum 枚举类型，则是找 GenderEnum 枚举类型的对应反序列化器）；<br>
 * 2. 如果找不到则开始查找 Enum 类型（所有枚举父类）的反序列化器；<br>
 * 3. 如果都找不到则会采用默认的枚举反序列化器（它仅能根据枚举类型的 name、ordinal 来进行反序列化）。
 * </p>
 * <p>
 * 重写增强后：<br>
 * 1. 同默认 1；<br>
 * 2. 同默认 2；<br>
 * 3. 如果也找不到 Enum 类型（所有枚举父类）的反序列化器，开始查找指定枚举类型的接口的反序列化器（例如：GenderEnum 枚举类型，则是找它的接口 BaseEnum 的反序列化器）；<br>
 * 4. 同默认 3。
 * </p>
 *
 * @author Charles7c
 * @since 2.4.0
 */
public class SimpleDeserializersWrapper extends SimpleDeserializers {

    @Override
    public ValueDeserializer<?> findEnumDeserializer(JavaType type,
        DeserializationConfig config,
        BeanDescription.Supplier beanDesc) {
        ValueDeserializer<?> deser = super.findEnumDeserializer(type, config, beanDesc);
        if (deser != null) {
            return deser;
        }
        // 重写增强：开始查找指定枚举类型的接口的反序列化器（例如：GenderEnum 枚举类型，则是找它的接口 BaseEnum 的反序列化器）
        for (Class<?> typeInterface : type.getRawClass().getInterfaces()) {
            deser = this._classMappings.get(new ClassKey(typeInterface));
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }
}
