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

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.accept.ApiVersionStrategy;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import top.continew.starter.extension.crud.test.CrudTestApplication;

/**
 * CRUD 请求映射自动配置的 API 版本策略测试
 * <p>
 * 回归验证：启用 API 版本后，Boot 的 WebMvcAutoConfiguration 仍生效，mvcApiVersionStrategy 可正常注入
 *
 * @author Charles7c
 * @since 3.0.0
 */
@SpringBootTest(classes = CrudTestApplication.class,
    properties = "spring.mvc.apiversion.use.query-parameter=true")
class CrudApiVersionStrategyAutoConfigurationTest {

    @Autowired
    @Qualifier("mvcApiVersionStrategy")
    private ApiVersionStrategy apiVersionStrategy;

    @Autowired
    private RequestMappingHandlerMapping requestMappingHandlerMapping;

    @Test
    void shouldProvideApiVersionStrategy() {
        assertThat(apiVersionStrategy).isNotNull();
    }

    @Test
    void shouldKeepCrudRequestMappingHandlerMapping() {
        assertThat(requestMappingHandlerMapping)
            .isInstanceOf(CrudRequestMappingHandlerMapping.class);
    }
}
