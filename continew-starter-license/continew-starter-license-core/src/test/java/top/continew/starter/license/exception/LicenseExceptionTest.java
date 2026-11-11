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

package top.continew.starter.license.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BaseException;

/**
 * {@link LicenseException} 异常构造行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class LicenseExceptionTest {

    @Test
    @DisplayName("无参构造的 message 与 cause 均为空")
    void shouldCreateWithoutMessageAndCause() {
        LicenseException exception = new LicenseException();
        assertThat(exception).isInstanceOf(BaseException.class)
            .isInstanceOf(RuntimeException.class)
            .hasMessage(null)
            .hasNoCause();
    }

    @Test
    @DisplayName("message 构造保留异常信息且无 cause")
    void shouldCreateWithMessage() {
        LicenseException exception = new LicenseException("证书校验失败");
        assertThat(exception).hasMessage("证书校验失败").hasNoCause();
    }

    @Test
    @DisplayName("cause 构造保留根因且 message 取根因的 toString")
    void shouldCreateWithCause() {
        IllegalStateException cause = new IllegalStateException("根因");
        LicenseException exception = new LicenseException(cause);
        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getMessage()).isEqualTo(cause.toString());
    }

    @Test
    @DisplayName("message + cause 构造同时保留异常信息与根因")
    void shouldCreateWithMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("根因");
        LicenseException exception = new LicenseException("证书加载失败", cause);
        assertThat(exception).hasMessage("证书加载失败");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
