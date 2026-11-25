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

package top.continew.starter.storage.core;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
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
import top.continew.starter.storage.domain.file.InputStreamMultipartFile;
import top.continew.starter.storage.domain.file.TempFileMultipartFile;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.domain.model.req.ThumbnailInfo;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.domain.model.resp.ZipImportResp;
import top.continew.starter.storage.engine.StorageStrategyRouter;
import top.continew.starter.storage.processor.preprocess.FileValidator;
import top.continew.starter.storage.processor.preprocess.ThumbnailProcessor;
import top.continew.starter.storage.processor.preprocess.impl.FileSizeValidator;
import top.continew.starter.storage.processor.progress.UploadProgressListener;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * FileStorageService 流式上传分派、范围下载与 ZIP 批量导入导出单元测试。
 *
 * <p>路由层由 mock {@link StorageStrategyRouter} 提供，落盘语义由记录式假策略裁决。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("FileStorageService 流式与 ZIP 行为单元测试")
class FileStorageServiceStreamTest {

    private static final String PLATFORM = "fake";

    private FakeStrategy strategy;
    private FileStorageService service;
    private StorageProperties properties;

    @BeforeEach
    void setUp() {
        StorageStrategyRouter router = mock(StorageStrategyRouter.class);
        this.strategy = new FakeStrategy();
        when(router.route(PLATFORM)).thenReturn(this.strategy);
        when(router.getDefaultStorage()).thenReturn(PLATFORM);
        this.properties = new StorageProperties();
        this.service = new FileStorageService(router, mock(ProcessorRegistry.class),
            mock(FileRecorder.class), this.properties);
    }

    @Test
    @DisplayName("流式文件在支持流式上传的策略下走 uploadStream，源流被关闭")
    void shouldDispatchStreamFileToUploadStream() {
        byte[] content = "stream-data".getBytes(StandardCharsets.UTF_8);
        TrackingInputStream in = new TrackingInputStream(content);
        this.strategy.streamSupport = true;

        this.service.upload(PLATFORM, "bucket", "a.bin",
            new InputStreamMultipartFile("file", "a.bin", "application/octet-stream", -1, in));

        assertTrue(this.strategy.streamUploadCalled, "应走流式上传而非临时文件兜底");
        assertNull(this.strategy.uploadedFile, "不应走常规 upload");
        assertArrayEquals(content, this.strategy.streamBytes);
        assertTrue(in.closed, "上传完成后源流应被关闭");
    }

    @Test
    @DisplayName("存在内容读取型校验器时流式文件先物化，校验器拿到可复读文件")
    void shouldMaterializeForContentReadingValidator() {
        byte[] content = "v-content".getBytes(StandardCharsets.UTF_8);
        this.service.addProcessor(new RecordingValidator());

        this.service.of(new ByteArrayInputStream(content), "v.bin",
            "application/octet-stream").upload();

        assertTrue(this.strategy.uploadedFile instanceof TempFileMultipartFile,
            "校验器读内容需可复读文件，应先物化");
        org.junit.jupiter.api.Assertions.assertFalse(this.strategy.streamUploadCalled);
    }

    @Test
    @DisplayName("流式文件加缩略图需求时物化为临时文件再走常规上传")
    void shouldMaterializeStreamWhenThumbnailRequested() {
        byte[] content = "thumb".getBytes(StandardCharsets.UTF_8);
        this.service.addProcessor(new PassthroughThumbnailProcessor());

        FileInfo info = this.service.of(new ByteArrayInputStream(content), "pic.bin",
            "image/png").thumbnail(10, 10).upload();

        // 缩略图需二次读取：主流物化为临时文件，随后主文件与缩略图各上传一次
        assertEquals(2, this.strategy.uploadedPaths.size());
        org.junit.jupiter.api.Assertions.assertFalse(this.strategy.streamUploadCalled);
        org.junit.jupiter.api.Assertions.assertNotNull(info);

    }

