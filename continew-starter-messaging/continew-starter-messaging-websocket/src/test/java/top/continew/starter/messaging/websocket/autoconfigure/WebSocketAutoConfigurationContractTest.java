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

package top.continew.starter.messaging.websocket.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import top.continew.starter.messaging.websocket.core.WebSocketClientService;
import top.continew.starter.messaging.websocket.dao.DefaultWebSocketSessionDao;
import top.continew.starter.messaging.websocket.dao.WebSocketSessionDao;
import top.continew.starter.test.support.AutoConfigurationImportsAssert;

/**
 * WebSocket 自动配置装配契约测试
 *
 * <p>本模块依赖 Servlet 环境（{@code @EnableWebSocket} 注册 {@code WebSocketConfigurer}），
 * 因此使用 {@code WebApplicationContextRunner} 而非普通 runner。装配存在一条硬性要求：
 * {@code WebSocketClientService} 必须由使用方提供，模块默认工厂方法直接抛出
 * {@code NoSuchBeanDefinitionException}，缺实现时应当启动失败而非静默放行，该语义在此固化。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class WebSocketAutoConfigurationContractTest {

    private static final String BASE_PACKAGE = "top.continew.starter.messaging.websocket";

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(WebSocketAutoConfiguration.class))
        .withBean(WebSocketClientService.class, () -> mock(WebSocketClientService.class));

    /**
     * 模块包内扫描到的自动配置类应全部登记进 imports 资源。
     */
    @Test
    void shouldRegisterAllAutoConfigurationsIntoImports() {
        assertThat(AutoConfigurationImportsAssert.findUnregistered(BASE_PACKAGE)).isEmpty();
    }

    /**
     * 默认启用（matchIfMissing）且使用方提供客户端服务时应完整装配。
     */
    @Test
    void shouldRegisterBeansByDefaultWithClientService() {
        this.contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WebSocketConfigurer.class);
            assertThat(context).hasSingleBean(WebSocketSessionDao.class);
        });
    }

    /**
     * 缺少使用方客户端服务实现时应启动失败，不得静默放行。
     */
    @Test
    void shouldFailWhenClientServiceMissing() {
        WebApplicationContextRunner bareRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(WebSocketAutoConfiguration.class));
        bareRunner.run(context -> {
            assertThat(context.getStartupFailure()).isNotNull();
            assertThat(context.getStartupFailure())
                .hasRootCauseInstanceOf(NoSuchBeanDefinitionException.class);
        });
    }

    /**
     * 显式关闭后应整体跳过装配，不得产生 WebSocket 侧 Bean。
     */
    @Test
    void shouldSkipWhenDisabled() {
        this.contextRunner.withPropertyValues("continew-starter.messaging.websocket.enabled=false")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).doesNotHaveBean(WebSocketConfigurer.class);
                assertThat(context).doesNotHaveBean(WebSocketSessionDao.class);
            });
    }

    /**
     * 使用方自定义会话 DAO 时应替换默认实现（@ConditionalOnMissingBean 退让）。
     */
    @Test
    void shouldUseCustomSessionDaoInsteadOfDefault() {
        WebSocketSessionDao customDao = mock(WebSocketSessionDao.class);
        this.contextRunner.withBean(WebSocketSessionDao.class, () -> customDao)
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(WebSocketSessionDao.class);
                assertThat(context.getBean(WebSocketSessionDao.class)).isSameAs(customDao);
                assertThat(context.getBean(WebSocketSessionDao.class))
                    .isNotInstanceOf(DefaultWebSocketSessionDao.class);
            });
    }

    /**
     * path 配置应绑定到 WebSocket 属性。
     */
    @Test
    void shouldBindPathProperty() {
        this.contextRunner.withPropertyValues("continew-starter.messaging.websocket.path=/ws-test")
            .run(context -> {
                assertThat(context).hasNotFailed();
                WebSocketProperties properties = context.getBean(WebSocketProperties.class);
                assertThat(properties.getPath()).isEqualTo("/ws-test");
            });
    }
}
