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

package top.continew.starter.storage.strategy.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import top.continew.starter.storage.autoconfigure.properties.LocalStorageConfig;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * LocalStorageStrategy 本地文件系统行为单元测试。
 *
 * <p>本地存储语义（路径规范化、目录清理、分片合并）由本地文件系统即可裁决，
 * 不上容器；S3 协议行为由 MinIO 集成测试覆盖，两者分工不重叠。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("LocalStorageStrategy 本地存储行为单元测试")
class LocalStorageStrategyTest {

    private static final String ENDPOINT = "http://localhost:5880/local";
    private static final String HELLO_MD5 = "5d41402abc4b2a76b9719d911017c592";

    @TempDir
    private Path bucketDir;

    @TempDir
    private Path multipartTempDir;

    private LocalStorageStrategy strategy;
    private LocalStorageConfig config;

    @BeforeEach
    void setUp() {
        config = new LocalStorageConfig();
        config.setPlatform("local-it");
        config.setBucketName(bucketDir.toString());
        config.setEndpoint(ENDPOINT);
        config.setMultipartTempDir(multipartTempDir.toString());
        strategy = new LocalStorageStrategy(config);
    }

    @Test
    @DisplayName("上传后文件真实落盘，下载与存在性判定一致")
    void shouldUploadAndDownloadRoundtrip() throws IOException {
        String path = "docs/hello.txt";
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        strategy.upload(bucketDir.toString(), path, file("hello.txt", content));

        assertTrue(strategy.exists(bucketDir.toString(), path));
        assertTrue(Files.exists(bucketDir.resolve("docs/hello.txt")), "文件应按相对路径真实落盘");
        try (InputStream in = strategy.download(bucketDir.toString(), path)) {
            assertArrayEquals(content, in.readAllBytes());
        }
    }

    @Test
    @DisplayName("getFileInfo 返回真实文件大小、MD5 ETag 与访问 URL，缺失文件返回 null")
    void shouldExposeRealFileInfoWithMd5Etag() {
        String path = "info.txt";
        byte[] content = "hello".getBytes(StandardCharsets.UTF_8);
        strategy.upload(bucketDir.toString(), path, file("info.txt", content));

        FileInfo info = strategy.getFileInfo(bucketDir.toString(), path);
        assertEquals("local-it", info.getPlatform());
        assertEquals("info.txt", info.getName());
        assertEquals("info.txt", info.getPath());
        assertEquals(content.length, info.getSize().longValue());
        assertEquals(HELLO_MD5, info.getMetadata().get("etag"));
        assertEquals(ENDPOINT + "/" + path, info.getUrl());
        assertNotNull(info.getUploadTime());

        assertNull(strategy.getFileInfo(bucketDir.toString(), "missing.txt"));
    }

    @Test
    @DisplayName("list 按前缀过滤真实文件并受 maxKeys 限制")
    void shouldListWithPrefixAndMaxKeys() {
        String bucket = bucketDir.toString();
        strategy.upload(bucket, "media/a.txt", file("a.txt", "1".getBytes(StandardCharsets.UTF_8)));
        strategy.upload(bucket, "media/b.txt", file("b.txt", "2".getBytes(StandardCharsets.UTF_8)));
        strategy.upload(bucket, "other/c.txt", file("c.txt", "3".getBytes(StandardCharsets.UTF_8)));

        List<FileInfo> media = strategy.list(bucket, "media", 10);
        assertEquals(2, media.size());
        assertTrue(media.stream().allMatch(info -> info.getPath().startsWith("media")));

        assertEquals(1, strategy.list(bucket, "media", 1).size());
    }

    @Test
    @DisplayName("copy 保留源文件，move 后源文件删除且内容保留")
    void shouldCopyAndMoveBetweenPaths() throws IOException {
        String bucket = bucketDir.toString();
        byte[] content = "copy-move".getBytes(StandardCharsets.UTF_8);
        strategy.upload(bucket, "src/original.txt", file("original.txt", content));

        strategy.copy(bucket, bucket, "src/original.txt", "dst/copied.txt");
        assertArrayEquals(content, readAll("dst/copied.txt"));
        assertTrue(strategy.exists(bucket, "src/original.txt"));

        strategy.move(bucket, bucket, "dst/copied.txt", "dst/moved.txt");
        assertArrayEquals(content, readAll("dst/moved.txt"));
        assertFalse(strategy.exists(bucket, "dst/copied.txt"));
    }

