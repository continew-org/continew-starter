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

package top.continew.starter.storage.operation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.autoconfigure.properties.StorageProperties;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.core.FileStorageService;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.domain.model.resp.ZipImportResp;
import top.continew.starter.storage.engine.StorageStrategyRouter;
import top.continew.starter.storage.processor.registry.ProcessorRegistry;
import top.continew.starter.storage.service.FileRecorder;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * file()/files() 链式操作单元测试。
 *
 * <p>路由层由 mock {@link StorageStrategyRouter} 提供，委托参数与流归属由记录式假策略裁决。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("链式文件与 ZIP 操作单元测试")
class FileOperationTest {

    private static final String PLATFORM = "fake";
    private static final String OTHER = "other";

    private StorageStrategyRouter router;
    private FakeStrategy strategy;
    private FakeStrategy otherStrategy;
    private FileStorageService service;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        this.router = mock(StorageStrategyRouter.class);
        this.strategy = new FakeStrategy();
        this.otherStrategy = new FakeStrategy();
        when(this.router.route(PLATFORM)).thenReturn(this.strategy);
        when(this.router.route(OTHER)).thenReturn(this.otherStrategy);
        when(this.router.getDefaultStorage()).thenReturn(PLATFORM);
        this.properties = new StorageProperties();
        this.service = new FileStorageService(this.router, mock(ProcessorRegistry.class),
            mock(FileRecorder.class), this.properties);
    }

    @Test
    @DisplayName("链式构建不触发 IO，终结调用才解析默认平台")
    void shouldResolvePlatformLazilyOnTerminal() {
        FileOperation op = this.service.file("bucket", "a.txt").platform(OTHER);
        verify(this.router, never()).route(OTHER);

        op.download();

        verify(this.router).route(OTHER);
        assertEquals(1, this.otherStrategy.downloadedPaths.size());
        assertTrue(this.strategy.downloadedPaths.isEmpty());
    }

    @Test
    @DisplayName("无默认平台时显式平台仍可使用（默认平台延迟取值）")
    void shouldUseExplicitPlatformWhenDefaultMissing() throws IOException {
        when(this.router.getDefaultStorage())
            .thenThrow(new StorageException("无可用默认存储平台"));
        this.otherStrategy.downloadBytes.put("a.txt",
            "data".getBytes(StandardCharsets.UTF_8));
        this.otherStrategy.rangeBytes = "xy".getBytes(StandardCharsets.UTF_8);
        this.otherStrategy.batchZip = "zip".getBytes(StandardCharsets.UTF_8);
        this.otherStrategy.downloadBytes.put("a.zip",
            buildZip(Map.of("b.txt", "B".getBytes(StandardCharsets.UTF_8))));

        // 单文件
        try (InputStream in = this.service.file("bucket", "a.txt").platform(OTHER)
            .download()) {
            assertEquals("data", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
        // range
        this.service.file("bucket", "a.txt").platform(OTHER).range(0, 1)
            .writeTo(new ByteArrayOutputStream());
        // 批量删除与 ZIP 下载
        this.service.files("bucket", List.of("a.txt")).platform(OTHER).delete();
        this.service.files("bucket", List.of("a.txt")).platform(OTHER).zip()
            .writeTo(new ByteArrayOutputStream());
        // ZIP 导入
        ZipImportResp resp = this.service.file("bucket", "a.zip").platform(OTHER)
            .asZip().importZip();

        assertEquals(1, resp.getSuccess());
        assertEquals(List.of("a.txt"), this.otherStrategy.batchDeletedPaths);
        assertEquals(List.of("a/b.txt"), this.otherStrategy.uploadedPaths);
        verify(this.router, never()).route(PLATFORM);
    }

    @Test
    @DisplayName("未指定平台时终结调用使用默认平台")
    void shouldUseDefaultPlatformWhenNotSpecified() throws IOException {
        this.strategy.downloadBytes.put("a.txt", "data".getBytes(StandardCharsets.UTF_8));

        InputStream in = this.service.file("bucket", "a.txt").download();

        assertEquals("data", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        verify(this.router).route(PLATFORM);
        assertEquals(List.of("a.txt"), this.strategy.downloadedPaths);
    }

    @Test
    @DisplayName("单文件终结调用参数按序透传")
    void shouldDelegateSingleFileTerminalsInOrder() {
        this.service.file("bucket", "a.txt").delete();
        this.service.file("bucket", "b.txt").copyTo("bk.txt");
        this.service.file("bucket", "c.txt").copyTo("tb", "bk2.txt");
        this.service.file("bucket", "d.txt").moveTo("mk.txt");
        this.service.file("bucket", "e.txt").moveTo("tb", "mk2.txt");
        FileInfo info = this.service.file("bucket", "f.txt").info();
        boolean exists = this.service.file("bucket", "g.txt").exists();

        assertEquals(List.of("a.txt"), this.strategy.deletedPaths);
        assertEquals(List.of("bucket|bucket|b.txt|bk.txt", "bucket|tb|c.txt|bk2.txt"),
            this.strategy.copyArgs);
        assertEquals(List.of("bucket|bucket|d.txt|mk.txt", "bucket|tb|e.txt|mk2.txt"),
            this.strategy.moveArgs);
        assertEquals("f.txt", info.getPath());
        assertFalse(exists);
    }

    @Test
    @DisplayName("预签名 URL 透传平台、桶、路径与秒数")
    void shouldDelegatePresignedUrls() {
        assertEquals("sig:get:bucket:a.txt:60",
            this.service.file("bucket", "a.txt").presignedUrl(60));
        assertEquals("sig:put:bucket:a.txt:120",
            this.service.file("bucket", "a.txt").platform(OTHER).uploadPresignedUrl(120));
    }

    @Test
    @DisplayName("范围下载透传闭区间参数")
    void shouldPassRangeArguments() throws IOException {
        this.strategy.rangeBytes = "xyz".getBytes(StandardCharsets.UTF_8);

        InputStream in = this.service.file("bucket", "a.bin").range(2, 8).download();
        in.close();

        assertEquals(2, this.strategy.rangeStart);
        assertEquals(8, this.strategy.rangeEnd);
    }

    @Test
    @DisplayName("范围写出复用服务层写流契约")
    void shouldWriteRangeToOutput() {
        this.strategy.rangeBytes = "xy".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        this.service.file("bucket", "a.bin").range(0, 1).writeTo(out);

        assertEquals("xy", out.toString(StandardCharsets.UTF_8));
        assertEquals(0, this.strategy.rangeStart);
        assertEquals(1, this.strategy.rangeEnd);
    }

    @Test
    @DisplayName("批量删除在构建时快照路径列表")
    void shouldSnapshotPathsForBatchDelete() {
        List<String> paths = new ArrayList<>(List.of("a.txt", "b.txt"));
        FilesOperation ops = this.service.files("bucket", paths);
        paths.add("evil.txt");

        ops.delete();

        assertEquals(List.of("a.txt", "b.txt"), this.strategy.batchDeletedPaths);
    }

    @Test
    @DisplayName("files().zip() 下载与写出委托批量打包")
    void shouldDelegateBatchZip() {
        this.strategy.batchZip = "zip-bytes".getBytes(StandardCharsets.UTF_8);
        List<String> paths = List.of("a.txt", "b.txt");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.service.files("bucket", paths).zip().writeTo(out);

        assertEquals("zip-bytes", out.toString(StandardCharsets.UTF_8));
        assertEquals(List.of("a.txt", "b.txt"), this.strategy.batchDownloadPaths);
        assertEquals(List.of("bucket"), this.strategy.batchDownloadBuckets);
    }

    @Test
    @DisplayName("asZip 默认导入同桶同级同名目录")
    void shouldImportZipToSameBucketDefaultDir() throws IOException {
        this.strategy.downloadBytes.put("dir/a.zip",
            buildZip(Map.of("a.txt", "A".getBytes(StandardCharsets.UTF_8))));

        ZipImportResp resp = this.service.file("bucket", "dir/a.zip").asZip().importZip();

        assertEquals(1, resp.getSuccess());
        assertEquals(List.of("dir/a.zip"), this.strategy.downloadedPaths);
        assertEquals(List.of("dir/a/a.txt"), this.strategy.uploadedPaths);
        assertEquals(List.of("bucket"), this.strategy.uploadedBuckets);
    }

    @Test
    @DisplayName("asZip 支持显式目标路径与跨桶目标")
    void shouldImportZipToExplicitTargets() throws IOException {
        this.strategy.downloadBytes.put("a.zip",
            buildZip(Map.of("b.txt", "B".getBytes(StandardCharsets.UTF_8))));

        this.service.file("bucket", "a.zip").asZip().importTo("target/").importZip();

        assertEquals(List.of("target/b.txt"), this.strategy.uploadedPaths);

        this.otherStrategy.downloadBytes.put("a.zip",
            buildZip(Map.of("c.txt", "C".getBytes(StandardCharsets.UTF_8))));
        this.service.file("bucket", "a.zip").platform(OTHER).asZip().importToBucket("tb")
            .importZip();

        assertEquals(List.of("a/c.txt"), this.otherStrategy.uploadedPaths);
        assertEquals(List.of("tb"), this.otherStrategy.uploadedBuckets);
    }

    @Test
    @DisplayName("asZip 校验 .zip 后缀大小写不敏感与文件名非空")
    void shouldValidateZipSourcePath() {
        FileOperation txtOp = this.service.file("bucket", "dir/a.txt");
        assertThrows(StorageException.class, txtOp::asZip);
        FileOperation bareNameOp = this.service.file("bucket", "dir/.zip");
        assertThrows(StorageException.class, bareNameOp::asZip);
        FileOperation zipxOp = this.service.file("bucket", "dir/a.zipx");
        assertThrows(StorageException.class, zipxOp::asZip);
        // 大写后缀合法
        this.service.file("bucket", "dir/A.ZIP").asZip();
    }

    @Test
    @DisplayName("asZip 非法源路径在构造期拒绝且不触发任何 IO")
    void shouldRejectIllegalZipPathWithoutIO() {
        FileOperation traversalOp = this.service.file("bucket", "dir/../a.zip");
        assertThrows(StorageException.class, traversalOp::asZip);
        FileOperation nullOp = this.service.file("bucket", null);
        assertThrows(StorageException.class, nullOp::asZip);
        assertTrue(this.strategy.downloadedPaths.isEmpty());
        assertTrue(this.strategy.uploadedPaths.isEmpty());
    }

    @Test
    @DisplayName("asZip 保留带前导斜杠的对象键，派生目标剥为相对路径")
    void shouldKeepLeadingSlashKeyAndDeriveRelativeTarget() throws IOException {
        this.strategy.downloadBytes.put("/dir/a.zip",
            buildZip(Map.of("a.txt", "A".getBytes(StandardCharsets.UTF_8))));

        ZipImportResp resp = this.service.file("bucket", "/dir/a.zip").asZip().importZip();

        assertEquals(1, resp.getSuccess());
        assertEquals(List.of("/dir/a.zip"), this.strategy.downloadedPaths);
        assertEquals(List.of("dir/a/a.txt"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("asZip 导入失败时下载流仍由服务关闭")
    void shouldCloseDownloadedStreamOnImportFailure() throws IOException {
        this.properties.setZipImportMaxEntryBytes(1);
        TrackingInputStream source = new TrackingInputStream(
            buildZip(Map.of("big.bin", "0123456789".getBytes(StandardCharsets.UTF_8))));
        this.strategy.downloadStreams.put("a.zip", source);

        ZipImportResp resp = this.service.file("bucket", "a.zip").asZip().importZip();

        assertEquals(1, resp.getFailed());
        assertEquals(1, source.closeCount, "下载流由 importZip 恰好关闭一次");
    }

    @Test
    @DisplayName("asZip 归档级错误时下载流同样被关闭")
    void shouldCloseDownloadedStreamOnArchiveError() throws IOException {
        // 首个本地文件头压缩方法字段非法触发归档级错误
        byte[] zip = buildZip(Map.of("a.txt", "A".getBytes(StandardCharsets.UTF_8)));
        zip[8] = 99;
        zip[9] = 0;
        TrackingInputStream source = new TrackingInputStream(zip);
        this.strategy.downloadStreams.put("a.zip", source);

        ZipFileOperation zipOp = this.service.file("bucket", "a.zip").asZip();
        assertThrows(StorageException.class, zipOp::importZip);
        assertEquals(1, source.closeCount);
    }

    /**
     * 构造内存 ZIP 包
     *
     * @param entries 条目名到内容的映射
     * @return ZIP 字节
     * @throws IOException 写入失败时抛出
     */
    private static byte[] buildZip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                zos.putNextEntry(new ZipEntry(e.getKey()));
                zos.write(e.getValue());
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }

    /**
     * 可观测关闭状态的字节输入流
     */
    private static class TrackingInputStream extends ByteArrayInputStream {

        private int closeCount;

        TrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closeCount++;
            super.close();
        }
    }

    /**
     * 记录式假存储策略：只实现本测试覆盖的调用面，记录入参便于断言
     */
    private static class FakeStrategy implements StorageStrategy {

        private final Map<String, InputStream> downloadStreams = new HashMap<>();
        private final Map<String, byte[]> downloadBytes = new HashMap<>();
        private final List<String> downloadedPaths = new ArrayList<>();
        private final List<String> uploadedPaths = new ArrayList<>();
        private final List<String> uploadedBuckets = new ArrayList<>();
        private final List<String> deletedPaths = new ArrayList<>();
        private final List<String> batchDeletedPaths = new ArrayList<>();
        private final List<String> batchDownloadPaths = new ArrayList<>();
        private final List<String> batchDownloadBuckets = new ArrayList<>();
        private final List<String> copyArgs = new ArrayList<>();
        private final List<String> moveArgs = new ArrayList<>();
        private byte[] rangeBytes = new byte[0];
        private long rangeStart = -1;
        private long rangeEnd = -1;
        private byte[] batchZip;

        @Override
        public void upload(String bucket, String path, MultipartFile file) {
            this.uploadedBuckets.add(bucket);
            this.uploadedPaths.add(path);
        }

        @Override
        public InputStream download(String bucket, String path) {
            this.downloadedPaths.add(path);
            InputStream stream = this.downloadStreams.get(path);
            if (stream != null) {
                return stream;
            }
            return new ByteArrayInputStream(this.downloadBytes.getOrDefault(path, new byte[0]));
        }

        @Override
        public InputStream batchDownload(String bucket, List<String> paths) {
            this.batchDownloadBuckets.add(bucket);
            this.batchDownloadPaths.addAll(paths);
            return new ByteArrayInputStream(this.batchZip);
        }

        @Override
        public InputStream downloadRange(String bucket, String path, long start, long end) {
            this.rangeStart = start;
            this.rangeEnd = end;
            return new ByteArrayInputStream(this.rangeBytes);
        }

        @Override
        public void delete(String bucket, String path) {
            this.deletedPaths.add(path);
        }

        @Override
        public void batchDelete(String bucket, List<String> paths) {
            this.batchDeletedPaths.addAll(paths);
        }

        @Override
        public boolean exists(String bucket, String path) {
            return false;
        }

        @Override
        public FileInfo getFileInfo(String bucket, String path) {
            FileInfo info = new FileInfo();
            info.setBucket(bucket);
            info.setPath(path);
            return info;
        }

        @Override
        public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
            return List.of();
        }

        @Override
        public void copy(String sourceBucket, String targetBucket, String sourcePath,
            String targetPath) {
            this.copyArgs.add(sourceBucket + "|" + targetBucket + "|" + sourcePath + "|"
                + targetPath);
        }

        @Override
        public void move(String sourceBucket, String targetBucket, String sourcePath,
            String targetPath) {
            this.moveArgs.add(sourceBucket + "|" + targetBucket + "|" + sourcePath + "|"
                + targetPath);
        }

        @Override
        public String getPlatform() {
            return PLATFORM;
        }

        @Override
        public String defaultBucket() {
            return "bucket";
        }

        @Override
        public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
            return "sig:get:" + bucket + ":" + path + ":" + expireSeconds;
        }

        @Override
        public String generateUploadPresignedUrl(String bucket, String path, long expireSeconds) {
            return "sig:put:" + bucket + ":" + path + ":" + expireSeconds;
        }

        @Override
        public MultipartInitResp initMultipartUpload(String bucket, String path, String contentType,
            Map<String, String> metadata) {
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
            // 假策略不需要中止分片行为
        }

        @Override
        public List<MultipartUploadResp> listParts(String bucket, String path, String uploadId) {
            return List.of();
        }
    }
}
