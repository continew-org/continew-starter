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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import software.amazon.awssdk.services.s3.model.ListMultipartUploadsRequest;
import software.amazon.awssdk.services.s3.model.ListPartsRequest;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.strategy.impl.OssStorageStrategy;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

/**
 * OssStorageStrategy 分片上传真实 MinIO 集成测试。
 *
 * <p>分片语义（uploadId 状态、分片 ETag、合并结果）由真实 S3 服务端裁决；分片
 * 阈值在基座配置中压小，使同批用例同时覆盖内存缓冲与临时文件两条读入路径。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("OssStorageStrategy 分片上传真实 MinIO 集成测试")
class OssMultipartUploadIT extends MinioContainerSupport {

    private static final String BUCKET = "itest-oss-multipart";

    @BeforeAll
    static void setUpBucket() {
        createBucket(BUCKET);
    }

    @Test
    @DisplayName("三段分片（末片内存路径 + 前片临时文件路径）合并后内容与服务端对象一致，临时分片即时清理")
    void shouldCompleteMultipartUploadWithMixedPartPaths() throws IOException {
        String key = randomPrefix() + "assembled.bin";
        OssStorageStrategy oss = strategy();
        MultipartInitResp init = oss.initMultipartUpload(BUCKET, key, "application/octet-stream",
            null);
        assertEquals(BUCKET, init.getBucket());
        assertEquals(key, init.getPath());
        assertNotNull(init.getUploadId());

        // S3 协议要求除最后一片外每片不小于 5 MiB：前两片满足协议并超过内存阈值（4KB），
        // 走临时文件路径；末片不受限，走内存路径
        byte[] part1 = partData(5 * 1024 * 1024, (byte) 1);
        byte[] part2 = partData(5 * 1024 * 1024, (byte) 2);
        byte[] part3 = partData(1024, (byte) 3);
        MultipartUploadResp resp1 = oss.uploadPart(BUCKET, key, init.getUploadId(), 1,
            new ByteArrayInputStream(part1));
        MultipartUploadResp resp2 = oss.uploadPart(BUCKET, key, init.getUploadId(), 2,
            new ByteArrayInputStream(part2));
        MultipartUploadResp resp3 = oss.uploadPart(BUCKET, key, init.getUploadId(), 3,
            new ByteArrayInputStream(part3));
        assertTrue(resp1.isSuccess());
        assertTrue(resp2.isSuccess());
        assertTrue(resp3.isSuccess());

        // 策略返回的分片列表与真实服务端分片状态交叉核对
        List<MultipartUploadResp> listedParts = oss.listParts(BUCKET, key, init.getUploadId());
        assertEquals(3, listedParts.size());
        assertEquals(3, probe()
            .listParts(ListPartsRequest.builder()
                .bucket(BUCKET)
                .key(key)
                .uploadId(init.getUploadId())
                .build())
            .parts()
            .size());

        List<MultipartUploadResp> parts = List.of(resp1, resp2, resp3);
        oss.completeMultipartUpload(BUCKET, key, init.getUploadId(), parts, true);

        byte[] expected = new byte[part1.length + part2.length + part3.length];
        System.arraycopy(part1, 0, expected, 0, part1.length);
        System.arraycopy(part2, 0, expected, part1.length, part2.length);
        System.arraycopy(part3, 0, expected, part1.length + part2.length, part3.length);
        assertArrayEquals(expected, probeObjectAsBytes(BUCKET, key));
        try (InputStream in = oss.download(BUCKET, key)) {
            assertArrayEquals(expected, in.readAllBytes());
        }
        assertEquals(expected.length, headObject(BUCKET, key).contentLength().longValue());
        // 临时文件路径的分片在上传后立即清理，目录中不留临时残留
        try (var files = Files.list(multipartTempDir())) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }

    @Test
    @DisplayName("verifyParts 开启时记录 ETag 与服务端不一致则拒绝合并")
    void shouldRejectCompleteWhenRecordedEtagMismatch() {
        String key = randomPrefix() + "mismatch.bin";
        OssStorageStrategy oss = strategy();
        MultipartInitResp init = oss.initMultipartUpload(BUCKET, key, "application/octet-stream",
            null);
        MultipartUploadResp part = oss.uploadPart(BUCKET, key, init.getUploadId(), 1,
            new ByteArrayInputStream(partData(1024, (byte) 9)));
        assertTrue(part.isSuccess());

        MultipartUploadResp tampered = new MultipartUploadResp();
        tampered.setPartNumber(1);
        tampered.setPartETag("deadbeef-not-real");
        tampered.setSuccess(true);
        List<MultipartUploadResp> parts = List.of(tampered);
        String uploadId = init.getUploadId();
        assertThrows(StorageException.class, () -> oss.completeMultipartUpload(BUCKET, key,
            uploadId, parts, true));

        oss.abortMultipartUpload(BUCKET, key, init.getUploadId());
    }

    @Test
    @DisplayName("无效 uploadId 的分片上传按失败结果返回且不抛出（异常转结果语义）")
    void shouldReportFailedPartOnUnknownUploadId() {
        MultipartUploadResp resp = strategy()
            .uploadPart(BUCKET, randomPrefix() + "orphan.bin", "no-such-upload-id", 1,
                new ByteArrayInputStream(partData(1024, (byte) 0)));
        assertFalse(resp.isSuccess());
        assertNotNull(resp.getErrorMessage());
    }

    @Test
    @DisplayName("abort 后服务端分片上传状态被真实移除")
    void shouldRemoveServerUploadStateOnAbort() {
        String key = randomPrefix() + "aborted.bin";
        OssStorageStrategy oss = strategy();
        MultipartInitResp init = oss.initMultipartUpload(BUCKET, key, "application/octet-stream",
            null);
        MultipartUploadResp part = oss.uploadPart(BUCKET, key, init.getUploadId(), 1,
            new ByteArrayInputStream(partData(1024, (byte) 4)));
        assertTrue(part.isSuccess());

        oss.abortMultipartUpload(BUCKET, key, init.getUploadId());

        // 探针核对真实服务端：该 uploadId 不再出现在未完成分片上传列表中
        var uploads = probe()
            .listMultipartUploads(ListMultipartUploadsRequest.builder().bucket(BUCKET).build())
            .uploads();
        assertTrue(uploads.stream().noneMatch(upload -> init.getUploadId()
            .equals(upload.uploadId())));
    }

    /**
     * 生成指定大小的确定性分片数据。
     *
     * @param size 数据字节数
     * @param seed 填充种子
     * @return 分片数据
     */
    private static byte[] partData(int size, byte seed) {
        byte[] data = new byte[size];
        Arrays.fill(data, seed);
        return data;
    }
}
