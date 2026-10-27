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

package top.continew.starter.extension.crud.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.continew.starter.extension.crud.test.CrudTestApplication;

/**
 * CRUD 请求映射自动配置测试
 * <p>
 * 回归验证：CRUD 扩展通过 WebMvcRegistrations 替换 RequestMappingHandlerMapping，不得顶掉 Boot 的 WebMvcAutoConfiguration
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest(classes = CrudTestApplication.class)
@AutoConfigureMockMvc
class CrudRequestMappingAutoConfigurationTest {

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUseCrudRequestMappingHandlerMapping() {
        assertThat(requestMappingHandlerMapping)
            .isInstanceOf(CrudRequestMappingHandlerMapping.class);
    }

    @Test
    void shouldApplyCrudRequestMappingPrefix() throws Exception {
        mockMvc.perform(get("/test-crud/hello"))
            .andExpect(status().isOk())
            .andExpect(content().string("hello"));
    }
}
