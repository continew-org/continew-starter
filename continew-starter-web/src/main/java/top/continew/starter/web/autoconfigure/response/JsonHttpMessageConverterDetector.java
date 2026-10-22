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

package top.continew.starter.web.autoconfigure.response;

import org.jspecify.annotations.Nullable;
import org.springframework.http.converter.HttpMessageConverter;

import java.util.Set;

/**
 * JSON 消息转换器识别器
 * <p>
 * 按父类名称判断，避免硬引用具体 Jackson 版本类型。Jackson 2 与 Jackson 3 的转换器没有共同父类，
 * 只按其中一种判断会导致另一种场景下响应包装静默失效。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
final class JsonHttpMessageConverterDetector {

    private static final Set<String> JSON_CONVERTER_BASE_TYPES = Set.of(
        "org.springframework.http.converter.AbstractJacksonHttpMessageConverter",
        "org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter",
        "org.springframework.http.converter.json.AbstractJsonHttpMessageConverter");

    private JsonHttpMessageConverterDetector() {
    }

    /**
     * 是否为 JSON 消息转换器
     *
     * @param converterType            转换器类型
     * @param customConverterClassName 自定义 JSON 转换器类名
     * @return 是否为 JSON 消息转换器
     */
    static boolean isJsonConverter(Class<? extends HttpMessageConverter<?>> converterType,
        @Nullable String customConverterClassName) {
        for (Class<?> type = converterType; type != null; type = type.getSuperclass()) {
            if (JSON_CONVERTER_BASE_TYPES.contains(type.getName())) {
                return true;
            }
        }
        return customConverterClassName != null && customConverterClassName.equals(converterType
            .getName());
    }
}
