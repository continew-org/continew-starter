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

package top.continew.starter.auth.refresh.token.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.springframework.web.socket.WebSocketHttpHeaders.SEC_WEBSOCKET_PROTOCOL;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.Map;

/**
 * Spring 握手日志与升级策略生命周期契约。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AuthHandshakeHandlerTest {

    @Test
    void shouldRemoveBearerOffersBeforeSpringHandshakeTrace() {
        MockHttpServletRequest servlet = new MockHttpServletRequest("GET", "/websocket");
        servlet.addHeader(SEC_WEBSOCKET_PROTOCOL, "chat.v2, , Bearer.A");
        servlet.addHeader(SEC_WEBSOCKET_PROTOCOL, "Bearer.");
        MockHttpServletResponse response = new MockHttpServletResponse();
        Logger logger = (Logger) LoggerFactory.getLogger(DefaultHandshakeHandler.class);
        Level previousLevel = logger.getLevel();
        boolean additive = logger.isAdditive();
        ListAppender<ILoggingEvent> events = new ListAppender<>();
        events.start();
        logger.addAppender(events);
        logger.setAdditive(false);
        logger.setLevel(Level.TRACE);
        try {
            AuthHandshakeHandler handler = new AuthHandshakeHandler();
            boolean upgraded = handler.doHandshake(new ServletServerHttpRequest(servlet),
                new ServletServerHttpResponse(response), mock(WebSocketHandler.class), Map.of());
            assertThat(upgraded).isFalse();
            assertThat(response.getStatus()).isEqualTo(400);
            assertThat(events.list).filteredOn(event -> event.getLevel() == Level.TRACE)
                .extracting(ILoggingEvent::getFormattedMessage).singleElement().asString()
                .contains("chat.v2").doesNotContain("Bearer.");
        } finally {
            logger.detachAppender(events);
            logger.setLevel(previousLevel);
            logger.setAdditive(additive);
            events.stop();
        }
    }

    @Test
    void shouldForwardContainerLifecycleToSpring() {
        AuthHandshakeHandler handler = new AuthHandshakeHandler();
        handler.setServletContext(new MockServletContext());
        assertThat(handler.isRunning()).isFalse();
        handler.start();
        assertThat(handler.isRunning()).isTrue();
        handler.stop();
        assertThat(handler.isRunning()).isFalse();
    }
}
