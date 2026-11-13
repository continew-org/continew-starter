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
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * OssStorageStrategy 预签名 URL 真实 MinIO 集成测试。
 *
 * <p>预签名的服务端有效性（签名可接受、匿名被拒、过期错误码）只有真实 HTTP 请求
 * 才能证明：URL 以 JDK HttpClient 直接打到 MinIO，下载字节与探针核对内容一致。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@DisplayName("OssStorageStrategy 预签名 URL 真实 MinIO 集成测试")
class OssPresignedUrlIT extends MinioContainerSupport {

    private static final String BUCKET = "itest-oss-presign";
    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @BeforeAll
    static void setUpBucket() {
        createBucket(BUCKET);
    }

    @Test
    @DisplayName("GET 预签名 URL 可真实下载，内容与上传字节一致")
    void shouldDownloadRealObjectViaPresignedGetUrl() throws IOException, InterruptedException {
        String key = randomPrefix() + "presigned.txt";
        byte[] content = "presigned download".getBytes(StandardCharsets.UTF_8);
        strategy().upload(BUCKET, key, new MockMultipartFile("file", "presigned.txt",
            "text/plain", content));

        String url = strategy().generatePresignedUrl(BUCKET, key, 300);
        assertTrue(url.contains("X-Amz-Signature"), "预签名 URL 应携带签名参数");

        HttpResponse<byte[]> response = HTTP.send(HttpRequest
            .newBuilder(URI.create(url))
            .GET()
            .build(), HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, response.statusCode());
        assertArrayEquals(content, response.body());
    }

    @Test
    @DisplayName("剥离签名参数后匿名访问被真实服务端以 403 拒绝")
    void shouldRejectAnonymousAccessWithoutSignature() throws IOException, InterruptedException {
        String key = randomPrefix() + "private.txt";
        strategy().upload(BUCKET, key, new MockMultipartFile("file", "private.txt", "text/plain",
            "private".getBytes(StandardCharsets.UTF_8)));

        String url = strategy().generatePresignedUrl(BUCKET, key, 300);
        String unsignedUrl = url.substring(0, url.indexOf('?'));

        HttpResponse<Void> response = HTTP.send(HttpRequest
            .newBuilder(URI.create(unsignedUrl))
            .GET()
            .build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(403, response.statusCode());
    }

    @Test
    @DisplayName("PUT 预签名 URL 可真实上传，对象经探针核对存在且内容一致")
    void shouldUploadRealObjectViaPresignedPutUrl() throws IOException, InterruptedException {
        String key = randomPrefix() + "uploaded-by-presign.txt";
        byte[] content = "presigned upload".getBytes(StandardCharsets.UTF_8);

        String url = strategy().generateUploadPresignedUrl(BUCKET, key, 300);
        HttpResponse<Void> response = HTTP.send(HttpRequest
            .newBuilder(URI.create(url))
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content))
            .build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(200, response.statusCode());

        assertArrayEquals(content, probeObjectAsBytes(BUCKET, key));
    }

    @Test
    @DisplayName("指向不存在桶的预签名 URL 生成成功，真实请求由服务端返回 404")
    void shouldSurfaceRealNotFoundForMissingBucket() throws IOException, InterruptedException {
        String url = strategy().generatePresignedUrl("itest-oss-no-such-presign", randomPrefix()
            + "ghost.txt", 300);

        HttpResponse<Void> response = HTTP.send(HttpRequest
            .newBuilder(URI.create(url))
            .GET()
            .build(), HttpResponse.BodyHandlers.discarding());
        assertEquals(404, response.statusCode());
    }
}