    @Test
    @DisplayName("物化路径读取源流失败时记为物化失败并关闭源流")
    void shouldFailMaterializationWhenSourceReadFails() {
        this.service.addProcessor(new RecordingValidator());
        InputStream failing = new InputStream() {

            private int emitted;

            @Override
            public int read() throws IOException {
                throw new IOException("模拟流读取失败");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (emitted >= 4) {
                    throw new IOException("模拟流读取失败");
                }
                int n = Math.min(len, 4 - emitted);
                emitted += n;
                return n;
            }
        };

        UploadPretreatment pretreatment = this.service.of(failing, "m.bin",
            "application/octet-stream");
        assertThrows(StorageException.class, pretreatment::upload);
    }

    @Test
    @DisplayName("策略不支持流式上传时物化为临时文件回退常规 upload")
    void shouldMaterializeStreamForUnsupportedStrategy() {
        byte[] content = "payload".getBytes(StandardCharsets.UTF_8);
        this.strategy.streamSupport = false;

        this.service.upload(PLATFORM, "bucket", "a.bin",
            new InputStreamMultipartFile("file", "a.bin", "application/octet-stream", -1,
                new ByteArrayInputStream(content)));

        assertTrue(this.strategy.uploadedFile instanceof TempFileMultipartFile);
        assertEquals(content.length, this.strategy.uploadedFile.getSize());
    }

    @Test
    @DisplayName("of(InputStream) 端到端：文件名生成、流式分派与返回 FileInfo")
    void shouldUploadStreamViaPretreatment() {
        byte[] content = "e2e".getBytes(StandardCharsets.UTF_8);

        FileInfo info = this.service.of(
            new ByteArrayInputStream(content), "hello.txt", "text/plain").upload();

        assertNotNull(info);
        assertTrue(this.strategy.streamUploadCalled);
        assertArrayEquals(content, this.strategy.streamBytes);
    }

    @Test
    @DisplayName("流式上传按大小校验器上限硬限：超限抛错且源流关闭")
    void shouldEnforceStreamSizeLimitDuringUpload() {
        // 大小校验器上限 4 字节，流实发 10 字节，读取侧超限即失败
        this.service.addProcessor(new FileSizeValidator(4));
        this.strategy.streamSupport = true;
        TrackingInputStream in = new TrackingInputStream(
            "0123456789".getBytes(StandardCharsets.UTF_8));
        UploadPretreatment pretreatment = this.service.of(in, "big.bin",
            "application/octet-stream");

        assertThrows(StorageException.class, pretreatment::upload);
        assertTrue(in.closed, "校验失败路径源流也必须关闭");
    }

    @Test
    @DisplayName("流式上传中断：源关闭、进度走 onError 且不报 onComplete")
    void shouldReportErrorNotCompleteOnStreamFailure() {
        this.strategy.streamSupport = true;
        this.strategy.streamFailsAt = 3;
        UploadProgressListener listener = mock(UploadProgressListener.class,
            CALLS_REAL_METHODS);
        TrackingInputStream in = new TrackingInputStream(
            "0123456789".getBytes(StandardCharsets.UTF_8));

        UploadPretreatment pretreatment = this.service.of(in, "fail.bin",
            "application/octet-stream").onProgress(listener);
        assertThrows(StorageException.class, pretreatment::upload);

        assertTrue(in.closed);
        verify(listener).onError(any(Exception.class));
        verify(listener, never()).onComplete();
    }

    @Test
    @DisplayName("流式上传成功显式 complete，未知长度回传实际读取字节数")
    void shouldCompleteProgressAndReportActualSize() {
        byte[] content = "0123456789".getBytes(StandardCharsets.UTF_8);
        this.strategy.streamSupport = true;
        this.strategy.fileSize = -1L;
        UploadProgressListener listener = mock(UploadProgressListener.class,
            CALLS_REAL_METHODS);

        FileInfo info = this.service.of(new TrackingInputStream(content), "ok.bin",
            "application/octet-stream").onProgress(listener).upload();

        verify(listener).onComplete();
        verify(listener, never()).onError(any(Exception.class));
        assertEquals(content.length, info.getSize(), "未知长度流须回传实际读取大小而非 -1");
    }

