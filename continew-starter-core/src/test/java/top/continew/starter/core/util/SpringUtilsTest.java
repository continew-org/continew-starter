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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring 工具类测试
 *
 * <p>
 * 路径匹配为纯逻辑断言；容器相关方法通过静态模拟 {@link SpringUtil} 提供受控上下文。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SpringUtilsTest {

    @Test
    @DisplayName("isMatch 支持 PathPattern 的 ** 通配")
    void shouldMatchDoubleWildcardPattern() {
        assertThat(SpringUtils.isMatch("/user/list", "/user/**")).isTrue();
        assertThat(SpringUtils.isMatch("/user/list/detail", "/user/**")).isTrue();
        assertThat(SpringUtils.isMatch("/role/list", "/user/**")).isFalse();
    }

    @Test
    @DisplayName("isMatch 支持 * 单层与 ? 单字符通配")
    void shouldMatchSingleWildcardPattern() {
        assertThat(SpringUtils.isMatch("/user/list", "/user/*")).isTrue();
        assertThat(SpringUtils.isMatch("/user/list/detail", "/user/*")).isFalse();
        assertThat(SpringUtils.isMatch("/user/1", "/user/?")).isTrue();
        assertThat(SpringUtils.isMatch("/user/12", "/user/?")).isFalse();
    }

    @Test
    @DisplayName("isMatch 精确模式要求路径全等")
    void shouldRequireExactMatchWithoutWildcard() {
        assertThat(SpringUtils.isMatch("/user", "/user")).isTrue();
        assertThat(SpringUtils.isMatch("/user/list", "/user")).isFalse();
    }

    @Test
    @DisplayName("isMatch 列表重载命中任一模式即匹配")
    void shouldMatchAnyPatternInList() {
        assertThat(SpringUtils.isMatch("/user/list", List.of("/role/**", "/user/**"))).isTrue();
        assertThat(SpringUtils.isMatch("/menu/list", List.of("/role/**", "/user/**"))).isFalse();
    }

    @Test
    @DisplayName("isMatch 可变参数重载命中任一模式即匹配")
    void shouldMatchAnyPatternInVarargs() {
        assertThat(SpringUtils.isMatch("/user/list", "/role/**", "/user/**")).isTrue();
        assertThat(SpringUtils.isMatch("/menu/list", "/role/**", "/user/**")).isFalse();
    }

    @Test
    @DisplayName("isMatchAnt 支持 Ant 风格双星跨层匹配")
    void shouldMatchAntStylePattern() {
        assertThat(SpringUtils.isMatchAnt("/user/list", "/user/*")).isTrue();
        assertThat(SpringUtils.isMatchAnt("/a/b/c.html", "/a/b/*")).isTrue();
        assertThat(SpringUtils.isMatchAnt("/a/b/c.html", "/a/*.html")).isFalse();
    }

    @Test
    @DisplayName("isMatchAnt 列表重载命中任一模式即匹配")
    void shouldMatchAnyAntPatternInList() {
        assertThat(SpringUtils.isMatchAnt("/user/list", List.of("/role/*", "/user/*"))).isTrue();
        assertThat(SpringUtils.isMatchAnt("/menu/list", List.of("/role/*", "/user/*"))).isFalse();
    }

    @Test
    @DisplayName("getBean 忽略 Bean 缺失异常时返回 null")
    void shouldReturnNullWhenIgnoreMissingBean() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(String.class))
                .thenThrow(new NoSuchBeanDefinitionException(String.class));
            assertThat(SpringUtils.getBean(String.class, true)).isNull();
        }
    }

    @Test
    @DisplayName("getBean 不忽略 Bean 缺失异常时原样抛出")
    void shouldThrowWhenNotIgnoreMissingBean() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            NoSuchBeanDefinitionException missing = new NoSuchBeanDefinitionException(String.class);
            springUtil.when(() -> SpringUtil.getBean(String.class)).thenThrow(missing);
            Assertions.assertThatThrownBy(() -> SpringUtils.getBean(String.class, false))
                .isSameAs(missing);
        }
    }

    @Test
    @DisplayName("getProxy 从容器返回同类 Bean")
    void shouldReturnBeanFromContainer() {
        String proxy = "container-bean";
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(String.class)).thenReturn(proxy);
            assertThat(SpringUtils.getProxy("target")).isEqualTo(proxy);
        }
    }

    @Test
    @DisplayName("deRegisterResourceHandler 按补全的路径模式移除既有映射")
    void shouldRemoveRegisteredResourceHandler() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        Map<Object, Object> handlerMap = new HashMap<>();
        handlerMap.put("/static/**", mock(ResourceHttpRequestHandler.class));
        // SimpleUrlHandlerMapping 的 handlerMap 在容器装配期填充，测试直接注入受控映射表
        ReflectionTestUtils.setField(mapping, "handlerMap", handlerMap);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            ApplicationContext context = mock(ApplicationContext.class);
            when(context.getBean("resourceHandlerMapping", HandlerMapping.class))
                .thenReturn(mapping);
            springUtil.when(SpringUtil::getApplicationContext).thenReturn(context);
            SpringUtils.deRegisterResourceHandler(Map.of("/static", "/data/static"));
            assertThat(mapping.getHandlerMap()).doesNotContainKey("/static/**");
        }
    }

    @Test
    @DisplayName("getHandlerMethod 对缺失处理器映射的请求返回 null")
    void shouldReturnNullWhenHandlerMappingMissing() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil
                .when(() -> SpringUtil.getBean("requestMappingHandlerMapping",
                    RequestMappingHandlerMapping.class))
                .thenThrow(new NoSuchBeanDefinitionException(RequestMappingHandlerMapping.class));
            assertThat(SpringUtils.getHandlerMethod(mock(HttpServletRequest.class))).isNull();
        }
    }

    @Test
    @DisplayName("getHandlerMethod 对无处理链的请求返回 null")
    void shouldReturnNullWhenNoHandlerChain() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandler(request)).thenReturn(null);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil
                .when(() -> SpringUtil.getBean("requestMappingHandlerMapping",
                    RequestMappingHandlerMapping.class))
                .thenReturn(handlerMapping);
            assertThat(SpringUtils.getHandlerMethod(request)).isNull();
        }
    }

    @Test
    @DisplayName("getHandlerMethod 对非 HandlerMethod 处理器返回 null")
    void shouldReturnNullWhenHandlerNotHandlerMethod() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandler(request))
            .thenReturn(new HandlerExecutionChain(mock(ResourceHttpRequestHandler.class)));
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil
                .when(() -> SpringUtil.getBean("requestMappingHandlerMapping",
                    RequestMappingHandlerMapping.class))
                .thenReturn(handlerMapping);
            assertThat(SpringUtils.getHandlerMethod(request)).isNull();
        }
    }

    @Test
    @DisplayName("getHandlerMethod 返回 HandlerMethod 类型处理器")
    void shouldReturnHandlerMethod() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HandlerMethod handlerMethod = mock(HandlerMethod.class);
        RequestMappingHandlerMapping handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(handlerMapping.getHandler(request))
            .thenReturn(new HandlerExecutionChain(handlerMethod));
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil
                .when(() -> SpringUtil.getBean("requestMappingHandlerMapping",
                    RequestMappingHandlerMapping.class))
                .thenReturn(handlerMapping);
            assertThat(SpringUtils.getHandlerMethod(request)).isSameAs(handlerMethod);
        }
    }

    @Test
    @DisplayName("deRegisterResourceHandler 对未注册模式保持映射表其余键不变")
    void shouldKeepOtherHandlersWhenRemoveAbsentPattern() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        ResourceHttpRequestHandler keptHandler = mock(ResourceHttpRequestHandler.class);
        Map<Object, Object> handlerMap = new HashMap<>();
        handlerMap.put("/static/**", keptHandler);
        // SimpleUrlHandlerMapping 的 handlerMap 在容器装配期填充，测试直接注入受控映射表
        ReflectionTestUtils.setField(mapping, "handlerMap", handlerMap);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            ApplicationContext context = mock(ApplicationContext.class);
            when(context.getBean("resourceHandlerMapping", HandlerMapping.class))
                .thenReturn(mapping);
            springUtil.when(SpringUtil::getApplicationContext).thenReturn(context);
            SpringUtils.deRegisterResourceHandler(Map.of("/absent", "/data/absent"));
            assertThat(mapping.getHandlerMap()).containsKey("/static/**");
        }
    }
}
