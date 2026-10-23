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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.continew.starter.web.test.config.GlobalResponseTestConfiguration;

/**
 * 全局响应国际化集成测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest(properties = {"continew-starter.web.response.i18n=true",
    "spring.messages.basename=i18n/messages",
    "continew-starter.web.response.print-exception-in-global-advice=false"})
@AutoConfigureMockMvc
@Import(GlobalResponseTestConfiguration.class)
class GlobalResponseI18nIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRenderI18nMessage() throws Exception {
        mockMvc.perform(get("/test/ok").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.msg").value("操作成功"));
    }

    @Test
    void shouldRenderI18nMessageForFailure() throws Exception {
        mockMvc.perform(get("/test/exception").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("1"))
            .andExpect(jsonPath("$.msg").value("服务器异常，请联系管理员"));
    }

    @Test
    void shouldKeepOriginalMessageWhenCodeMissing() throws Exception {
        mockMvc.perform(get("/test/alias").header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
            .andExpect(status().is(418))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.msg").value("别名异常"));
    }

    @Test
    void shouldKeepValidationMessage() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"\"}")
            .header(HttpHeaders.ACCEPT_LANGUAGE, "zh-CN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("1"))
            .andExpect(jsonPath("$.msg").value("name must not be blank"));
    }
}
