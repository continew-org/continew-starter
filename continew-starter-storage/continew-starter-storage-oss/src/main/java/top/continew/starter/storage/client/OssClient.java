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

package top.continew.starter.storage.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.crt.S3CrtHttpConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.storage.model.req.StorageProperties;
import top.continew.starter.storage.util.OssUtils;
import top.continew.starter.storage.util.StorageUtils;

import java.net.URI;
import java.time.Duration;

/**
 * S3客户端
 *
 * @author echo
 * @since 2.9.0
 */
public class OssClient {

    private static final Logger log = LoggerFactory.getLogger(OssClient.class);

    /**
     * 配置属性
     */
    private final StorageProperties properties;

    /**
     * s3 异步客户端
     */
    private final S3AsyncClient client;

    /**
     * S3 数据传输的高级工具
     */
    private final S3TransferManager transferManager;

    /**
     * S3 预签名
     */
    private final S3Presigner presigner;

    /**
     * 获取属性
     *
     * @return {@link StorageProperties }
     */
    public StorageProperties getProperties() {
        return properties;
    }

    /**
     * 构造方法
     *
     * @param s3PropertiesReq 微型性能要求
     */
    public OssClient(StorageProperties s3PropertiesReq) {
        this.properties = s3PropertiesReq;

        // 创建认证信息
        StaticCredentialsProvider auth = StaticCredentialsProvider.create(AwsBasicCredentials.create(properties
            .getAccessKey(), properties.getSecretKey()));

        URI uriWithProtocol = StorageUtils.createUriWithProtocol(properties.getEndpoint());

        // 创建 客户端连接
        client = S3AsyncClient.crtBuilder()
            .credentialsProvider(auth) // 认证信息
            .endpointOverride(uriWithProtocol) // 连接端点
            .region(OssUtils.getRegion(properties.getRegion()))
            .targetThroughputInGbps(20.0) //吞吐量
            .minimumPartSizeInBytes(10 * 1025 * 1024L)
            .checksumValidationEnabled(false)
            .httpConfiguration(S3CrtHttpConfiguration.builder()
                .connectionTimeout(Duration.ofSeconds(60)) // 设置连接超时
                .build())
            .build();

        // 基于 CRT 创建 S3 Transfer Manager 的实例
        this.transferManager = S3TransferManager.builder().s3Client(this.client).build();

        this.presigner = S3Presigner.builder()
            .region(OssUtils.getRegion(properties.getRegion()))
            .credentialsProvider(auth)
            .endpointOverride(uriWithProtocol)
            .build();

        // 只创建 默认存储的的桶
        if (s3PropertiesReq.getIsDefault()) {
            try {
                // 检查存储桶是否存在
                client.headBucket(HeadBucketRequest.builder().bucket(properties.getBucketName()).build());
                log.info("默认存储-存储桶 {} 已存在", properties.getBucketName());
            } catch (NoSuchBucketException e) {
                log.info("默认存储桶 {} 不存在，尝试创建...", properties.getBucketName());
                try {
                    // 创建存储桶
                    client.createBucket(CreateBucketRequest.builder().bucket(properties.getBucketName()).build());
                    log.info("默认存储-存储桶 {} 创建成功", properties.getBucketName());
                } catch (Exception createException) {
                    log.error("创建默认存储-存储桶 {} 失败", properties.getBucketName(), createException);
                    throw new BusinessException("创建默认存储-桶出错", createException);
                }
            } catch (Exception e) {
                log.error("检查默认存储-存储桶 {} 时出错", properties.getBucketName(), e);
                throw new BusinessException("检查默认存储-桶时出错", e);
            }
        }
        log.info("加载 S3 存储 => {}", properties.getCode());
    }

    /**
     * 获得客户端
     *
     * @return {@link S3TransferManager }
     */
    public S3AsyncClient getClient() {
        return client;
    }

    /**
     * 获得 高效连接客户端 主要用于 上传下载 复制 删除
     *
     * @return {@link S3TransferManager }
     */
    public S3TransferManager getTransferManager() {
        return transferManager;
    }

    /**
     * 获得 S3 预签名
     *
     * @return {@link S3Presigner }
     */
    public S3Presigner getPresigner() {
        return presigner;
    }
}
