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
import top.continew.starter.storage.model.req.StorageProperties;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 本地客户端
 *
 * @author echo
 * @date 2024/12/16 19:37
 */
public class LocalClient {
    private static final Logger log = LoggerFactory.getLogger(LocalClient.class);

    /**
     * 配置属性
     */
    private final StorageProperties properties;

    /**
     * 构造函数
     *
     * @param properties 配置属性
     */
    public LocalClient(StorageProperties properties) {
        this.properties = properties;
        // 判断是否是默认存储，若不存在桶目录，则创建
        if (Boolean.TRUE.equals(properties.getIsDefault())) {
            String bucketName = properties.getBucketName();
            if (bucketName != null && !bucketName.isEmpty()) {
                createBucketDirectory(bucketName);
            } else {
                log.info("默认存储-存储桶已存在 => {}", bucketName);
            }
        }
        log.info("加载 Local 存储 => {}", properties.getCode());
    }

    /**
     * 获取属性
     *
     * @return {@link StorageProperties }
     */
    public StorageProperties getProperties() {
        return properties;
    }

    /**
     * 创建桶目录
     *
     * @param bucketName 桶名称
     */
    private void createBucketDirectory(String bucketName) {
        Path bucketPath = Path.of(bucketName);
        try {
            if (Files.notExists(bucketPath)) {
                Files.createDirectories(bucketPath);
                log.info("默认存储-存储桶创建成功 : {}", bucketPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("创建默认存储-存储桶失败 => 路径: {}", bucketPath.toAbsolutePath(), e);
        }
    }
}
