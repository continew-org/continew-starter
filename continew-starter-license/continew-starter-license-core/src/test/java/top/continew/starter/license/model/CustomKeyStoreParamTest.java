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

package top.continew.starter.license.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@link CustomKeyStoreParam} 密钥参数行为测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CustomKeyStoreParamTest {

    @TempDir
    private Path tempDir;

    @Test
    @DisplayName("五参构造保留 alias、storePass、keyPass")
    void shouldKeepAliasAndPasswordsFromFiveArgConstructor() {
        CustomKeyStoreParam keyStoreParam = new CustomKeyStoreParam(CustomKeyStoreParamTest.class,
            "keystore.jks", "public-alias", "store-pass-123", "key-pass-123");
        assertThat(keyStoreParam.getAlias()).isEqualTo("public-alias");
        assertThat(keyStoreParam.getStorePwd()).isEqualTo("store-pass-123");
        assertThat(keyStoreParam.getKeyPwd()).isEqualTo("key-pass-123");
    }

    @Test
    @DisplayName("getStream 读取 storePath 指向的磁盘文件内容")
    void shouldReadStreamFromStorePath() throws IOException {
        Path keyStoreFile = tempDir.resolve("privateKeys.store");
        Files.write(keyStoreFile, "fake-keystore-content".getBytes(StandardCharsets.UTF_8));
        CustomKeyStoreParam keyStoreParam = new CustomKeyStoreParam(CustomKeyStoreParamTest.class,
            keyStoreFile.toString(), "private-alias", "store-pass-123", "key-pass-123");

        try (InputStream inputStream = keyStoreParam.getStream()) {
            assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
                .isEqualTo("fake-keystore-content");
        }
    }

    @Test
    @DisplayName("storePath 不存在时 getStream 抛出 FileNotFoundException")
    void shouldThrowFileNotFoundWhenStorePathMissing() {
        Path missingFile = tempDir.resolve("missing.store");
        CustomKeyStoreParam keyStoreParam = new CustomKeyStoreParam(CustomKeyStoreParamTest.class,
            missingFile.toString(), "private-alias", "store-pass-123", "key-pass-123");
        assertThatThrownBy(keyStoreParam::getStream).isInstanceOf(FileNotFoundException.class);
    }

    @Test
    @DisplayName("两参构造不初始化 alias 与密码字段，均返回 null")
    void shouldReturnNullAliasAndPasswordsFromTwoArgConstructor() {
        // 锁定当前行为：两参构造只委托父类，本类字段保持 null
        CustomKeyStoreParam keyStoreParam = new CustomKeyStoreParam(CustomKeyStoreParamTest.class,
            "keystore.jks");
        assertThat(keyStoreParam.getAlias()).isNull();
        assertThat(keyStoreParam.getStorePwd()).isNull();
        assertThat(keyStoreParam.getKeyPwd()).isNull();
    }

    @Test
    @DisplayName("两参构造的 storePath 为 null，getStream 抛出 NullPointerException")
    void shouldThrowNpeWhenStreamRequestedOnTwoArgConstructor() {
        // 锁定当前行为：两参构造未赋值 storePath，getStream 直接对 null 路径创建文件流
        CustomKeyStoreParam keyStoreParam = new CustomKeyStoreParam(CustomKeyStoreParamTest.class,
            "keystore.jks");
        assertThatThrownBy(keyStoreParam::getStream).isInstanceOf(NullPointerException.class);
    }
}
