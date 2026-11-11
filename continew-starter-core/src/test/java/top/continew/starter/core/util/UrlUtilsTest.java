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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * URL 工具类测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class UrlUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"http://continew.top", "https://continew.top/docs",
        "HTTPS://CONTINEW.TOP"})
    @DisplayName("http 与 https 前缀（忽略大小写）识别为 HTTP URL")
    void shouldRecognizeHttpAndHttpsUrl(String url) {
        assertThat(UrlUtils.isHttpUrl(url)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ftp://files.continew.top", "ws://socket.continew.top",
        "www.continew.top", "/relative/path"})
    @DisplayName("非 http/https 协议与相对路径不识别为 HTTP URL")
    void shouldRejectNonHttpUrl(String url) {
        assertThat(UrlUtils.isHttpUrl(url)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("null 与空串识别为非 HTTP URL（锁定当前行为：底层前缀判断对空输入返回 false）")
    void shouldRejectNullAndEmptyUrl(String url) {
        assertThat(UrlUtils.isHttpUrl(url)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "http://a.com,true",
        "httpx://a.com,false"})
    @DisplayName("协议判断以协议名加冒号为完整前缀边界")
    void shouldJudgeHttpPrefixWithColonBoundary(String url, boolean expected) {
        assertThat(UrlUtils.isHttpUrl(url)).isEqualTo(expected);
    }
}
