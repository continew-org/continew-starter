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

package top.continew.starter.extension.datapermission.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BaseException;

/**
 * {@link DataPermissionException} 异常测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DataPermissionExceptionTest {

    @Test
    @DisplayName("异常属于运行时异常体系")
    void shouldBeRuntimeException() {
        assertThat(BaseException.class.isAssignableFrom(DataPermissionException.class)).isTrue();
        assertThat(RuntimeException.class.isAssignableFrom(DataPermissionException.class)).isTrue();
    }

    @Test
    @DisplayName("消息构造器保留消息文本且原因为空")
    void shouldKeepMessageFromConstructor() {
        DataPermissionException exception = new DataPermissionException("demo");
        assertThat(exception.getMessage()).isEqualTo("demo");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("消息与原因构造器同时保留两者")
    void shouldKeepMessageAndCauseFromConstructor() {
        IllegalStateException cause = new IllegalStateException("root");
        DataPermissionException exception = new DataPermissionException("demo", cause);
        assertThat(exception.getMessage()).isEqualTo("demo");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("unsupportedDataScope 拼接数据范围说明")
    void shouldBuildUnsupportedDataScopeMessage() {
        assertThat(DataPermissionException.unsupportedDataScope("UNKNOWN").getMessage())
            .isEqualTo("Unsupported data scope: UNKNOWN");
    }

    @Test
    @DisplayName("unsupportedDatabase 拼接数据库类型说明")
    void shouldBuildUnsupportedDatabaseMessage() {
        assertThat(DataPermissionException.unsupportedDatabase("oracle").getMessage())
            .isEqualTo("Unsupported database for data permission: oracle");
    }

    @Test
    @DisplayName("invalidUserData 拼接用户数据缺陷说明")
    void shouldBuildInvalidUserDataMessage() {
        assertThat(DataPermissionException.invalidUserData("roles is empty").getMessage())
            .isEqualTo("Invalid user data: roles is empty");
    }

    @Test
    @DisplayName("methodNotFound 拼接语句定位说明")
    void shouldBuildMethodNotFoundMessage() {
        assertThat(
            DataPermissionException.methodNotFound("com.demo.mapper.selectList").getMessage())
            .isEqualTo("Method not found for data permission: com.demo.mapper.selectList");
    }
}
