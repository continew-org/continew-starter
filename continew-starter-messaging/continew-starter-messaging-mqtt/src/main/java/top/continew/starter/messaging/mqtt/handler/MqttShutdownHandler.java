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

package top.continew.starter.messaging.mqtt.handler;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;

/**
 * mqtt关闭处理程序
 *
 * @author echo
 * @since 2.15.0
 */
@SuppressWarnings("ClassCanBeRecord")
public class MqttShutdownHandler
    implements DisposableBean, ApplicationListener<ContextClosedEvent> {

    private final MqttPahoMessageDrivenChannelAdapter inboundAdapter;
    private final MqttPahoMessageHandler outboundHandler;

    public MqttShutdownHandler(MqttPahoMessageDrivenChannelAdapter inboundAdapter,
        MqttPahoMessageHandler outboundHandler) {
        this.inboundAdapter = inboundAdapter;
        this.outboundHandler = outboundHandler;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        stopMqttConnections();
    }

    @Override
    public void destroy() throws Exception {
        stopMqttConnections();
    }

    private void stopMqttConnections() {
        try {
            if (inboundAdapter != null && inboundAdapter.isRunning()) {
                inboundAdapter.stop();
            }

            if (outboundHandler != null) {
                outboundHandler.stop();
            }
            // 给一点时间让连接正常关闭
            Thread.sleep(500);
        } catch (Exception ignored) {
        }
    }
}
