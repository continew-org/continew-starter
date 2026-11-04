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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.StdSerializer;
import top.continew.starter.json.jackson.enums.BigNumberSerializeMode;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

import java.time.LocalDateTime;
import java.util.TimeZone;

/**
 * Jackson 自动配置契约测试
 *
 * <p>固化 JacksonAutoConfiguration 的装配契约：imports 登记完整性、默认注册
 * JsonMapperBuilderCustomizer、与 Boot Jackson 自动配置组合后定制对最终 JsonMapper
 * 生效、大数值序列化模式属性绑定、用户 customizer 并存扩展点。</p>
 *
 * <p>本模块无装配开关，也无 @ConditionalOnMissingBean 回退点（customizer 为无条件注册，
 * Boot 按集合语义合并全部 customizer），开关与替换式 fallback 用例不适用，用户扩展
 * 契约以「并存且生效」固化。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class JacksonAutoConfigurationContractTest {

    /**
     * Boot 4.1 的 Jackson 自动配置与本模块自动配置类同名，以全限定类字面量消歧组合。
     */
    private static final Class<?> BOOT_JACKSON_AUTO_CONFIGURATION =
        org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration.class;

    private final ApplicationContextRunner starterRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration.class));

    private final ApplicationContextRunner fullRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(BOOT_JACKSON_AUTO_CONFIGURATION,
            JacksonAutoConfiguration.class));

    /**
     * classpath 上扫描到的 @AutoConfiguration 类型应全部登记进 imports 资源。
     */
    @Test
    void autoConfigurationImportsShouldBeComplete() {
        assertTrue(AutoConfigurationImportsAssert
            .findUnregistered("top.continew.starter.json.jackson").isEmpty());
    }

    /**
     * 默认应注册本模块的 JsonMapperBuilderCustomizer。
     */
    @Test
    void shouldRegisterCustomizerByDefault() {
        this.starterRunner.run(context -> assertThat(context)
            .hasSingleBean(JsonMapperBuilderCustomizer.class));
    }

    /**
     * 与 Boot Jackson 自动配置组合时应产出最终 JsonMapper，且本模块的日期时间格式
     * 定制对其生效（LocalDateTime 按 yyyy-MM-dd HH:mm:ss 输出）。
     */
    @Test
    void shouldApplyDateTimeCustomizationToFinalMapper() {
        this.fullRunner.run(context -> {
            assertThat(context).hasSingleBean(JsonMapper.class);
            JsonMapper mapper = context.getBean(JsonMapper.class);
            assertThat(mapper.writeValueAsString(LocalDateTime.of(2026, 10, 23, 8, 30, 0)))
                .isEqualTo("\"2026-10-23 08:30:00\"");
        });
    }

    /**
     * 大数值序列化模式默认 FLEXIBLE：安全范围内的 Long 仍输出数字。
     */
    @Test
    void shouldKeepFlexibleBigNumberModeByDefault() {
        this.fullRunner.run(context -> {
            assertThat(context.getBean(JacksonExtensionProperties.class)
                .getBigNumberSerializeMode()).isEqualTo(BigNumberSerializeMode.FLEXIBLE);
            assertThat(context.getBean(JsonMapper.class).writeValueAsString(Long.valueOf(123L)))
                .isEqualTo("123");
        });
    }

    /**
     * 大数值序列化模式覆盖为 TO_STRING 后：Long 一律输出字符串（安全范围同样字符串化）。
     */
    @Test
    void shouldSwitchBigNumberModeToToString() {
        this.fullRunner
            .withPropertyValues("spring.jackson.big-number-serialize-mode=to_string")
            .run(context -> {
                assertThat(context.getBean(JacksonExtensionProperties.class)
                    .getBigNumberSerializeMode()).isEqualTo(BigNumberSerializeMode.TO_STRING);
                assertThat(context.getBean(JsonMapper.class).writeValueAsString(Long.valueOf(123L)))
                    .isEqualTo("\"123\"");
            });
    }

    /**
     * 用户 customizer 应与本模块 customizer 并存，且对最终 JsonMapper 生效。
     */
    @Test
    void shouldCoexistAndApplyUserCustomizer() {
        JsonMapperBuilderCustomizer userCustomizer = new UserPayloadCustomizer();
        // 用户 customizer 需指定 bean 名，避免与本模块同名 @Bean 方法产生定义覆盖冲突
        this.fullRunner
            .withBean("userJsonMapperBuilderCustomizer", JsonMapperBuilderCustomizer.class,
                () -> userCustomizer)
            .run(context -> {
                assertThat(context).getBeans(JsonMapperBuilderCustomizer.class)
                    .containsValue(userCustomizer);
                assertThat(context.getBean(JsonMapper.class).writeValueAsString(new UserPayload()))
                    .isEqualTo("\"user-payload\"");
            });
    }

    /**
     * 用户扩展载荷类型，用于验证用户 customizer 的序列化定制生效。
     */
    static class UserPayload {
    }

    /**
     * 用户扩展的定制器：为本模块未覆盖的载荷类型追加序列化器，模拟业务侧扩展。
     */
    static class UserPayloadCustomizer implements JsonMapperBuilderCustomizer {

        @Override
        public void customize(JsonMapper.Builder builder) {
            // 时区定制覆盖 starter 的 JVM 默认值设置，验证用户 customizer 后置生效
            builder.defaultTimeZone(TimeZone.getTimeZone("UTC"));
            SimpleModule module = new SimpleModule();
            module.addSerializer(UserPayload.class, new UserPayloadSerializer());
            builder.addModule(module);
        }
    }

    /**
     * 用户载荷的固定值序列化器，输出不依赖对象内容，断言结果稳定。
     */
    static class UserPayloadSerializer extends StdSerializer<UserPayload> {

        UserPayloadSerializer() {
            super(UserPayload.class);
        }

        @Override
        public void serialize(UserPayload value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString("user-payload");
        }
    }
}
