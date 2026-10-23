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

package top.continew.starter.web.autoconfigure.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.http.converter.autoconfigure.ServerHttpMessageConvertersCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.format.FormatterRegistry;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.continew.starter.web.autoconfigure.mvc.converter.BaseEnumConverterFactory;
import top.continew.starter.web.autoconfigure.mvc.converter.time.DateConverter;
import top.continew.starter.web.autoconfigure.mvc.converter.time.LocalDateConverter;
import top.continew.starter.web.autoconfigure.mvc.converter.time.LocalDateTimeConverter;
import top.continew.starter.web.autoconfigure.mvc.converter.time.LocalTimeConverter;

import jakarta.annotation.PostConstruct;

/**
 * Web MVC 自动配置
 *
 * @author Charles7c
 * @since 2.4.0
 */
@AutoConfiguration
public class WebMvcAutoConfiguration implements WebMvcConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebMvcAutoConfiguration.class);

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new BaseEnumConverterFactory());
        registry.addConverter(new DateConverter());
        registry.addConverter(new LocalDateTimeConverter());
        registry.addConverter(new LocalDateConverter());
        registry.addConverter(new LocalTimeConverter());
    }

    /**
     * 消息转换器定制
     * <p>
     * 自定义 converters 时，需要手动在最前面添加 ByteArrayHttpMessageConverter，否则 Spring Doc OpenAPI 的
     * {@code /*}{@code /api-docs/**}（例如：/v3/api-docs/default）接口响应内容会变为 Base64 编码后的内容，
     * 最终导致接口文档解析失败。详情请参阅：https://github.com/springdoc/springdoc-openapi/issues/2143
     * </p>
     *
     * @return 服务端消息转换器定制器
     */
    @Bean
    public ServerHttpMessageConvertersCustomizer continewServerHttpMessageConvertersCustomizer() {
        return builder -> builder.configureMessageConvertersList(converters -> {
            if (converters.stream().noneMatch(ByteArrayHttpMessageConverter.class::isInstance)) {
                converters.add(0, new ByteArrayHttpMessageConverter());
            }
        });
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug("[ContiNew Starter] - Auto Configuration 'Web MVC' completed initialization.");
    }
}
