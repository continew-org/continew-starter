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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;
import software.amazon.awssdk.services.s3.model.UploadPartResponse;
import top.continew.starter.storage.autoconfigure.properties.OssStorageConfig;
import top.continew.starter.storage.common.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * OssStorageStrategy 未知长度流式上传单元测试。
 *
 * <p>探测缓冲阈值与分片缓冲（S3 下限 5MB）路径在本地由 mock {@link S3Client} 裁决：
 * 小流/空流落单次 PUT，超探测阈值走分片上传，中途失败须中止任务且不得 complete。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("OssStorageStrategy 未知长度流式上传单元测试")
class OssStorageStrategyStreamUploadTest {

    private static OssStorageStrategy newStrategy(S3Client client) {
        OssStorageConfig config = new OssStorageConfig();
        config.setPlatform("oss-test");
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("a");
        config.setSecretKey("b");
        config.setBucketName("bucket");
        // 探测阈值调小，避免小内容流落进分片路径；分片缓冲仍受 S3 下限 5MB 钳制
        config.setMultipartUploadThreshold(4096L);
        config.setMultipartUploadPartSize(4096L);
        return new OssStorageStrategy(config, client);
    }

    @Test
    @DisplayName("未知长度小流不超过探测阈值，落单次 PUT")
    void shouldPutSmallUnknownStream() {
        S3Client client = mock(S3Client.class);
        OssStorageStrategy strategy = newStrategy(client);
        byte[] content = new byte[1024];

        strategy.uploadStream("bucket", "small.bin", new ByteArrayInputStream(content),
            "application/octet-stream", -1);

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    @DisplayName("未知长度空流落零字节单次 PUT")
    void shouldPutEmptyUnknownStream() {
        S3Client client = mock(S3Client.class);
        OssStorageStrategy strategy = newStrategy(client);

        strategy.uploadStream("bucket", "empty.bin", new ByteArrayInputStream(new byte[0]),
            "application/octet-stream", -1);

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        verify(client, never()).createMultipartUpload(any(CreateMultipartUploadRequest.class));
    }

    @Test
    @DisplayName("upload 关闭由本方法打开的 MultipartFile 输入流")
    void shouldCloseOpenedMultipartInputStreamOnUpload() throws IOException {
        S3Client client = mock(S3Client.class);
        OssStorageStrategy strategy = newStrategy(client);
        CountingCloseStream in = new CountingCloseStream(new byte[16]);

        strategy.upload("bucket", "put.bin",
            new CountingCloseMultipartFile("put.bin", "application/octet-stream", in, 16));

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals(1, in.closeCount, "upload 打开的输入流应只关闭一次");
    }

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

    private static class CountingCloseMultipartFile
        extends org.springframework.mock.web.MockMultipartFile {

        private final InputStream in;

        CountingCloseMultipartFile(String name, String contentType, InputStream in,
            long size) {
            super(name, name, contentType, new byte[(int) size]);
            this.in = in;
        }

        @Override
        public InputStream getInputStream() {
            return this.in;
        }
    }

    @Test
    @DisplayName("未知长度超探测阈值走分片上传，以探测数据为首片")
    void shouldUploadLargeUnknownStreamAsMultipart() {
        S3Client client = mock(S3Client.class);
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(CreateMultipartUploadResponse.builder().uploadId("up-1").build());
        when(client.uploadPart(any(UploadPartRequest.class), any(RequestBody.class)))
            .thenReturn(UploadPartResponse.builder().eTag("etag-1").build());
        OssStorageStrategy strategy = newStrategy(client);
        // 4097 字节探测满触发分片：首片=探测 4096 + 余量 1，单片完成
        byte[] content = new byte[4097];

        strategy.uploadStream("bucket", "large.bin", new ByteArrayInputStream(content),
            "application/octet-stream", -1);

        verify(client).createMultipartUpload(any(CreateMultipartUploadRequest.class));
        verify(client).uploadPart(any(UploadPartRequest.class), any(RequestBody.class));
        verify(client).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }

    @Test
    @DisplayName("分片上传读取失败时中止任务且不 complete")
    void shouldAbortMultipartWhenReadFails() {
        S3Client client = mock(S3Client.class);
        when(client.createMultipartUpload(any(CreateMultipartUploadRequest.class)))
            .thenReturn(CreateMultipartUploadResponse.builder().uploadId("up-2").build());
        OssStorageStrategy strategy = newStrategy(client);
        InputStream failing = new InputStream() {

            private int emitted;

            @Override
            public int read() throws IOException {
                throw new IOException("模拟流读取失败");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (emitted >= 4096) {
                    throw new IOException("模拟流读取失败");
                }
                int n = Math.min(len, 4096 - emitted);
                emitted += n;
                return n;
            }
        };

        assertThrows(StorageException.class,
            () -> strategy.uploadStream("bucket", "fail.bin", failing,
                "application/octet-stream", -1));

        verify(client).abortMultipartUpload(any(AbortMultipartUploadRequest.class));
        verify(client, never()).completeMultipartUpload(any(CompleteMultipartUploadRequest.class));
    }
}
