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

package top.continew.starter.storage.it;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.S3Exception;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.strategy.impl.OssStorageStrategy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * OssStorageStrategy 对象级操作真实 MinIO 集成测试。
 *
 * <p>上传/下载/删除等语义由真实 S3 服务端裁决，mock 无法证明；所有断言均通过
 * 独立探针客户端核对真实对象状态。桶不存在路径由 MinIO 真实错误码驱动。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("OssStorageStrategy 对象操作真实 MinIO 集成测试")
class OssObjectOperationIT extends MinioContainerSupport {

    private static final String BUCKET = "itest-oss-objects";
    private static final String MISSING_BUCKET = "itest-oss-no-such-bucket";

    @BeforeAll
    static void setUpBucket() {
        createBucket(BUCKET);
    }

    @Test
    @DisplayName("上传后对象以预期键与元数据落库，下载内容与原始字节一致")
    void shouldUploadAndDownloadRoundtrip() throws IOException {
        String key = randomPrefix() + "roundtrip.txt";
        byte[] content = "continew-starter oss roundtrip".getBytes(StandardCharsets.UTF_8);
        strategy().upload(BUCKET, key, of("roundtrip.txt", "text/plain", content));

        // 探针直连服务端核对：对象存在、内容类型与长度与上传声明一致
        var head = headObject(BUCKET, key);
        assertEquals("text/plain", head.contentType());
        assertEquals(content.length, head.contentLength().longValue());

        try (InputStream in = strategy().download(BUCKET, key)) {
            assertArrayEquals(content, in.readAllBytes());
        }
        // 探针独立读取服务端字节，排除策略下载链路自证
        assertArrayEquals(content, probeObjectAsBytes(BUCKET, key));
    }

    @Test
    @DisplayName("键规范化：反斜杠与连续斜杠统一为单斜杠，前导斜杠移除")
    void shouldNormalizeObjectKeyOnUpload() {
        String prefix = randomPrefix();
        byte[] content = "normalized".getBytes(StandardCharsets.UTF_8);
        strategy().upload(BUCKET, "//" + prefix + "nested//sub\\a.txt", of("a.txt",
            "text/plain", content));

        // 服务端真实键名为规范化结果，原始路径不落库
        var head = headObject(BUCKET, prefix + "nested/sub/a.txt");
        assertEquals(content.length, head.contentLength().longValue());
    }

    @Test
    @DisplayName("getFileInfo 返回与真实 HeadObject 一致的元数据，缺失对象返回 null")
    void shouldExposeRealHeadMetadataInFileInfo() {
        String key = randomPrefix() + "info.txt";
        byte[] content = "file info".getBytes(StandardCharsets.UTF_8);
        strategy().upload(BUCKET, key, of("info.txt", "text/plain", content));

        var head = headObject(BUCKET, key);
        FileInfo info = strategy().getFileInfo(BUCKET, key);
        assertEquals(BUCKET, info.getBucket());
        assertEquals("oss-it", info.getPlatform());
        assertEquals(key, info.getPath());
        assertEquals("info.txt", info.getName());
        assertEquals(head.contentLength().longValue(), info.getSize().longValue());
        assertEquals(head.eTag(), info.getMetadata().get("etag"));

        assertNull(strategy().getFileInfo(BUCKET, randomPrefix() + "missing.txt"));
    }

    @Test
    @DisplayName("exists 依据真实 HeadObject 判定对象存在与缺失")
    void shouldCheckExistenceAgainstRealHeadObject() {
        String key = randomPrefix() + "exists.txt";
        strategy().upload(BUCKET, key, of("exists.txt", "text/plain", "yes"
            .getBytes(StandardCharsets.UTF_8)));
        assertTrue(strategy().exists(BUCKET, key));
        assertFalse(strategy().exists(BUCKET, randomPrefix() + "missing.txt"));
    }

