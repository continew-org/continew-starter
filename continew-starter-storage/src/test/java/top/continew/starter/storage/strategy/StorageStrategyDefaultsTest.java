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

package top.continew.starter.storage.strategy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * {@link StorageStrategy} 可选默认方法语义单元测试。
 *
 * <p>流式上传与范围下载的默认实现拒绝服务、支持标志默认为 false、cleanup
 * 默认为空操作——这些是策略实现可省略方法时的统一契约。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("StorageStrategy 默认方法语义单元测试")
class StorageStrategyDefaultsTest {

    private static final StorageStrategy STRATEGY = new StorageStrategy() {

        @Override
        public void upload(String bucket, String path,
            org.springframework.web.multipart.MultipartFile file) {
        }

        @Override
        public InputStream download(String bucket, String path) {
            return null;
        }

        @Override
        public void delete(String bucket, String path) {
        }

        @Override
        public void batchDelete(String bucket, List<String> paths) {
        }

        @Override
        public boolean exists(String bucket, String path) {
            return false;
        }

        @Override
        public FileInfo getFileInfo(String bucket, String path) {
            return null;
        }

        @Override
        public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
            return List.of();
        }

        @Override
        public void copy(String sourceBucket, String targetBucket, String sourcePath,
            String targetPath) {
        }

        @Override
        public void move(String sourceBucket, String targetBucket, String sourcePath,
            String targetPath) {
        }

        @Override
        public String getPlatform() {
            return "t";
        }

        @Override
        public String defaultBucket() {
            return null;
        }

        @Override
        public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
            return null;
        }

        @Override
        public String generateUploadPresignedUrl(String bucket, String path, long expireSeconds) {
            return null;
        }

        @Override
        public MultipartInitResp initMultipartUpload(String bucket, String path,
            String contentType, Map<String, String> metadata) {
            return null;
        }

        @Override
        public MultipartUploadResp uploadPart(String bucket, String path, String uploadId,
            int partNumber, InputStream data) {
            return null;
        }

        @Override
        public FileInfo completeMultipartUpload(String bucket, String path, String uploadId,
            List<MultipartUploadResp> parts, boolean verifyParts) {
            return null;
        }

        @Override
        public void abortMultipartUpload(String bucket, String path, String uploadId) {
        }

        @Override
        public List<MultipartUploadResp> listParts(String bucket, String path, String uploadId) {
            return List.of();
        }
    };

    @Test
    @DisplayName("默认流式上传与范围下载拒绝服务")
    void shouldRejectUnsupportedOptionalOperations() {
        assertThrows(StorageException.class,
            () -> STRATEGY.uploadStream("b", "p", InputStream.nullInputStream(), "t", 0));
        assertThrows(StorageException.class,
            () -> STRATEGY.downloadRange("b", "p", 0, 1));
    }

    @Test
    @DisplayName("默认不支持流式上传，cleanup 为空操作")
    void shouldDefaultUnsupportedFlags() {
        assertFalse(STRATEGY.supportUploadStream());
        STRATEGY.cleanup();
    }
}
