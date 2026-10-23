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

package top.continew.starter.json.jackson.autoconfigure;

import cn.hutool.core.date.DatePattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import tools.jackson.databind.ext.javatime.deser.DurationDeserializer;
import tools.jackson.databind.ext.javatime.deser.InstantDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.deser.LocalTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.DurationSerializer;
import tools.jackson.databind.ext.javatime.ser.InstantSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.ext.javatime.ser.LocalTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;
import top.continew.starter.core.enums.BaseEnum;
import top.continew.starter.core.util.GeneralPropertySourceFactory;
import top.continew.starter.json.jackson.serializer.BaseEnumDeserializer;
import top.continew.starter.json.jackson.serializer.BaseEnumSerializer;
import top.continew.starter.json.jackson.serializer.BigNumberSerializer;
import top.continew.starter.json.jackson.serializer.SimpleDeserializersWrapper;

import java.math.BigInteger;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * Jackson 自动配置
 *
 * @author Charles7c
 * @author Jasmine
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(JacksonExtensionProperties.class)
@PropertySource(value = "classpath:default-json-jackson.yml",
    factory = GeneralPropertySourceFactory.class)
public class JacksonAutoConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonAutoConfiguration.class);
    private final JacksonExtensionProperties properties;

    public JacksonAutoConfiguration(JacksonExtensionProperties properties) {
        this.properties = properties;
    }

    @Bean
    public JsonMapperBuilderCustomizer jsonMapperBuilderCustomizer() {
        return builder -> {
            // 时区跟随 JVM 默认值（优先级高于 spring.jackson.time-zone 配置），与 2.x 行为保持一致
            builder.defaultTimeZone(TimeZone.getDefault());
            builder.addModule(javaTimeModule());
            builder.addModule(baseEnumModule());
            builder.addModule(bigNumberModule());
            LOGGER.debug(
                "[ContiNew Starter] - Auto Configuration 'Jackson' completed initialization.");
        };
    }

    /**
     * 日期时间序列化及反序列化配置
     *
     * @return {@link SimpleModule}
     * @since 1.0.0
     */
    private SimpleModule javaTimeModule() {
        SimpleModule javaTimeModule = new SimpleModule();
        // 针对时间类型：LocalDateTime 的序列化和反序列化处理
        DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern(DatePattern.NORM_DATETIME_PATTERN);
        javaTimeModule.addSerializer(LocalDateTime.class,
            new LocalDateTimeSerializer(dateTimeFormatter));
        javaTimeModule.addDeserializer(LocalDateTime.class,
            new LocalDateTimeDeserializer(dateTimeFormatter));
        // 针对时间类型：LocalDate 的序列化和反序列化处理
        DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern(DatePattern.NORM_DATE_PATTERN);
        javaTimeModule.addSerializer(LocalDate.class, new LocalDateSerializer(dateFormatter));
        javaTimeModule.addDeserializer(LocalDate.class, new LocalDateDeserializer(dateFormatter));
        // 针对时间类型：LocalTime 的序列化和反序列化处理
        DateTimeFormatter timeFormatter =
            DateTimeFormatter.ofPattern(DatePattern.NORM_TIME_PATTERN);
        javaTimeModule.addSerializer(LocalTime.class, new LocalTimeSerializer(timeFormatter));
        javaTimeModule.addDeserializer(LocalTime.class, new LocalTimeDeserializer(timeFormatter));
        // 针对时间类型：Instant 的序列化和反序列化处理
        javaTimeModule.addSerializer(Instant.class, InstantSerializer.INSTANCE);
        javaTimeModule.addDeserializer(Instant.class, InstantDeserializer.INSTANT);
        // 针对时间类型：Duration 的序列化和反序列化处理
        javaTimeModule.addSerializer(Duration.class, DurationSerializer.INSTANCE);
        javaTimeModule.addDeserializer(Duration.class, DurationDeserializer.INSTANCE);
        return javaTimeModule;
    }

    /**
     * 枚举序列化及反序列化配置
     *
     * @return SimpleModule /
     * @since 2.4.0
     */
    private SimpleModule baseEnumModule() {
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(BaseEnum.class, BaseEnumSerializer.SERIALIZER_INSTANCE);
        SimpleDeserializersWrapper deserializers = new SimpleDeserializersWrapper();
        deserializers.addDeserializer(BaseEnum.class, new BaseEnumDeserializer());
        simpleModule.setDeserializers(deserializers);
        return simpleModule;
    }

    /**
     * 大数值序列化及反序列化配置
     *
     * @return SimpleModule /
     * @since 2.12.1
     */
    private SimpleModule bigNumberModule() {
        SimpleModule bigNumberModule = new SimpleModule();
        switch (properties.getBigNumberSerializeMode()) {
            case FLEXIBLE -> {
                bigNumberModule.addSerializer(Long.class, BigNumberSerializer.SERIALIZER_INSTANCE);
                bigNumberModule.addSerializer(Long.TYPE, BigNumberSerializer.SERIALIZER_INSTANCE);
                bigNumberModule.addSerializer(BigInteger.class,
                    BigNumberSerializer.SERIALIZER_INSTANCE);
            }
            case TO_STRING -> {
                bigNumberModule.addSerializer(Long.class, ToStringSerializer.instance);
                bigNumberModule.addSerializer(Long.TYPE, ToStringSerializer.instance);
                bigNumberModule.addSerializer(BigInteger.class, ToStringSerializer.instance);
            }
            default ->
                LOGGER.warn("[ContiNew Starter] - Jackson 大数值序列化模式：NO_OPERATE，超过 JS 范围的数值会损失精度");
        }
        return bigNumberModule;
    }
}