    @Test
    @DisplayName("downloadRange 只校验参数合法性，大小解析与钳制交给策略")
    void shouldPassRangeThroughToStrategy() throws IOException {
        this.strategy.rangeBytes = "234".getBytes(StandardCharsets.UTF_8);

        try (InputStream in = this.service.downloadRange(PLATFORM, "bucket", "f.bin", 8, 100)) {
            in.readAllBytes();
        }
        assertEquals(8, this.strategy.rangeStart);
        assertEquals(100, this.strategy.rangeEnd);
        assertEquals(0, this.strategy.getFileInfoCalls, "门面不得为校验预查文件信息");

        assertThrows(StorageException.class, () -> this.service.downloadRange(PLATFORM, "bucket",
            "f.bin", -1, 2));
        assertThrows(StorageException.class, () -> this.service.downloadRange(PLATFORM, "bucket",
            "f.bin", 10, 2));
    }

    @Test
    @DisplayName("batchDownload 走策略 SPI，尊重第三方自定义实现")
    void shouldDelegateBatchDownloadToStrategy() throws IOException {
        byte[] marker = "custom-zip".getBytes(StandardCharsets.UTF_8);
        this.strategy.customBatchZip = marker;

        try (InputStream zip = this.service.batchDownload(PLATFORM, "bucket", List.of("x"))) {
            assertArrayEquals(marker, zip.readAllBytes());
        }
        assertTrue(this.strategy.batchDownloadCalled);
    }

    @Test
    @DisplayName("batchDownload 产出合法 ZIP：条目名规范化且重名自动去重")
    void shouldProduceValidZipWithDedupedEntryNames() throws IOException {
        this.strategy.downloadBytes.put("dir/a.txt", "one".getBytes(StandardCharsets.UTF_8));

        try (InputStream zipStream = this.service.batchDownload(PLATFORM, "bucket",
            List.of("dir/a.txt", "dir/a.txt"));
            ZipInputStream zin = new ZipInputStream(zipStream)) {
            ZipEntry first = zin.getNextEntry();
            assertEquals("dir/a.txt", first.getName());
            assertArrayEquals("one".getBytes(StandardCharsets.UTF_8), zin.readAllBytes());
            ZipEntry second = zin.getNextEntry();
            assertEquals("dir/a (1).txt", second.getName());
            assertNull(zin.getNextEntry());
        }
    }

