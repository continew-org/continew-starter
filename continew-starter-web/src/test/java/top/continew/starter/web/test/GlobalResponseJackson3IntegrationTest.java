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

package top.continew.starter.web.test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import top.continew.starter.web.test.config.GlobalResponseTestConfiguration;

/**
 * 全局响应集成测试（默认 Jackson 3 场景，不配置 preferred-json-mapper）
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest(
    properties = {"continew-starter.web.response.print-exception-in-global-advice=false"})
@AutoConfigureMockMvc
@Import(GlobalResponseTestConfiguration.class)
class GlobalResponseJackson3IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldWrapJsonResponse() throws Exception {
        mockMvc.perform(get("/test/ok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.data.value").value("hello"));
    }

    @Test
    void shouldHandleUnmatchedException() throws Exception {
        mockMvc.perform(get("/test/exception"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("1"));
    }
}
