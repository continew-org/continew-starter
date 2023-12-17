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

package top.charles7c.continew.starter.log.common.model;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 可记录的 HTTP 请求信息
 *
 * @author Andy Wilkinson（Spring Boot Actuator）
 * @author Phillip Webb（Spring Boot Actuator）
 * @author Charles7c
 * @see RecordableHttpResponse
 * @since 1.1.0
 */
public interface RecordableHttpRequest {

    /**
     * 获取请求方式
     *
     * @return 请求方式
     */
    String getMethod();

    /**
     * 获取 URI
     *
     * @return URI
     */
    URI getUri();

    /**
     * 获取 IP
     *
     * @return IP
     */
    String getIp();

    /**
     * 获取请求头
     *
     * @return 请求头
     */
    Map<String, List<String>> getHeaders();

    /**
     * 获取请求体
     *
     * @return 请求体
     */
    String getBody();

    /**
     * 获取请求参数
     *
     * @return 请求参数
     */
    Map<String, Object> getParam();
}
