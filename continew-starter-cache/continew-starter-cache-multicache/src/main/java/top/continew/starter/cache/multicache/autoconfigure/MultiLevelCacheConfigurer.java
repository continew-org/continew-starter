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

package top.continew.starter.cache.multicache.autoconfigure;

import java.util.List;

/**
 * 多级缓存自定义配置
 *
 * <p>
 * 业务方实现此接口并注册为 Bean，可预声明缓存名（启动即建好缓存，
 * 避免首次访问的懒创建竞态，也让 specs 配置有校验对象）。
 * </p>
 *
 * @author KAI
 * @since 3.0.0
 */
public interface MultiLevelCacheConfigurer {

    /**
     * 预声明的缓存名列表（返回 null 表示完全懒创建）
     *
     * @return 预声明的缓存名列表
     */
    default List<String> getCacheNames() {
        return null;
    }
}
