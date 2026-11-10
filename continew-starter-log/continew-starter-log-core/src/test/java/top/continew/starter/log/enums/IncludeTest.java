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

package top.continew.starter.log.enums;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link Include} 枚举测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class IncludeTest {

    /**
     * 默认包含信息应为请求头、响应头、请求参数与响应参数，且保持声明顺序
     */
    @Test
    @DisplayName("默认包含信息为四类默认项且有序")
    void defaultIncludesShouldContainFourDefaultEntriesInDeclaredOrder() {
        assertThat(Include.defaultIncludes()).containsExactly(Include.REQUEST_HEADERS,
            Include.RESPONSE_HEADERS, Include.REQUEST_PARAM, Include.RESPONSE_PARAM);
    }

    /**
     * 锁定当前行为：默认包含信息为不可修改集合，外部追加应抛 UnsupportedOperationException
     */
    @Test
    @DisplayName("默认包含信息不可修改（锁定当前行为）")
    void defaultIncludesShouldBeUnmodifiable() {
        assertThatThrownBy(() -> Include.defaultIncludes().add(Include.REQUEST_BODY))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    /**
     * 默认包含信息为静态常量，多次调用返回同一实例
     */
    @Test
    @DisplayName("默认包含信息复用同一静态实例")
    void defaultIncludesShouldReuseSameStaticInstance() {
        assertThat(Include.defaultIncludes()).isSameAs(Include.defaultIncludes());
    }

    /**
     * 枚举应覆盖描述、模块、IP 归属地、浏览器、操作系统与请求/响应体等全部 11 项
     */
    @Test
    @DisplayName("枚举共 11 个常量且含全部业务项")
    void enumShouldDeclareAllElevenConstants() {
        assertThat(Include.values()).hasSize(11);
        assertThat(Include.values()).extracting(Include::name)
            .contains("DESCRIPTION", "MODULE", "REQUEST_HEADERS", "REQUEST_BODY", "REQUEST_PARAM",
                "IP_ADDRESS", "BROWSER", "OS", "RESPONSE_HEADERS", "RESPONSE_BODY",
                "RESPONSE_PARAM");
    }
}
