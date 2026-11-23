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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import top.continew.starter.storage.autoconfigure.properties.OssStorageConfig;
import top.continew.starter.storage.common.constant.StorageConstant;
import top.continew.starter.storage.common.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * OssStorageStrategy 流式上传上限单元测试。
 *
 * <p>AWS 单次 PUT Object 硬限 5 GiB，已知长度超限须在调用 SDK 与消费流之前拒绝，
 * 用 mock {@link S3Client} 与计数流裁决，无需真实大文件。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("OssStorageStrategy 流式上传上限单元测试")
class OssStorageStrategyStreamLimitTest {

    private static OssStorageStrategy newStrategy(S3Client client) {
        OssStorageConfig config = new OssStorageConfig();
        config.setPlatform("oss-test");
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("a");
        config.setSecretKey("b");
        config.setBucketName("bucket");
        return new OssStorageStrategy(config, client);
    }

    @Test
    @DisplayName("已知长度超过单次 PUT 上限时拒绝且不触 SDK、不读流")
    void shouldFailFastWhenKnownLengthExceedsPutLimit() {
        S3Client client = mock(S3Client.class);
        OssStorageStrategy strategy = newStrategy(client);
        TrackingStream stream = new TrackingStream();

        StorageException e = assertThrows(StorageException.class,
            () -> strategy.uploadStream("bucket", "big.bin", stream,
                "application/octet-stream", StorageConstant.S3_SINGLE_PUT_MAX_SIZE + 1));
        assertTrue(e.getMessage().contains("分片上传"));
        assertEquals(0, stream.readCount, "超限拒绝不得读取源流");
        verify(client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("已知长度等于单次 PUT 上限时正常走单次 PUT")
    void shouldPutObjectWhenLengthAtPutLimit() {
        S3Client client = mock(S3Client.class);
        OssStorageStrategy strategy = newStrategy(client);
        ByteArrayInputStream stream = new ByteArrayInputStream(new byte[0]);

        strategy.uploadStream("bucket", "ok.bin", stream, "application/octet-stream",
            StorageConstant.S3_SINGLE_PUT_MAX_SIZE);

        verify(client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    /**
     * 记录读取次数的输入流
     */
    private static class TrackingStream extends InputStream {

        private int readCount;

        @Override
        public int read() throws IOException {
            readCount++;
            return -1;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            readCount++;
            return -1;
        }
    }
}