    @Test
    @DisplayName("writeBatchDownload 与 batchDownload 字节一致")
    void shouldWriteSameZipToOutputStream() throws IOException {
        this.strategy.downloadBytes.put("f.txt", "data".getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        this.service.writeBatchDownload(PLATFORM, "bucket", List.of("f.txt"), out);
        try (ZipInputStream zin = new ZipInputStream(new ByteArrayInputStream(out
            .toByteArray()))) {
            assertEquals("f.txt", zin.getNextEntry().getName());
        }
    }

    @Test
    @DisplayName("importZip 流式导入：非法与绝对路径条目名记失败继续")
    void shouldImportZipAndSkipUnsafeEntry() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("good/a.txt", "A".getBytes(StandardCharsets.UTF_8));
        entries.put("../evil.txt", "E".getBytes(StandardCharsets.UTF_8));
        entries.put("/abs/path.txt", "F".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", "inbox",
            new ByteArrayInputStream(zip));

        assertEquals(3, resp.getTotal());
        assertEquals(1, resp.getSuccess());
        assertEquals(2, resp.getFailed());
        assertEquals(List.of("inbox/good/a.txt"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("importZip 单条超解压大小记失败并停止导入")
    void shouldStopWhenEntryExceedsSizeLimit() throws IOException {
        this.properties.setZipImportMaxEntryBytes(4);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("big.bin", "0123456789".getBytes(StandardCharsets.UTF_8));
        entries.put("ok.bin", "ab".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        // 单条超限即终止，后续条目不再处理，此前无成功上传
        assertEquals(1, resp.getTotal());
        assertEquals(0, resp.getSuccess());
        assertEquals(1, resp.getFailed());
        assertTrue(this.strategy.uploadedPaths.isEmpty());
    }

    @Test
    @DisplayName("importZip 单条正好达到上限视为成功")
    void shouldAcceptEntryAtExactSizeLimit() throws IOException {
        this.properties.setZipImportMaxEntryBytes(10);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("ok.bin", "0123456789".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(1, resp.getSuccess());
        assertEquals(0, resp.getFailed());
    }

    @Test
    @DisplayName("importZip 被跳过 STORED 条目的排空超限记该条一次失败并停止")
    void shouldCountSkippedStoredEntryAgainstTotal() throws IOException {
        this.properties.setZipImportMaxTotalBytes(5);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("../bad.bin", "12345678".getBytes(StandardCharsets.UTF_8));
        entries.put("ok.bin", "ab".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildStoredZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        // 排空经计量 read 超总量限制：同一非法名条目只计一次失败
        assertEquals(1, resp.getTotal());
        assertEquals(0, resp.getSuccess());
        assertEquals(1, resp.getFailed());
        assertTrue(resp.getEntries().get(0).getError().contains("总解压大小限制"));
        assertTrue(this.strategy.uploadedPaths.isEmpty());
    }

    @Test
    @DisplayName("importZip 被跳过 DEFLATED 条目的排空超限记该条一次失败并停止")
    void shouldCountSkippedDeflatedEntryAgainstTotal() throws IOException {
        this.properties.setZipImportMaxTotalBytes(5);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("../bad.bin", "12345678".getBytes(StandardCharsets.UTF_8));
        entries.put("ok.bin", "ab".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(1, resp.getTotal());
        assertEquals(0, resp.getSuccess());
        assertEquals(1, resp.getFailed());
        assertTrue(resp.getEntries().get(0).getError().contains("总解压大小限制"));
        assertTrue(this.strategy.uploadedPaths.isEmpty());
    }

    @Test
    @DisplayName("importZip 尾部 STORED 条目成功导入")
    void shouldImportStoredEntry() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("raw.bin", "stored-data".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildStoredZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(1, resp.getSuccess());
        assertEquals(List.of("raw.bin"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("importZip 总量超限记失败停止，此前成功保留不回滚")
    void shouldStopOnTotalLimitKeepingPriorSuccesses() throws IOException {
        this.properties.setZipImportMaxTotalBytes(3);
        this.strategy.existing.add("dup.txt");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        entries.put("dup.txt", "bb".getBytes(StandardCharsets.UTF_8));
        entries.put("c.bin", "cc".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        // a.bin 解压 2 字节成功；dup.txt 物化后累计 4 > 3 触发总量限制，未上传即失败；
        // c.bin 因循环中断不再出现，此前成功保留不回滚
        assertEquals(2, resp.getTotal());
        assertEquals(1, resp.getSuccess());
        assertEquals(1, resp.getFailed());
        assertEquals(List.of("a.bin"), this.strategy.uploadedPaths);
        assertTrue(this.strategy.deletedPaths.isEmpty(), "不拥有目标对象，禁止事后删除");
    }

    @Test
    @DisplayName("importZip 目录条目计入总数并记失败")
    void shouldCountDirectoryEntryInImportTotal() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("dir/", new byte[0]);
        entries.put("dir/a.txt", "A".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(2, resp.getTotal());
        assertEquals(1, resp.getSuccess());
        assertEquals(1, resp.getFailed());
    }

    @Test
    @DisplayName("importZip 条目数超限（含目录）记失败停止")
    void shouldStopOnEntryCountLimit() throws IOException {
        this.properties.setZipImportMaxEntries(1);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        entries.put("b.bin", "bb".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        // 首条被拒条目计入 total 并留痕，其内容不读取
        assertEquals(2, resp.getTotal());
        assertEquals(1, resp.getSuccess());
        assertEquals(1, resp.getFailed());
        assertEquals("b.bin", resp.getEntries().get(1).getName());
        assertEquals(List.of("a.bin"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("importZip 源流关闭失败按归档级错误抛出")
    void shouldExposeSourceCloseFailure() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        TrackingInputStream zipSource = new TrackingInputStream(buildZip(entries));
        zipSource.throwOnClose = true;

        StorageException e = assertThrows(StorageException.class,
            () -> this.service.importZip(PLATFORM, "bucket", null, zipSource));

        assertInstanceOf(IOException.class, e.getCause());
        assertTrue(zipSource.closed);
        assertEquals(List.of("a.bin"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("importZip 下一条目头部损坏按归档级错误抛出")
    void shouldThrowOnCorruptedNextEntryHeader() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        entries.put("b.bin", "bb".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);
        // 第二个本地文件头（PK\x03\x04）的压缩方法字段改为非法值
        int firstHeader = indexOf(zip, new byte[] {0x50, 0x4B, 3, 4}, 0);
        int secondHeader = indexOf(zip, new byte[] {0x50, 0x4B, 3, 4},
            firstHeader + 4);
        zip[secondHeader + 8] = 99;
        zip[secondHeader + 9] = 0;

        ByteArrayInputStream zipIn = new ByteArrayInputStream(zip);
        StorageException e = assertThrows(StorageException.class,
            () -> this.service.importZip(PLATFORM, "bucket", null, zipIn));

        assertInstanceOf(IOException.class, e.getCause());
        assertEquals(List.of("a.bin"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("importZip 配置 Long.MAX_VALUE 限额正常导入")
    void shouldImportWithMaxValueLimits() throws IOException {
        this.properties.setZipImportMaxEntryBytes(Long.MAX_VALUE);
        this.properties.setZipImportMaxTotalBytes(Long.MAX_VALUE);
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        entries.put("b.bin", "bb".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(2, resp.getSuccess());
        assertEquals(0, resp.getFailed());
    }

    @Test
    @DisplayName("importZip 父路径尾斜杠规范化后按单斜杠拼接")
    void shouldStripTrailingSlashOfParentPath() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.txt", "A".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", "folder/",
            new ByteArrayInputStream(zip));

        assertEquals(1, resp.getSuccess());
        assertEquals(List.of("folder/a.txt"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("importZip 由方法关闭 ZIP 源流")
    void shouldCloseZipSourceByMethod() throws IOException {
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        TrackingInputStream zipSource = new TrackingInputStream(buildZip(entries));

        this.service.importZip(PLATFORM, "bucket", null, zipSource);

        assertTrue(zipSource.closed);
    }

    @Test
    @DisplayName("importZip 同名目标覆盖写入，不做存在性保护")
    void shouldOverwriteExistingObjectOnImport() throws IOException {
        this.strategy.existing.add("a.bin");
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("a.bin", "aa".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(1, resp.getSuccess());
        assertEquals(List.of("a.bin"), this.strategy.uploadedPaths);
        assertTrue(this.strategy.deletedPaths.isEmpty());
    }

    @Test
    @DisplayName("importZip 无消息异常的上传失败如实记失败并继续下一条")
    void shouldRecordNullMessageUploadFailureAndContinue() throws IOException {
        // 策略对 bad.bin 抛无消息异常：成功/失败与消息分离，不得把 null 消息误判成功
        this.strategy.uploadFailures.put("bad.bin", new StorageException((String) null));
        Map<String, byte[]> entries = new LinkedHashMap<>();
        entries.put("bad.bin", "aa".getBytes(StandardCharsets.UTF_8));
        entries.put("ok.bin", "bb".getBytes(StandardCharsets.UTF_8));
        byte[] zip = buildZip(entries);

        ZipImportResp resp = this.service.importZip(PLATFORM, "bucket", null,
            new ByteArrayInputStream(zip));

        assertEquals(2, resp.getTotal());
        assertEquals(1, resp.getSuccess());
        assertEquals(1, resp.getFailed());
        assertNull(resp.getEntries().get(0).getError(), "失败条目错误消息可为 null，但状态必为失败");
        assertEquals(List.of("ok.bin"), this.strategy.uploadedPaths);
    }

    @Test
    @DisplayName("直传路径源流恰由 closeStream 关闭一次")
    void shouldCloseSourceStreamExactlyOnceOnDirectUpload() {
        CountingCloseStream in = new CountingCloseStream(
            "direct".getBytes(StandardCharsets.UTF_8));
        this.strategy.streamSupport = true;

        this.service.upload(PLATFORM, "bucket", "d.bin",
            new InputStreamMultipartFile("file", "d.bin", "application/octet-stream", -1,
                in));

        assertTrue(this.strategy.streamUploadCalled);
        assertEquals(1, in.closeCount, "直传源流应只关闭一次");
    }

    @Test
    @DisplayName("物化路径源流恰由 closeStream 关闭一次")
    void shouldCloseSourceStreamExactlyOnceOnMaterialize() {
        CountingCloseStream in = new CountingCloseStream(
            "mat".getBytes(StandardCharsets.UTF_8));
        this.strategy.streamSupport = false;

        this.service.upload(PLATFORM, "bucket", "m.bin",
            new InputStreamMultipartFile("file", "m.bin", "application/octet-stream", -1,
                in));

        assertTrue(this.strategy.uploadedFile instanceof TempFileMultipartFile);
        assertEquals(1, in.closeCount, "物化源流应只关闭一次");
    }

    @Test
    @DisplayName("源流关闭失败仅记日志，不吞掉已成功的上传")
    void shouldNotFailUploadWhenSourceCloseFails() {
        this.strategy.streamSupport = true;
        TrackingInputStream in = new TrackingInputStream(
            "close-fail".getBytes(StandardCharsets.UTF_8));
        in.throwOnClose = true;

        // 上传成功路径源流关闭抛错：log-only，不得把落盘后成功误报为失败
        this.service.upload(PLATFORM, "bucket", "c.bin",
            new InputStreamMultipartFile("file", "c.bin", "application/octet-stream", -1,
                in));

        assertTrue(this.strategy.streamUploadCalled);
        assertTrue(in.closed);
    }

    /**
     * 记录 close() 调用次数的字节输入流
     */
    private static class CountingCloseStream extends ByteArrayInputStream {

        private int closeCount;

        CountingCloseStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closeCount++;
            super.close();
        }
    }

    /**
     * 构造内存 ZIP 包
     *
     * @param entries 条目名到内容的映射（迭代顺序即写入顺序）
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
     * 构造 STORED（未压缩）内存 ZIP 包
     *
     * @param entries 条目名到内容的映射（迭代顺序即写入顺序）
     * @return ZIP 字节
     * @throws IOException 写入失败时抛出
     */
    private static byte[] buildStoredZip(Map<String, byte[]> entries) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (Map.Entry<String, byte[]> e : entries.entrySet()) {
                CRC32 crc = new CRC32();
                crc.update(e.getValue());
                ZipEntry entry = new ZipEntry(e.getKey());
                entry.setMethod(ZipEntry.STORED);
                entry.setSize(e.getValue().length);
                entry.setCompressedSize(e.getValue().length);
                entry.setCrc(crc.getValue());
                zos.putNextEntry(entry);
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

        private boolean closed;
        private boolean throwOnClose;

        TrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
            if (throwOnClose) {
                throw new IOException("模拟源流关闭失败");
            }
        }
    }

    /**
     * 定位字节序列首次出现的位置
     *
     * @param haystack 被搜索数组
     * @param needle   目标序列
     * @param from     起始下标（含）
     * @return 首次出现下标，未找到返回 -1
     */
    private static int indexOf(byte[] haystack, byte[] needle, int from) {
        outer: for (int i = from; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    /**
     * 内容读取型校验器：除 FileSize/FileType 外即按"可能读内容"处理
     */
    private static class RecordingValidator implements FileValidator {

        @Override
        public String getName() {
            return RecordingValidator.class.getSimpleName();
        }

        @Override
        public boolean support(UploadContext context) {
            return true;
        }

        @Override
        public void validate(UploadContext context) {
            // 记录不读内容；触发 hasContentReadingProcessors → 物化路径即可
        }
    }

    /**
     * 恒支持缩略图的假处理器：返回固定字节，不经图像解码
     */
    private static class PassthroughThumbnailProcessor implements ThumbnailProcessor {

        @Override
        public String getName() {
            return PassthroughThumbnailProcessor.class.getSimpleName();
        }

        @Override
        public boolean support(UploadContext context) {
            return true;
        }

        @Override
        public ThumbnailInfo process(UploadContext context, InputStream sourceInputStream) {
            ThumbnailInfo info = new ThumbnailInfo();
            info.setData(new byte[] {1, 2});
            info.setFormat("png");
            info.setWidth(10);
            info.setHeight(10);
            return info;
        }
    }

    /**
     * 记录式假存储策略：只实现本测试覆盖的调用面，记录入参便于断言
     */
    private static class FakeStrategy implements StorageStrategy {

        private boolean streamSupport = true;
        private boolean streamUploadCalled;
        private byte[] streamBytes;
        private MultipartFile uploadedFile;
        private Long fileSize = 10L;
        private byte[] rangeBytes = new byte[0];
        private long rangeStart = -1;
        private long rangeEnd = -1;
        private final Map<String, byte[]> downloadBytes = new HashMap<>();
        private final List<String> uploadedPaths = new ArrayList<>();
        private final List<String> deletedPaths = new ArrayList<>();
        private final Set<String> existing = new HashSet<>();
        private int getFileInfoCalls;
        private boolean batchDownloadCalled;
        private int streamFailsAt = -1;
        private byte[] customBatchZip;
        private final Map<String, RuntimeException> uploadFailures = new HashMap<>();

        @Override
        public void upload(String bucket, String path, MultipartFile file) {
            RuntimeException failure = this.uploadFailures.get(path);
            if (failure != null) {
                throw failure;
            }
            this.uploadedFile = file;
            this.uploadedPaths.add(path);
        }

        @Override
        public void uploadStream(String bucket, String path, InputStream inputStream,
            String contentType, long contentLength) {
            this.streamUploadCalled = true;
            try {
                if (this.streamFailsAt >= 0) {
                    byte[] head = inputStream.readNBytes(this.streamFailsAt);
                    this.streamBytes = head;
                    throw new IOException("模拟流读取中断");
                }
                this.streamBytes = inputStream.readAllBytes();
            } catch (IOException e) {
                throw new StorageException("流读取失败", e);
            }
        }

        @Override
        public boolean supportUploadStream() {
            return this.streamSupport;
        }

        @Override
        public InputStream download(String bucket, String path) {
            return new ByteArrayInputStream(this.downloadBytes.get(path));
        }

        @Override
        public InputStream batchDownload(String bucket, List<String> paths) {
            this.batchDownloadCalled = true;
            if (this.customBatchZip != null) {
                return new ByteArrayInputStream(this.customBatchZip);
            }
            return StorageStrategy.super.batchDownload(bucket, paths);
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
            paths.forEach(path -> delete(bucket, path));
        }

        @Override
        public boolean exists(String bucket, String path) {
            return this.existing.contains(path);
        }

        @Override
        public FileInfo getFileInfo(String bucket, String path) {
            this.getFileInfoCalls++;
            if (this.fileSize == null) {
                return null;
            }
            FileInfo info = new FileInfo();
            info.setBucket(bucket);
            info.setPath(path);
            info.setSize(this.fileSize);
            return info;
        }

        @Override
        public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
            return List.of();
        }

        @Override
        public void copy(String sourceBucket, String targetBucket, String sourcePath,
            String targetPath) {
            // 假策略不需要复制行为
        }

        @Override
        public void move(String sourceBucket, String targetBucket, String sourcePath,
            String targetPath) {
            // 假策略不需要移动行为
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
            return null;
        }

        @Override
        public String generateUploadPresignedUrl(String bucket, String path, long expireSeconds) {
            return null;
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
