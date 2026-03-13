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

package top.continew.starter.storage.autoconfigure;

import cn.hutool.core.util.StrUtil;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.storage.autoconfigure.properties.LocalStorageConfig;
import top.continew.starter.storage.autoconfigure.properties.StorageProperties;
import top.continew.starter.storage.engine.StorageStrategyRegistrar;
import top.continew.starter.storage.strategy.StorageStrategy;
import top.continew.starter.storage.strategy.impl.LocalStorageStrategy;

import java.util.List;

/**
 * 本地存储自动配置
 *
 * @author echo
 * @since 2.14.0
 */
@ConditionalOnProperty(prefix = PropertiesConstants.STORAGE, name = "local")
public class LocalStorageAutoConfiguration implements StorageStrategyRegistrar {

    private final StorageProperties storageProperties;

    public LocalStorageAutoConfiguration(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 注册本地存储策略
     *
     * @param strategies 策略列表
     */
    @Bean
    @Override
    public void register(List<StorageStrategy> strategies) {
        for (LocalStorageConfig config : storageProperties.getLocal()) {
            if (config.isEnabled()) {
                if (config.getMultipartUploadThreshold() == null || config.getMultipartUploadThreshold() <= 0) {
                    config.setMultipartUploadThreshold(storageProperties.getMultipartUploadThreshold());
                }
                if (config.getMultipartUploadPartSize() == null || config.getMultipartUploadPartSize() <= 0) {
                    config.setMultipartUploadPartSize(storageProperties.getMultipartUploadPartSize());
                }
                if (StrUtil.isBlank(config.getMultipartTempDir())) {
                    config.setMultipartTempDir(storageProperties.getLocalMultipartTempDir());
                }
                strategies.add(new LocalStorageStrategy(config));
            }
        }
    }
}
