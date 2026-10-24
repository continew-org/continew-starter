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

package top.continew.starter.auth.satoken.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.handler.MappedInterceptor;
import top.continew.starter.core.constant.StringConstants;

import java.util.List;

/**
 * SaToken 自动配置测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class SaTokenAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(SaTokenAutoConfiguration.class))
        .withPropertyValues("sa-token.extension.enabled=true");

    /**
     * JWT 模式开启时注入 StpLogicJwtForSimple
     */
    @Test
    @DisplayName("jwt-enabled=true 时注入 StpLogicJwtForSimple")
    void shouldInjectJwtStpLogic() {
        runner.withPropertyValues("sa-token.extension.jwt-enabled=true")
            .run(context -> assertThat(context.getBean(StpLogic.class))
                .isInstanceOf(StpLogicJwtForSimple.class));
    }

    /**
     * JWT 模式关闭时不注入 StpLogic
     */
    @Test
    @DisplayName("jwt-enabled 缺省时不注入 StpLogic")
    void shouldNotInjectStpLogicWithoutJwt() {
        runner.run(context -> assertThat(context).doesNotHaveBean(StpLogic.class));
    }

    /**
     * 非 Web 环境不装配 Web 层配置
     */
    @Test
    @DisplayName("非 Web 环境不装配 SaTokenWebConfiguration")
    void shouldNotLoadWebConfigurationWithoutWebApp() {
        runner.run(context -> assertThat(context).doesNotHaveBean(SaTokenWebConfiguration.class));
    }

    /**
     * 装配默认登录拦截器且允许使用方覆盖
     */
    @Test
    @DisplayName("装配默认 SaInterceptor 且允许使用方覆盖")
    void shouldAssembleAndAllowOverrideSaInterceptor() {
        runner.run(context -> assertThat(context).hasSingleBean(SaInterceptor.class));
        SaInterceptor customInterceptor = new SaInterceptor(handle -> {
        });
        runner.withBean(SaInterceptor.class, () -> customInterceptor)
            .run(context -> assertThat(context.getBean(SaInterceptor.class))
                .isSameAs(customInterceptor));
    }

    /**
     * Web 层注册登录拦截器
     */
    @Test
    @DisplayName("SaTokenWebConfiguration 注册登录拦截器")
    void shouldRegisterInterceptor() {
        SaInterceptor interceptor = new SaInterceptor(handle -> {
        });
        SaTokenWebConfiguration configuration = new SaTokenWebConfiguration(interceptor);
        VisibleInterceptorRegistry registry = new VisibleInterceptorRegistry();
        configuration.addInterceptors(registry);
        assertThat(registry.getInterceptors()).hasSize(1);
        MappedInterceptor mappedInterceptor = (MappedInterceptor) registry.getInterceptors().get(0);
        assertThat(mappedInterceptor.getInterceptor()).isSameAs(interceptor);
        assertThat(mappedInterceptor.getIncludePathPatterns())
            .containsExactly(StringConstants.PATH_PATTERN);
    }

    /**
     * 放开 getInterceptors 访问级的测试用注册器
     */
    static class VisibleInterceptorRegistry extends InterceptorRegistry {

        /**
         * 获取已注册拦截器
         *
         * @return 已注册拦截器列表
         */
        @Override
        public List<Object> getInterceptors() {
            return super.getInterceptors();
        }
    }
}