    @Test
    @DisplayName("delete 移除文件并自底向上清理空父目录")
    void shouldDeleteFileAndEmptyParentDirectories() {
        String bucket = bucketDir.toString();
        strategy.upload(bucket, "deep/nested/a/b.txt", file("b.txt", "x"
            .getBytes(StandardCharsets.UTF_8)));

        strategy.delete(bucket, "deep/nested/a/b.txt");

        assertFalse(strategy.exists(bucket, "deep/nested/a/b.txt"));
        assertFalse(Files.exists(bucketDir.resolve("deep/nested/a")), "空父目录应被级联清理");
    }

    @Test
    @DisplayName("batchDelete 删除全部目标；部分失败时抛出异常并指明失败路径")
    void shouldBatchDeleteAndReportFailedPaths() throws IOException {
        String bucket = bucketDir.toString();
        strategy.upload(bucket, "batch/one.txt", file("one.txt", "1"
            .getBytes(StandardCharsets.UTF_8)));
        strategy.upload(bucket, "batch/two.txt", file("two.txt", "2"
            .getBytes(StandardCharsets.UTF_8)));
        // 非空目录触发 DirectoryNotEmptyException，构造部分失败场景
        Path occupiedDir = bucketDir.resolve("batch/occupied");
        Files.createDirectories(occupiedDir);
        Files.writeString(occupiedDir.resolve("keep.txt"), "keep");

        strategy.batchDelete(bucket, List.of("batch/one.txt", "batch/two.txt"));
        assertFalse(strategy.exists(bucket, "batch/one.txt"));
        assertFalse(strategy.exists(bucket, "batch/two.txt"));

        List<String> occupied = List.of("batch/occupied");
        StorageException error = assertThrows(StorageException.class, () -> strategy
            .batchDelete(bucket, occupied));
        assertTrue(error.getMessage().contains("batch/occupied"), "异常应指明失败路径");
    }

    @Test
    @DisplayName("路径穿越（../）在写入前被拒绝且不落盘")
    void shouldRejectPathTraversalOnUpload() {
        String bucket = bucketDir.toString();
        MockMultipartFile malicious = file("escape.txt", "evil".getBytes(StandardCharsets.UTF_8));

        assertThrows(StorageException.class, () -> strategy.upload(bucket, "../escape.txt",
            malicious));
        assertThrows(StorageException.class, () -> strategy.upload(bucket,
            "docs/../../escape.txt", malicious));
        assertFalse(Files.exists(bucketDir.resolve("escape.txt")), "桶内不应出现穿越文件");
        assertFalse(Files.exists(bucketDir.getParent().resolve("escape.txt")), "桶外不应出现穿越文件");
    }

    @Test
    @DisplayName("本地存储不支持预签名：两种方法均返回普通访问 URL")
    void shouldReturnPlainUrlForPresignedMethods() {
        String bucket = bucketDir.toString();
        assertEquals(ENDPOINT + "/docs/plain.txt", strategy.generatePresignedUrl(bucket,
            "docs/plain.txt", 300));
        assertEquals(ENDPOINT + "/docs/upload.txt", strategy.generateUploadPresignedUrl(bucket,
            "docs/upload.txt", 300));
    }