    @Test
    @DisplayName("list 按前缀返回真实对象并受 maxKeys 限制")
    void shouldListObjectsByPrefixWithMaxKeys() {
        String prefix = randomPrefix();
        for (int i = 0; i < 3; i++) {
            String path = prefix + "item-" + i + ".txt";
            strategy().upload(BUCKET, path, of("item.txt", "text/plain", String
                .valueOf(i).getBytes(StandardCharsets.UTF_8)));
        }

        List<FileInfo> all = strategy().list(BUCKET, prefix, 10);
        assertEquals(3, all.size());
        assertTrue(all.stream().allMatch(info -> info.getPath().startsWith(prefix)));

        List<FileInfo> limited = strategy().list(BUCKET, prefix, 2);
        assertEquals(2, limited.size());
    }

    @Test
    @DisplayName("copy 保留源对象，move 复制后删除源对象")
    void shouldCopyAndMoveObjectsInRealBucket() {
        String prefix = randomPrefix();
        String source = prefix + "source.txt";
        byte[] content = "copy-move".getBytes(StandardCharsets.UTF_8);
        strategy().upload(BUCKET, source, of("source.txt", "text/plain", content));

        String copied = prefix + "copied.txt";
        strategy().copy(BUCKET, BUCKET, source, copied);
        assertArrayEquals(content, probeObjectAsBytes(BUCKET, copied));
        // copy 语义保留源对象
        assertTrue(strategy().exists(BUCKET, source));

        String moved = prefix + "moved.txt";
        strategy().move(BUCKET, BUCKET, copied, moved);
        assertArrayEquals(content, probeObjectAsBytes(BUCKET, moved));
        assertFalse(strategy().exists(BUCKET, copied));
    }

    @Test
    @DisplayName("delete 与 batchDelete 从服务端移除真实对象")
    void shouldDeleteAndBatchDeleteRealObjects() {
        String prefix = randomPrefix();
        String single = prefix + "single.txt";
        strategy().upload(BUCKET, single, of("single.txt", "text/plain", "1"
            .getBytes(StandardCharsets.UTF_8)));
        strategy().delete(BUCKET, single);
        assertFalse(strategy().exists(BUCKET, single));

        List<String> paths = List.of(prefix + "batch-a.txt", prefix + "batch-b.txt", prefix
            + "batch-c.txt");
        for (String path : paths) {
            strategy().upload(BUCKET, path, of("batch.txt", "text/plain", "b"
                .getBytes(StandardCharsets.UTF_8)));
        }
        strategy().batchDelete(BUCKET, paths);
        long remaining = probe()
            .listObjectsV2(ListObjectsV2Request.builder().bucket(BUCKET).prefix(prefix).build())
            .contents()
            .size();
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("桶不存在：写读列操作包装为 StorageException，exists/getFileInfo 按真实 404 返回缺失语义")
    void shouldHandleMissingBucketWithRealServerErrors() {
        String key = randomPrefix() + "any.txt";
        OssStorageStrategy oss = strategy();
        MockMultipartFile file = of("any.txt", "text/plain", "x".getBytes(StandardCharsets.UTF_8));

        StorageException uploadError = assertThrows(StorageException.class, () -> oss.upload(
            MISSING_BUCKET, key, file));
        assertEquals(404, ((S3Exception) uploadError.getCause()).statusCode());

        StorageException downloadError = assertThrows(StorageException.class, () -> oss.download(
            MISSING_BUCKET, key));
        assertEquals(404, ((S3Exception) downloadError.getCause()).statusCode());

        StorageException listError = assertThrows(StorageException.class, () -> oss.list(
            MISSING_BUCKET, key, 10));
        assertEquals(404, ((S3Exception) listError.getCause()).statusCode());

        // MinIO 对缺失桶的 HeadObject 返回 404，策略按对象缺失语义收敛为 false/null
        assertFalse(strategy().exists(MISSING_BUCKET, key));
        assertNull(strategy().getFileInfo(MISSING_BUCKET, key));
    }

    /**
     * 构造测试上传文件（复用 spring-test 的 MockMultipartFile）。
     *
     * @param filename    文件名
     * @param contentType 内容类型
     * @param content     文件内容
     * @return 测试上传文件
     */
    private static MockMultipartFile of(String filename, String contentType, byte[] content) {
        return new MockMultipartFile("file", filename, contentType, content);
    }
}
