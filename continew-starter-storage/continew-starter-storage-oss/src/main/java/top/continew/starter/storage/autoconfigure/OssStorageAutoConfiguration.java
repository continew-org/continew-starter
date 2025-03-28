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

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import top.continew.starter.storage.dao.StorageDao;
import top.continew.starter.storage.dao.impl.StorageDaoDefaultImpl;

/**
 * 对象存储自动配置
 *
 * @author echo
 * @since 2.9.0
 */
@AutoConfiguration
public class OssStorageAutoConfiguration {

    /**
     * 存储记录持久层默认实现
     */
    @Bean
    @ConditionalOnMissingBean
    public StorageDao storageDao() {
        return new StorageDaoDefaultImpl();
    }
}
