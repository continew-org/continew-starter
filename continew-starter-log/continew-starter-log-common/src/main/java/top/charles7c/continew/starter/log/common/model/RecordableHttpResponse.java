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

import java.util.List;
import java.util.Map;

/**
 * 可记录的 HTTP 响应信息
 *
 * @author Andy Wilkinson（Spring Boot Actuator）
 * @author Charles7c
 * @see RecordableHttpRequest
 * @since 1.1.0
 */
public interface RecordableHttpResponse {

    /**
     * 获取状态码
     *
     * @return 状态码
     */
    int getStatus();

    /**
     * 获取响应头
     *
     * @return 响应头
     */
    Map<String, List<String>> getHeaders();

    /**
     * 获取响应体
     *
     * @return 响应体
     */
    String getBody();

    /**
     * 获取响应参数
     *
     * @return 响应参数
     */
    Map<String, Object> getParam();
}
