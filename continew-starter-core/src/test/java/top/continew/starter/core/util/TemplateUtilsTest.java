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

package top.continew.starter.core.util;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import cn.hutool.extra.template.TemplateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 模板工具类测试
 *
 * <p>
 * 本模块 classpath 未引入任何模板引擎实现（beetl/enjoy/velocity/freemarker 均不在测试依赖内），
 * 引擎自动发现必然失败，此处锁定该边界行为，防止依赖变化后无感改变路由。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TemplateUtilsTest {

    @Test
    @DisplayName("render 在无模板引擎实现时抛出 TemplateException（锁定当前行为）")
    void shouldThrowWhenNoTemplateEngineOnClasspath() {
        Map<String, Object> params = Map.of("name", "continew");
        assertThatThrownBy(() -> TemplateUtils.render("demo-template.txt", params))
            .isInstanceOf(TemplateException.class)
            .hasMessageContaining("No template found");
    }

    @Test
    @DisplayName("render 三参重载在无模板引擎实现时同样抛出 TemplateException")
    void shouldThrowForParentPathOverloadWhenNoEngine() {
        Map<String, Object> params = Map.of("name", "continew");
        assertThatThrownBy(() -> TemplateUtils.render("templates", "demo-template.txt", params))
            .isInstanceOf(TemplateException.class);
    }
}