    @Test
    @DisplayName("分片合并按分片编号排序执行，输入乱序不影响结果，完成后清理临时分片")
    void shouldMergeMultipartPartsInOrderAndCleanup() throws IOException {
        String bucket = bucketDir.toString();
        byte[] part1 = "AAAA".getBytes(StandardCharsets.UTF_8);
        byte[] part2 = "BBBBBB".getBytes(StandardCharsets.UTF_8);

        MultipartInitResp init = strategy.initMultipartUpload(bucket, "merged.bin",
            "application/octet-stream", null);
        MultipartUploadResp resp1 = strategy.uploadPart(bucket, "merged.bin", init.getUploadId(),
            1, new ByteArrayInputStream(part1));
        MultipartUploadResp resp2 = strategy.uploadPart(bucket, "merged.bin", init.getUploadId(),
            2, new ByteArrayInputStream(part2));
        assertTrue(resp1.isSuccess());
        assertTrue(resp2.isSuccess());
        assertEquals(md5Hex(part1), resp1.getPartETag());

        // 刻意以乱序传入已完成分片，合并结果必须仍按分片编号拼接
        strategy.completeMultipartUpload(bucket, "merged.bin", init.getUploadId(), List
            .of(resp2, resp1), false);

        assertArrayEquals(new byte[] {'A', 'A', 'A', 'A', 'B', 'B', 'B', 'B', 'B', 'B'}, readAll(
            "merged.bin"));
        assertFalse(tempTreeContains(multipartTempDir, init.getUploadId()),
            "合并完成后临时分片目录应被清理");
    }

    @Test
    @DisplayName("abort 清理该 uploadId 的全部临时分片")
    void shouldCleanTempPartsOnAbort() throws IOException {
        String bucket = bucketDir.toString();
        MultipartInitResp init = strategy.initMultipartUpload(bucket, "aborted.bin",
            "application/octet-stream", null);
        MultipartUploadResp resp = strategy.uploadPart(bucket, "aborted.bin", init
            .getUploadId(), 1, new ByteArrayInputStream("part".getBytes(StandardCharsets.UTF_8)));
        assertTrue(resp.isSuccess());
        assertTrue(tempTreeContains(multipartTempDir, init.getUploadId()), "分片应写入临时目录");

        strategy.abortMultipartUpload(bucket, "aborted.bin", init.getUploadId());

        assertFalse(tempTreeContains(multipartTempDir, init.getUploadId()),
            "取消后临时分片目录应被清理");
    }

    @Test
    @DisplayName("listParts 按本地实现语义返回空列表（分片记录由 FileRecorder 维护）")
    void shouldReturnEmptyListForListParts() {
        String bucket = bucketDir.toString();
        MultipartInitResp init = strategy.initMultipartUpload(bucket, "parts.bin",
            "application/octet-stream", null);
        assertTrue(strategy.listParts(bucket, "parts.bin", init.getUploadId()).isEmpty());
    }

    @Test
    @DisplayName("含分隔符的桶名在临时目录映射中被安全替换，不抛异常")
    void shouldSanitizeBucketSegmentForTempPath() {
        MultipartInitResp init = strategy.initMultipartUpload("a/b:c\\d", "sanitized.bin",
            "application/octet-stream", null);
        assertNotNull(init.getUploadId());
    }

    /**
     * 构造测试上传文件。
     *
     * @param filename 文件名
     * @param content  文件内容
     * @return 测试上传文件
     */
    private static MockMultipartFile file(String filename, byte[] content) {
        return new MockMultipartFile("file", filename, "application/octet-stream", content);
    }

    /**
     * 读取策略下载流中的全部字节。
     *
     * @param path 对象路径
     * @return 文件内容
     * @throws IOException 读取失败时抛出
     */
    private byte[] readAll(String path) throws IOException {
        try (InputStream in = strategy.download(bucketDir.toString(), path)) {
            return in.readAllBytes();
        }
    }

    /**
     * 计算内容的 MD5 十六进制串（与本地策略的 ETag 算法一致）。
     *
     * @param content 内容字节
     * @return 32 位小写 MD5
     */
    private static String md5Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            return HexFormat.of().formatHex(digest.digest(content)).toLowerCase();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JVM 缺失 MD5 算法", e);
        }
    }

    /**
     * 判断临时目录树中是否存在指定名称的条目（桶名含绝对路径会被整体映射，无法按固定子路径断言）。
     *
     * @param root 临时目录根
     * @param name 目标条目名
     * @return 是否存在
     * @throws IOException 遍历失败时抛出
     */
    private static boolean tempTreeContains(Path root, String name) throws IOException {
        try (var stream = Files.walk(root)) {
            return stream.anyMatch(path -> path.getFileName().toString().equals(name));
        }
    }
}
