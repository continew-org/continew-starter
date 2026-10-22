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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import top.continew.starter.web.test.config.GlobalResponseTestConfiguration;

/**
 * 全局响应集成测试（Jackson 2）
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest(
    properties = {"continew-starter.web.response.print-exception-in-global-advice=false",
        "continew-starter.web.response.exclude-urls=/test/excluded-url"})
@AutoConfigureMockMvc
@Import(GlobalResponseTestConfiguration.class)
class GlobalResponseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Environment environment;

    @Test
    void shouldPreferJackson2JsonMapper() {
        assertEquals("jackson2", environment
            .getProperty("spring.http.converters.preferred-json-mapper"));
    }

    @Test
    void shouldWrapJsonResponse() throws Exception {
        mockMvc.perform(get("/test/ok"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"))
            .andExpect(jsonPath("$.msg").value("ok"))
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.timestamp").isNumber())
            .andExpect(jsonPath("$.data.value").value("hello"));
    }

    @Test
    void shouldWrapVoidResponse() throws Exception {
        mockMvc.perform(get("/test/void"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void shouldNotWrapExcludedMethod() throws Exception {
        mockMvc.perform(get("/test/excluded"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("raw"))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void shouldNotWrapExcludedReturnType() throws Exception {
        mockMvc.perform(get("/test/excluded-type"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("raw"))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void shouldNotWrapExcludedUrl() throws Exception {
        mockMvc.perform(get("/test/excluded-url"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.value").value("url-raw"))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void shouldNotWrapResponseEntity() throws Exception {
        mockMvc.perform(get("/test/entity"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.value").value("entity"))
            .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    void shouldNotWrapFileResponse() throws Exception {
        mockMvc.perform(get("/test/file"))
            .andExpect(status().isOk())
            .andExpect(content().bytes("raw-bytes".getBytes()));
    }

    @Test
    void shouldHandleUnmatchedException() throws Exception {
        mockMvc.perform(get("/test/exception"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("1"))
            .andExpect(jsonPath("$.msg").value("boom"))
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldHandleValidationException() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("1"))
            .andExpect(jsonPath("$.msg").value("name must not be blank"));
    }

    @Test
    void shouldHandleConstraintViolationException() throws Exception {
        mockMvc.perform(get("/test/constraint").param("name", ""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("1"))
            .andExpect(jsonPath("$.msg").value("param must not be blank"));
    }

    @Test
    void shouldHandleExceptionAlias() throws Exception {
        mockMvc.perform(get("/test/alias"))
            .andExpect(status().is(418))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.msg").value("别名异常"));
    }

    @Test
    void shouldHandleExceptionAliasSubclass() throws Exception {
        mockMvc.perform(get("/test/alias-subclass"))
            .andExpect(status().is(418))
            .andExpect(jsonPath("$.code").value("4001"))
            .andExpect(jsonPath("$.msg").value("别名异常"));
    }

    @Test
    void shouldPreferBusinessExceptionHandler() throws Exception {
        mockMvc.perform(get("/test/custom"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("custom-code"))
            .andExpect(jsonPath("$.msg").value("custom-boom"));
    }
}
