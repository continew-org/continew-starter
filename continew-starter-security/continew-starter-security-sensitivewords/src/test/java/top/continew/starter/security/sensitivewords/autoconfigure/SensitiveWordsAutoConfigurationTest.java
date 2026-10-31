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

package top.continew.starter.security.sensitivewords.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.continew.starter.security.sensitivewords.service.DefaultSensitiveWordsService;
import top.continew.starter.security.sensitivewords.service.NoOpSensitiveWordsService;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsService;
import top.continew.starter.security.sensitivewords.validation.SensitiveWords;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;

/**
 * 敏感词自动配置测试
 *
 * @author luoqiz
 * @since 3.0.0
 */
class SensitiveWordsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SensitiveWordsAutoConfiguration.class));

    /**
     * 注册服务与引擎 Bean
     */
    @Test
    @DisplayName("注册服务与引擎 Bean")
    void defaultBeans() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false")
            .run(context -> {
                assertThat(context).hasSingleBean(SensitiveWordsService.class);
                assertThat(context).hasSingleBean(SensitiveWordBs.class);
                assertThat(context.getBean(SensitiveWordsService.class))
                    .isInstanceOf(DefaultSensitiveWordsService.class);
            });
    }

    /**
     * 默认加载内置词库
     *
     * <p>
     * 内置词库来自传递依赖 sensitive-word-data，该依赖缺失时上游静默返回空列表而不报错，
     * 此用例用于守住这条链路，避免词库静默变空后无人发现。
     * </p>
     */
    @Test
    @DisplayName("默认加载内置词库")
    void defaultDictionaryLoaded() {
        runner.run(context -> {
            SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
            assertThat(service.contains("法轮功")).isTrue();
            assertThat(service.contains("这是一段完全正常的文字")).isFalse();
        });
    }

    /**
     * 关闭检测时不注册引擎，服务返回空实现
     */
    @Test
    @DisplayName("enabled=false 时返回空实现")
    void disabled() {
        runner.withPropertyValues("continew-starter.security.sensitive-words.enabled=false")
            .run(context -> {
                assertThat(context).hasSingleBean(SensitiveWordsService.class);
                assertThat(context).doesNotHaveBean(SensitiveWordBs.class);
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                assertThat(service).isInstanceOf(NoOpSensitiveWordsService.class);
                assertThat(service.contains("任意内容")).isFalse();
                assertThat(service.findAll("任意内容")).isEmpty();
                assertThat(service.refresh().denyStaticCount()).isZero();
            });
    }

    /**
     * 关闭检测时 @SensitiveWords 校验全部通过
     *
     * <p>
     * 文档承诺关闭检测后仍可安全保留注解，此处用真实容器与真实校验器验证，避免只靠 mock 自证。
     * </p>
     */
    @Test
    @DisplayName("enabled=false 时 @SensitiveWords 校验通过")
    void disabledValidationPasses() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SensitiveWordsAutoConfiguration.class))
            .withBean(LocalValidatorFactoryBean.class)
            .withPropertyValues("continew-starter.security.sensitive-words.enabled=false",
                "continew-starter.security.sensitive-words.use-default-deny=false",
                "continew-starter.security.sensitive-words.deny-words=敏感内容")
            .run(context -> {
                assertThat(context).hasNotFailed();
                Validator validator = context.getBean(Validator.class);
                assertThat(validator.validate(new ValidatedForm("这是一段敏感内容"))).as("关闭检测后校验应全部通过")
                    .isEmpty();
            });
    }

    /**
     * 启用检测时同一表单校验失败，证明上一个用例不是恒真
     */
    @Test
    @DisplayName("enabled=true 时 @SensitiveWords 校验失败")
    void enabledValidationFails() {
        new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(SensitiveWordsAutoConfiguration.class))
            .withBean(LocalValidatorFactoryBean.class)
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false",
                "continew-starter.security.sensitive-words.deny-words=敏感内容")
            .run(context -> {
                assertThat(context).hasNotFailed();
                Validator validator = context.getBean(Validator.class);
                Set<ConstraintViolation<ValidatedForm>> violations =
                    validator.validate(new ValidatedForm("这是一段敏感内容"));
                assertThat(violations).hasSize(1);
                assertThat(violations.iterator().next().getMessage()).contains("敏感内容");
            });
    }

    /**
     * 关闭内置词库后仅使用配置词
     */
    @Test
    @DisplayName("关闭内置词库后仅使用配置词")
    void disableDefaultDeny() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false",
                "continew-starter.security.sensitive-words.deny-words=配置词")
            .run(context -> {
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                assertThat(service.contains("配置词")).isTrue();
                assertThat(service.refresh().denyStaticCount()).isEqualTo(1);
            });
    }

    /**
     * 自定义词库被自动纳入责任链
     */
    @Test
    @DisplayName("自定义 IWordDeny 被纳入责任链")
    void customDeny() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false")
            .withBean("customDeny", IWordDeny.class, () -> (IWordDeny) () -> List.of("自定义提供词"))
            .run(context -> {
                assertThat(context.getBean(SensitiveWordsService.class).contains("自定义提供词"))
                    .isTrue();
            });
    }

    /**
     * 用户自定义服务接管后自动配置让位
     */
    @Test
    @DisplayName("自定义 SensitiveWordsService 让自动配置让位")
    void customServiceTakesOver() {
        SensitiveWordsService custom = mock(SensitiveWordsService.class);
        when(custom.contains("任意内容")).thenReturn(true);
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false")
            .withBean(SensitiveWordsService.class, () -> custom)
            .run(context -> {
                assertThat(context).hasSingleBean(SensitiveWordsService.class);
                assertThat(context.getBean(SensitiveWordsService.class)).isSameAs(custom);
                assertThat(context.getBean(SensitiveWordsService.class).contains("任意内容")).isTrue();
            });
    }

    /**
     * 自定义引擎覆盖默认实现
     *
     * <p>
     * 运行时增删走引擎级增量接口，与引擎来源无关，自定义引擎下同样生效。
     * </p>
     */
    @Test
    @DisplayName("自定义 SensitiveWordBs 覆盖默认实现")
    void customWordBs() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false")
            .withBean(SensitiveWordBs.class, () -> SensitiveWordBs.newInstance()
                .wordDeny(() -> List.of("自定义引擎词"))
                .wordAllow(List::of)
                .wordFailFast(false)
                .init())
            .run(context -> {
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                assertThat(service.contains("自定义引擎词")).isTrue();

                service.addWord("运行时词");
                assertThat(service.contains("运行时词")).isTrue();
                service.removeWord("运行时词");
                assertThat(service.contains("运行时词")).isFalse();
            });
    }

    /**
     * 配置项接入引擎
     *
     * <p>
     * 钉死 {@code SensitiveWordsProperties} 到引擎参数的接线，防止摘除某一项后无人发现。
     * </p>
     */
    @Test
    @DisplayName("配置项接入引擎")
    void propertiesWiredToEngine() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false",
                "continew-starter.security.sensitive-words.deny-words=法轮功",
                "continew-starter.security.sensitive-words.ignore-repeat=true",
                "continew-starter.security.sensitive-words.num-check=true",
                "continew-starter.security.sensitive-words.email-check=true",
                "continew-starter.security.sensitive-words.url-check=true")
            .run(context -> {
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                // deny-words 接入
                assertThat(service.contains("法轮功")).isTrue();
                // ignore-repeat 接入：重复字符归一化后命中
                assertThat(service.contains("法法轮轮功")).isTrue();
                // num-check 接入：8 位及以上连续数字
                assertThat(service.contains("12345678")).isTrue();
                // email-check 接入
                assertThat(service.contains("a@b.com")).isTrue();
                // url-check 接入
                assertThat(service.contains("http://example.com")).isTrue();
            });
    }

    /**
     * 关闭检测时自定义引擎也不生效
     *
     * <p>
     * 开关语义必须无条件成立：即使注册了自定义 {@code SensitiveWordBs} Bean，
     * {@code enabled=false} 时也应返回空实现，否则关闭检测后自定义引擎仍在工作。
     * </p>
     */
    @Test
    @DisplayName("enabled=false 时自定义引擎也不生效")
    void disabledWithCustomWordBs() {
        runner.withPropertyValues("continew-starter.security.sensitive-words.enabled=false")
            .withBean(SensitiveWordBs.class, () -> SensitiveWordBs.newInstance()
                .wordDeny(() -> List.of("自定义引擎词"))
                .wordAllow(List::of)
                .wordFailFast(false)
                .init())
            .run(context -> {
                assertThat(context).hasSingleBean(SensitiveWordsService.class);
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                assertThat(service).isInstanceOf(NoOpSensitiveWordsService.class);
                assertThat(service.contains("自定义引擎词")).isFalse();
            });
    }

    /**
     * 多个自定义引擎未标注 @Primary 时启动失败
     *
     * <p>
     * 服务 Bean 通过 ObjectProvider 获取唯一引擎。注册多个自定义 {@code SensitiveWordBs}
     * 且未标注 {@code @Primary} 时，容器启动即失败（NoUniqueBeanDefinitionException），
     * 避免静默选用任意一个引擎。文档中的多引擎用法必须配合 @Primary。
     * </p>
     */
    @Test
    @DisplayName("多个自定义引擎未标注 @Primary 时启动失败")
    void multipleEnginesWithoutPrimaryFails() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false")
            .withBean("engine1", SensitiveWordBs.class, () -> SensitiveWordBs.newInstance()
                .wordDeny(() -> List.of("词一"))
                .wordAllow(List::of)
                .wordFailFast(false)
                .init())
            .withBean("engine2", SensitiveWordBs.class, () -> SensitiveWordBs.newInstance()
                .wordDeny(() -> List.of("词二"))
                .wordAllow(List::of)
                .wordFailFast(false)
                .init())
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(NoUniqueBeanDefinitionException.class);
            });
    }

    /**
     * 多个自定义引擎时 @Primary 生效，服务选用主引擎
     */
    @Test
    @DisplayName("多个自定义引擎时 @Primary 指定生效引擎")
    void multipleEnginesWithPrimary() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false")
            .withUserConfiguration(PrimaryEngineConfig.class)
            .run(context -> {
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                assertThat(service.contains("词一")).isTrue();
                assertThat(service.contains("词二")).isFalse();
            });
    }

    /**
     * 多引擎配置（其一标注 @Primary）
     */
    @Configuration
    static class PrimaryEngineConfig {

        @Bean
        @Primary
        public SensitiveWordBs primaryEngine() {
            return SensitiveWordBs.newInstance()
                .wordDeny(() -> List.of("词一"))
                .wordAllow(List::of)
                .wordFailFast(false)
                .init();
        }

        @Bean
        public SensitiveWordBs secondaryEngine() {
            return SensitiveWordBs.newInstance()
                .wordDeny(() -> List.of("词二"))
                .wordAllow(List::of)
                .wordFailFast(false)
                .init();
        }
    }

    /**
     * 运行时词与白名单通过自动配置可用
     */
    @Test
    @DisplayName("自动配置下运行时增删可用")
    void runtimeWordsWork() {
        runner
            .withPropertyValues("continew-starter.security.sensitive-words.use-default-deny=false",
                "continew-starter.security.sensitive-words.deny-words=静态词")
            .run(context -> {
                SensitiveWordsService service = context.getBean(SensitiveWordsService.class);
                service.addWord("临时词");
                assertThat(service.contains("临时词")).isTrue();

                service.addWordAllow("静态词");
                assertThat(service.contains("静态词")).isFalse();

                service.removeWordAllow("静态词");
                service.removeWord("临时词");
                assertThat(service.contains("静态词")).isTrue();
                assertThat(service.contains("临时词")).isFalse();
            });
    }

    /**
     * 校验用表单
     */
    static class ValidatedForm {

        @SensitiveWords
        private final String content;

        ValidatedForm(String content) {
            this.content = content;
        }

        public String getContent() {
            return content;
        }
    }
}
