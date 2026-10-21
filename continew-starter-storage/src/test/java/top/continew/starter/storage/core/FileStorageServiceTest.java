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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import top.continew.starter.storage.domain.model.req.MultipartUploadInitReq;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.FilePartInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.engine.StorageStrategyRouter;
import top.continew.starter.storage.processor.registry.ProcessorRegistry;
import top.continew.starter.storage.service.FileRecorder;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.util.List;

/**
 * 分片上传会话复用测试
 *
 * <p>
 * 会话复用只应在平台、存储桶、原始文件名（空白视为未传）与上级目录一致时发生；存储文件名不参与复用键，
 * 未传原始文件名时存储名不同仍会复用（与旧行为一致）。复用不同名或不同目录的会话会让后一次上传落进
 * 前一次的目录。
 * </p>
 *
 * @author luoqiz
 * @since 3.0.0
 */
class FileStorageServiceTest {

    private static final String PLATFORM = "local";

    private static final String BUCKET = "bucket";

    private static final String MD5 = "e3b0c44298fc1c149afbf4c8996fb924";

    private static final String STORED_NAME = "3f2a1c9e5b7d4a6f8c0e1d2b3a4c5d6e.pdf";

    private FileRecorder fileRecorder;

    private StorageStrategy storageStrategy;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        StorageStrategyRouter router = mock(StorageStrategyRouter.class);
        this.fileRecorder = mock(FileRecorder.class);
        this.storageStrategy = mock(StorageStrategy.class);
        when(router.route(PLATFORM)).thenReturn(this.storageStrategy);
        this.fileStorageService = new FileStorageService(router, mock(ProcessorRegistry.class),
            this.fileRecorder);
    }

    @Test
    void shouldReuseSessionWhenOriginalFileNameAndParentPathMatch() {
        MultipartInitResp cachedSession = this.buildSession("upload-cached", "年度报告.pdf", "/docs");
        when(this.fileRecorder.getUploadIdBySessionKey(this.sessionKey("年度报告.pdf", "/docs")))
            .thenReturn("upload-cached");
        when(this.fileRecorder.getMultipartSession("upload-cached")).thenReturn(cachedSession);
        FilePartInfo part = new FilePartInfo();
        part.setPartNumber(1);
        when(this.fileRecorder.getFileParts("upload-cached")).thenReturn(List.of(part));

        MultipartInitResp resp = this.fileStorageService.initMultipartUpload(this
            .buildReq("年度报告.pdf", "/docs/"));

        assertEquals("upload-cached", resp.getUploadId());
        assertTrue(resp.getUploadedPartNumbers().contains(1));
        verify(this.storageStrategy, never()).initMultipartUpload(any(), any(), any(), any());
        verify(this.fileRecorder, never()).saveMultipartSession(any(), any(), any());
    }

    @Test
    void shouldCreateNewSessionWhenOriginalFileNameDiffers() {
        MultipartInitResp cachedSession = this.buildSession("upload-cached", "工资表.xlsx", "/docs");
        when(this.fileRecorder.getUploadIdBySessionKey(this.sessionKey("b.pdf", "/docs")))
            .thenReturn("upload-cached");
        when(this.fileRecorder.getMultipartSession("upload-cached")).thenReturn(cachedSession);
        MultipartInitResp newSession = new MultipartInitResp();
        newSession.setUploadId("upload-new");
        newSession.setPlatform(PLATFORM);
        newSession.setBucket(BUCKET);
        when(this.storageStrategy.initMultipartUpload(any(), any(), any(), any()))
            .thenReturn(newSession);

        MultipartInitResp resp = this.fileStorageService.initMultipartUpload(this
            .buildReq("b.pdf", "/docs"));

        assertEquals("upload-new", resp.getUploadId());
        assertEquals("b.pdf", resp.getOriginalFileName());
        ArgumentCaptor<MultipartInitResp> sessionCaptor =
            ArgumentCaptor.forClass(MultipartInitResp.class);
        verify(this.fileRecorder).saveMultipartSession(eq("upload-new"), sessionCaptor.capture(),
            any());
        assertEquals("b.pdf", sessionCaptor.getValue().getOriginalFileName());
        verify(this.fileRecorder).setSessionKeyMapping(this.sessionKey("b.pdf", "/docs"),
            "upload-new");
    }

    @Test
    void shouldCreateNewSessionWhenParentPathDiffers() {
        MultipartInitResp cachedSession = this.buildSession("upload-cached", "年度报告.pdf", "/docs");
        when(this.fileRecorder.getUploadIdBySessionKey(this.sessionKey("年度报告.pdf", "/archive")))
            .thenReturn("upload-cached");
        when(this.fileRecorder.getMultipartSession("upload-cached")).thenReturn(cachedSession);
        MultipartInitResp newSession = new MultipartInitResp();
        newSession.setUploadId("upload-new");
        newSession.setPlatform(PLATFORM);
        newSession.setBucket(BUCKET);
        when(this.storageStrategy.initMultipartUpload(any(), any(), any(), any()))
            .thenReturn(newSession);

        MultipartInitResp resp = this.fileStorageService.initMultipartUpload(this
            .buildReq("年度报告.pdf", "/archive"));

        assertEquals("upload-new", resp.getUploadId());
        verify(this.fileRecorder).saveMultipartSession(eq("upload-new"), any(), any());
    }

    @Test
    void shouldReuseSessionWhenOriginalFileNameBlank() {
        MultipartInitResp cachedSession = this.buildSession("upload-cached", null, "/docs");
        when(this.fileRecorder.getUploadIdBySessionKey(this.sessionKey(null, "/docs")))
            .thenReturn("upload-cached");
        when(this.fileRecorder.getMultipartSession("upload-cached")).thenReturn(cachedSession);

        MultipartInitResp resp = this.fileStorageService.initMultipartUpload(this.buildReq("",
            "/docs"));

        assertEquals("upload-cached", resp.getUploadId());
        verify(this.storageStrategy, never()).initMultipartUpload(any(), any(), any(), any());
    }

    @Test
    void shouldReuseSessionForLegacyCallerWhenFileNameDiffers() {
        MultipartInitResp cachedSession = this.buildSession("upload-cached", null, "/docs");
        when(this.fileRecorder.getUploadIdBySessionKey(this.sessionKey(null, "/docs")))
            .thenReturn("upload-cached");
        when(this.fileRecorder.getMultipartSession("upload-cached")).thenReturn(cachedSession);

        MultipartUploadInitReq req = this.buildReq(null, "/docs");
        req.setFileName("another-stored-name.pdf");
        MultipartInitResp resp = this.fileStorageService.initMultipartUpload(req);

        assertEquals("upload-cached", resp.getUploadId());
        verify(this.storageStrategy, never()).initMultipartUpload(any(), any(), any(), any());
    }

    @Test
    void shouldDeleteMappingWithExpectedUploadId() {
        MultipartInitResp session = this.buildSession("upload-1", "年度报告.pdf", "/docs");
        when(this.fileRecorder.getMultipartSession("upload-1")).thenReturn(session);

        this.fileStorageService.abortMultipartUpload("upload-1");

        verify(this.fileRecorder)
            .deleteSessionKeyMapping(this.sessionKey("年度报告.pdf", "/docs"), "upload-1");
    }

    @Test
    void shouldUseSameKeyWhenAbortingWithRootParentPath() {
        MultipartInitResp session = this.buildSession("upload-1", "年度报告.pdf", null);
        when(this.fileRecorder.getMultipartSession("upload-1")).thenReturn(session);

        this.fileStorageService.abortMultipartUpload("upload-1");

        verify(this.fileRecorder)
            .deleteSessionKeyMapping(this.sessionKey("年度报告.pdf", null), "upload-1");
    }

    @Test
    void shouldDeleteSessionMappingOnlyWhenUploadIdMatches() {
        FileRecorder matched = mock(FileRecorder.class, CALLS_REAL_METHODS);
        when(matched.getUploadIdBySessionKey("session-key")).thenReturn("upload-1");
        assertTrue(matched.deleteSessionKeyMapping("session-key", "upload-1"));
        verify(matched).deleteSessionKeyMapping("session-key");

        FileRecorder overwritten = mock(FileRecorder.class, CALLS_REAL_METHODS);
        when(overwritten.getUploadIdBySessionKey("session-key")).thenReturn("upload-other");
        assertFalse(overwritten.deleteSessionKeyMapping("session-key", "upload-1"));
        verify(overwritten, never()).deleteSessionKeyMapping(any());
    }

    @Test
    void shouldBackfillOriginalFileNameFromSessionWhenCompleting() {
        MultipartInitResp session = this.buildSession("upload-1", "年度报告.pdf", "/docs");
        when(this.fileRecorder.getMultipartSession("upload-1")).thenReturn(session);
        FilePartInfo part = new FilePartInfo();
        part.setPartNumber(1);
        part.setPartETag("etag-1");
        part.setPartSize(1024L);
        part.setStatus("SUCCESS");
        when(this.fileRecorder.getFileParts("upload-1")).thenReturn(List.of(part));
        FileInfo fileInfo = new FileInfo();
        fileInfo.setName(STORED_NAME);
        fileInfo.setPath("docs/" + STORED_NAME);
        when(this.storageStrategy.completeMultipartUpload(any(), any(), any(), any(), anyBoolean()))
            .thenReturn(fileInfo);

        this.fileStorageService.completeMultipartUpload("upload-1", null);

        ArgumentCaptor<FileInfo> fileInfoCaptor = ArgumentCaptor.forClass(FileInfo.class);
        verify(this.fileRecorder).update(fileInfoCaptor.capture());
        assertEquals("年度报告.pdf", fileInfoCaptor.getValue().getOriginalFileName());
    }

    @Test
    void shouldBuildSessionKeyWithFullUploadDimension() {
        String sessionKey = FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5, "a.pdf",
            "/docs");

        assertEquals("c029f90cd360648ab8fdf5ac51fa8fb7279d9d6228b87c4ef5a702cd1b76eed7",
            sessionKey);
        assertNotEquals(sessionKey, FileStorageService.buildSessionKey("s3", BUCKET, MD5, "a.pdf",
            "/docs"));
        assertNotEquals(sessionKey, FileStorageService.buildSessionKey(PLATFORM, "other", MD5,
            "a.pdf", "/docs"));
        assertNotEquals(sessionKey, FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5,
            "b.pdf", "/docs"));
        assertNotEquals(sessionKey, FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5,
            "a.pdf", "/archive"));
        assertEquals(sessionKey, FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5, "a.pdf",
            "/docs/"));
        assertEquals(FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5, "a.pdf", null),
            FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5, "a.pdf", "/"));
    }

    private String sessionKey(String originalFileName, String parentPath) {
        return FileStorageService.buildSessionKey(PLATFORM, BUCKET, MD5, originalFileName,
            parentPath);
    }

    private MultipartInitResp buildSession(String uploadId, String originalFileName,
        String parentPath) {
        MultipartInitResp session = new MultipartInitResp();
        session.setUploadId(uploadId);
        session.setPlatform(PLATFORM);
        session.setBucket(BUCKET);
        session.setFileName(STORED_NAME);
        session.setOriginalFileName(originalFileName);
        session.setParentPath(parentPath);
        session.setFileMd5(MD5);
        session.setPath("docs/" + session.getFileName());
        return session;
    }

    private MultipartUploadInitReq buildReq(String originalFileName, String parentPath) {
        MultipartUploadInitReq req = new MultipartUploadInitReq();
        req.setPlatform(PLATFORM);
        req.setBucket(BUCKET);
        req.setFileName(STORED_NAME);
        req.setOriginalFileName(originalFileName);
        req.setParentPath(parentPath);
        req.setFileMd5(MD5);
        req.setFileSize(1024L);
        req.setContentType("application/pdf");
        return req;
    }
}
