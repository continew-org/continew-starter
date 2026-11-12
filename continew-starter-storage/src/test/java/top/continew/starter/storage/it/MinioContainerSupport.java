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

import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import top.continew.starter.storage.autoconfigure.properties.OssStorageConfig;
import top.continew.starter.storage.strategy.impl.OssStorageStrategy;
import top.continew.starter.test.support.ContainerImages;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * OSS 存储策略集成测试基座：整个 Failsafe JVM 共享一个真实 MinIO 容器。
 *
 * <p>{@code OssStorageStrategy} 构造成本高（内建 S3 客户端与预签名器），且桶、对象等
 * 服务端状态由各测试类以独立桶名隔离，因此容器与策略实例跨测试类共享，容器由 JVM
 * 退出时的 Ryuk 守护进程统一回收。断言一律通过独立的探针客户端直连真实 MinIO 核对
 * 服务端状态，不依赖被测策略自身返回值自证。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
abstract class MinioContainerSupport {

    /**
     * 分片上传临时目录：基座每次启动新建空目录，供「大分片落盘后即时清理」断言使用
     */
    private static final Path MULTIPART_TEMP_DIR = createMultipartTempDir();

    private static final MinIOContainer MINIO = new MinIOContainer(
        DockerImageName.parse(ContainerImages.MINIO_IMAGE));
    private static volatile OssStorageStrategy sharedStrategy;
    private static volatile S3Client probeClient;

    /**
     * 启动共享的真实 MinIO 容器，并建立被测策略与独立探针客户端。
     *
     * <p>策略与探针均启用 path-style 访问：随机映射端口的本地容器无法承载虚拟主机
     * 寻址，path-style 也是 MinIO 及私有化对象存储的主流接入方式。</p>
     */
    @BeforeAll
    static void startSharedMinio() {
        if (sharedStrategy != null) {
            return;
        }
        MINIO.start();
        probeClient = S3Client.builder()
            .endpointOverride(URI.create(MINIO.getS3URL()))
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(MINIO
                .getUserName(), MINIO.getPassword())))
            .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
            .build();
        sharedStrategy = new OssStorageStrategy(buildConfig());
    }

    /**
     * 组装被测策略的配置：分片阈值刻意压小，使单个测试即可同时覆盖分片上传的内存
     * 路径（不超内存阈值）与临时文件路径（超过内存阈值）。
     *
     * @return 被测策略配置
     */
    private static OssStorageConfig buildConfig() {
        OssStorageConfig config = new OssStorageConfig();
        config.setPlatform("oss-it");
        config.setEndpoint(MINIO.getS3URL());
        config.setAccessKey(MINIO.getUserName());
        config.setSecretKey(MINIO.getPassword());
        config.setBucketName("itest-oss-default");
        config.setPathStyleAccessEnabled(true);
        config.setMultipartUploadThreshold(8L * 1024);
        config.setMultipartUploadPartSize(4L * 1024);
        config.setMultipartTempDir(MULTIPART_TEMP_DIR.toString());
        return config;
    }

    /**
     * 获取被测的 OSS 存储策略。
     *
     * @return 连接共享容器的策略实例
     */
    static OssStorageStrategy strategy() {
        return sharedStrategy;
    }

    /**
     * 获取独立探针客户端：与被测策略的客户端相互独立，用于核对真实服务端状态。
     *
     * @return 直连共享容器的 S3 客户端
     */
    static S3Client probe() {
        return probeClient;
    }

    /**
     * 创建测试桶（已存在时忽略），各测试类用独立桶名互不干扰。
     *
     * @param bucket 桶名
     */
    static void createBucket(String bucket) {
        try {
            probeClient.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (S3Exception e) {
            // 桶已存在视为就绪（MinIO 对已拥有桶返回 409），其余服务端错误向上抛出使测试失败
            if (e.statusCode() != 409) {
                throw e;
            }
        }
    }

    /**
     * 探针读取指定对象的真实元数据（HeadObject）。
     *
     * @param bucket 桶名
     * @param key    对象键
     * @return 服务端元数据
     */
    static HeadObjectResponse headObject(String bucket, String key) {
        return probeClient.headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build());
    }

    /**
     * 探针读取指定对象的真实字节内容。
     *
     * @param bucket 桶名
     * @param key    对象键
     * @return 服务端对象内容
     */
    static byte[] probeObjectAsBytes(String bucket, String key) {
        return probeClient.getObjectAsBytes(GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()).asByteArray();
    }

    /**
     * 生成跨测试类防冲突的子前缀。
     *
     * @return 随机子前缀（以斜杠结尾）
     */
    static String randomPrefix() {
        return "p" + UUID.randomUUID().toString().replace("-", "") + "/";
    }

    /**
     * 获取本 JVM 专属的分片上传临时目录（基座启动时新建，天然无历史残留）。
     *
     * @return 临时目录
     */
    static Path multipartTempDir() {
        return MULTIPART_TEMP_DIR;
    }

    private static Path createMultipartTempDir() {
        try {
            return Files.createTempDirectory("storage-it-parts");
        } catch (IOException e) {
            throw new IllegalStateException("创建分片临时目录失败", e);
        }
    }
}
