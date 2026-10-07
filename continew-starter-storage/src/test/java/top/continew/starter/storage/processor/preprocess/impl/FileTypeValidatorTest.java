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

package top.continew.starter.storage.processor.preprocess.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.context.UploadContext;

/**
 * 上传文件类型校验器测试。
 *
 * <p>对应 GHSA-j7qm-qprw-c53w：攻击者可上传 {@code .jsp}、{@code .html}、{@code .sh}
 * 等非常规扩展名的文件。Starter 侧提供本校验器作为扩展名白名单的落地位置，本测试
 * 固化两条关键行为：配置白名单时非常规扩展名必须被拒绝；未配置白名单时校验器不作为
 * （fail-open），此时防护需由业务侧自行保证——这正是该 advisory 中业务侧漏配白名单
 * 得以成立的原因。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
class FileTypeValidatorTest {

    @Test
    void shouldRejectDisallowedExtensionWhenWhitelistConfigured() {
        FileTypeValidator validator = new FileTypeValidator("jpg", "png", "pdf");
        for (String filename : new String[] {"shell.jsp", "page.html", "run.sh", "payload.exe"}) {
            UploadContext context = this.contextOf(filename);
            assertThrows(StorageException.class, () -> validator.validate(context),
                "未在白名单内的扩展名应被拒绝: " + filename);
        }
    }

    @Test
    void shouldAcceptAllowedExtensionWhenWhitelistConfigured() {
        FileTypeValidator validator = new FileTypeValidator("jpg", "png", "pdf");
        for (String filename : new String[] {"photo.jpg", "photo.png", "doc.pdf"}) {
            assertDoesNotThrow(() -> validator.validate(this.contextOf(filename)));
        }
    }

    @Test
    void shouldMatchExtensionCaseInsensitively() {
        FileTypeValidator validator = new FileTypeValidator("jpg", "png");
        // 文件名侧统一转小写后比对，因此大写扩展名的可执行文件仍会被拒绝
        UploadContext jspContext = this.contextOf("shell.JSP");
        assertThrows(StorageException.class, () -> validator.validate(jspContext));
        UploadContext shContext = this.contextOf("run.Sh");
        assertThrows(StorageException.class, () -> validator.validate(shContext));
        // 白名单内的扩展名不区分大小写，应放行
        assertDoesNotThrow(() -> validator.validate(this.contextOf("photo.JPG")));
        assertDoesNotThrow(() -> validator.validate(this.contextOf("photo.PNG")));
    }

    @Test
    void shouldSkipValidationWhenWhitelistAbsent() {
        FileTypeValidator validator = new FileTypeValidator();
        assertDoesNotThrow(() -> validator.validate(this.contextOf("shell.jsp")));
    }

    @Test
    void shouldRejectWhenFileNameBlank() {
        FileTypeValidator validator = new FileTypeValidator("jpg");
        UploadContext context = new UploadContext();
        context.setFile(new MockMultipartFile("file", new byte[0]));
        assertThrows(StorageException.class, () -> validator.validate(context));
    }

    /**
     * 构造仅含文件名的上传上下文。
     *
     * @param filename 文件名
     * @return 上传上下文
     */
    private UploadContext contextOf(String filename) {
        MultipartFile file = new MockMultipartFile("file", filename, null, new byte[] {1});
        UploadContext context = new UploadContext();
        context.setFile(file);
        return context;
    }
}
