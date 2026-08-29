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

package top.continew.starter.messaging.mqtt.annotation;

import org.springframework.stereotype.Component;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * mqtt topic 监听器
 *
 * @author echo
 * @since 2.15.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Component
public @interface MqttListener {

    /**
     * 要监听的 MQTT 主题
     * <p>支持以下配置方式:
     * <pre>{@code
     * 方式1: 直接指定主题
     * @MqttListener(topic = "sensor/temperature")
     *
     * 方式2: 使用配置文件占位符
     *
     * @MqttListener(topic = "${mqtt.topic}")
     *
     *                     方式3: 使用通配符
     * @MqttListener(topic = "sensor/+/temperature") // 单级通配符
     * @MqttListener(topic = "sensor/#") // 多级通配符
     *                     }</pre>
     *
     *                     <p><b>通配符说明:</b>
     *                     <ul>
     *                     <li>{@code +} - 单级通配符,匹配一个层级的任意内容</li>
     *                     <li>{@code #} - 多级通配符,匹配零个或多个层级,只能用在主题末尾</li>
     *                     </ul>
     *
     * @return MQTT 主题字符串或配置占位符表达式
     */
    String topic();

    /**
     * QoS - 消息传输可靠性等级
     * <p>支持以下配置方式:
     * <ul>
     * <li>直接指定: {@code qos = "0"}, {@code qos = "1"}, {@code qos = "2"}</li>
     * <li>使用占位符: {@code qos = "${mqtt.qos}"}</li>
     * </ul>
     * <p><b>QoS 等级说明:</b>
     * <ul>
     * <li>{@code 0} - 最多一次,消息可能丢失</li>
     * <li>{@code 1} - 至少一次,消息可能重复</li>
     * <li>{@code 2} - 恰好一次,消息不丢失且不重复</li>
     * </ul>
     *
     * @return QoS 等级字符串或配置占位符,默认为 "0"
     */
    String qos() default "0";
}
