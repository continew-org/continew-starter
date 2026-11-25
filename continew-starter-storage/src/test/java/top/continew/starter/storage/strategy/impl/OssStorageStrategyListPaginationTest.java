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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;
import top.continew.starter.storage.autoconfigure.properties.OssStorageConfig;
import top.continew.starter.storage.domain.model.resp.FileInfo;

import java.time.Instant;
import java.util.List;

/**
 * OssStorageStrategy 列目录翻页行为单元测试。
 *
 * <p>真实 MinIO 无法在少量对象下稳定返回截断页，翻页聚合逻辑以 mock
 * {@link S3Client} 裁决，覆盖续传令牌推进与 maxKeys 截断。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("OssStorageStrategy 列目录翻页单元测试")
class OssStorageStrategyListPaginationTest {

    private static OssStorageStrategy newStrategy(S3Client client) {
        OssStorageConfig config = new OssStorageConfig();
        config.setPlatform("oss-test");
        config.setEndpoint("http://localhost:9000");
        config.setAccessKey("a");
        config.setSecretKey("b");
        config.setBucketName("bucket");
        return new OssStorageStrategy(config, client);
    }

    private static S3Object obj(String key) {
        return S3Object.builder().key(key).size(1L).lastModified(Instant.EPOCH).build();
    }

    @Test
    @DisplayName("截断页按 continuationToken 续传聚合，直至未截断")
    void shouldPaginateUntilNotTruncated() {
        S3Client client = mock(S3Client.class);
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(ListObjectsV2Response.builder()
                .isTruncated(true)
                .nextContinuationToken("token-2")
                .contents(obj("a/1.txt"), obj("a/2.txt"))
                .build())
            .thenReturn(ListObjectsV2Response.builder()
                .isTruncated(false)
                .contents(obj("a/3.txt"))
                .build());

        List<FileInfo> result = newStrategy(client).list("bucket", "a", 10);

        assertEquals(3, result.size());
        assertEquals(List.of("a/1.txt", "a/2.txt", "a/3.txt"),
            result.stream().map(FileInfo::getPath).toList());
        verify(client, times(2)).listObjectsV2(any(ListObjectsV2Request.class));
        verify(client).listObjectsV2(ListObjectsV2Request.builder()
            .bucket("bucket")
            .prefix("a")
            .maxKeys(8)
            .continuationToken("token-2")
            .build());
    }

    @Test
    @DisplayName("单页未截断时不再翻页")
    void shouldStopOnNonTruncatedFirstPage() {
        S3Client client = mock(S3Client.class);
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(ListObjectsV2Response.builder()
                .isTruncated(false)
                .contents(obj("a/1.txt"))
                .build());

        List<FileInfo> result = newStrategy(client).list("bucket", "a", 10);

        assertEquals(1, result.size());
        verify(client, times(1)).listObjectsV2(any(ListObjectsV2Request.class));
    }

    @Test
    @DisplayName("聚合超过 maxKeys 时截断返回前 maxKeys 条")
    void shouldTruncateAggregatedResultAtMaxKeys() {
        S3Client client = mock(S3Client.class);
        when(client.listObjectsV2(any(ListObjectsV2Request.class)))
            .thenReturn(ListObjectsV2Response.builder()
                .isTruncated(false)
                .contents(obj("a/1.txt"), obj("a/2.txt"), obj("a/3.txt"))
                .build());

        List<FileInfo> result = newStrategy(client).list("bucket", "a", 2);

        assertEquals(2, result.size());
    }
}
