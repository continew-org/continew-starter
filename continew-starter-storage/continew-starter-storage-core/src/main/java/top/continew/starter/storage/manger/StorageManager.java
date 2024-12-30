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

package top.continew.starter.storage.manger;

import top.continew.starter.cache.redisson.util.RedisUtils;
import top.continew.starter.core.validation.ValidationUtils;
import top.continew.starter.storage.constant.StorageConstant;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储策略管理器
 *
 * @author echo
 * @date 2024/12/16
 */
public class StorageManager {

    /**
     * 存储策略连接信息
     */
    private static final Map<String, StorageStrategy<?>> STORAGE_STRATEGY = new ConcurrentHashMap<>();

    /**
     * 加载存储策略
     *
     * @param code     存储码
     * @param strategy 对应存储策略
     */
    public static void load(String code, StorageStrategy<?> strategy) {
        STORAGE_STRATEGY.put(code, strategy);
    }

    /**
     * 卸载存储策略
     *
     * @param code 存储码
     */
    public static void unload(String code) {
        STORAGE_STRATEGY.remove(code);
    }

    /**
     * 根据 存储 code 获取对应存储策略
     *
     * @param code 代码
     * @return {@link StorageStrategy }
     */
    public static StorageStrategy<?> instance(String code) {
        StorageStrategy<?> strategy = STORAGE_STRATEGY.get(code);
        ValidationUtils.throwIfEmpty(strategy, "未找到存储配置:" + code);
        return strategy;
    }

    /**
     * 获取默认存储策略
     *
     * @return {@link StorageStrategy }
     */
    public static StorageStrategy<?> instance() {
        return instance(RedisUtils.get(StorageConstant.DEFAULT_KEY));
    }

}