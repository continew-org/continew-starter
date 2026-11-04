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

package top.continew.starter.smoke.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * 最小 Web 冒烟测试：验证组合 web、validation、json-jackson 三个 Starter 后应用可启动且核心契约成立
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class WebMinimalSmokeTest {

    @Autowired
    private TestRestTemplate restTemplate;

    /**
     * 应用可启动，普通对象响应被统一包装为 code=0 的成功结构
     */
    @Test
    void shouldStartAndWrapResponse() {
        ResponseEntity<String> response = restTemplate.getForEntity("/smoke/ok", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DocumentContext body = JsonPath.parse(response.getBody());
        assertEquals("0", body.read("$.code"));
        assertEquals("ok", body.read("$.msg"));
        assertEquals(true, body.read("$.success"));
        assertEquals("smoke", body.read("$.data.value"));
    }

    /**
     * 参数校验失败被全局异常处理捕获，响应为统一异常结构（code=1）
     */
    @Test
    void shouldHandleValidationError() {
        ResponseEntity<String> response =
            restTemplate.getForEntity("/smoke/validate?name=", String.class);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        DocumentContext body = JsonPath.parse(response.getBody());
        assertEquals("1", body.read("$.code"));
        assertEquals(false, body.read("$.success"));
    }
}
