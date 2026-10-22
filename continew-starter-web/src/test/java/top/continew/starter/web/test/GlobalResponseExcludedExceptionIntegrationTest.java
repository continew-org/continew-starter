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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import top.continew.starter.web.test.config.GlobalResponseTestConfiguration;

import jakarta.servlet.ServletException;

/**
 * 全局响应例外异常集成测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest(
    properties = {"continew-starter.web.response.print-exception-in-global-advice=false",
        "continew-starter.web.response.exclude-exception-types=java.lang.IllegalStateException"})
@AutoConfigureMockMvc
@Import(GlobalResponseTestConfiguration.class)
class GlobalResponseExcludedExceptionIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReleaseExcludedException() {
        ServletException exception = assertThrows(ServletException.class, () -> mockMvc
            .perform(get("/test/exception")));
        assertEquals(IllegalStateException.class, exception.getCause().getClass());
    }
}
