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

package top.continew.starter.auth.openapi.dao;

import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import top.continew.starter.auth.openapi.autoconfigure.OpenApiProperties;
import top.continew.starter.auth.openapi.autoconfigure.dao.OpenApiAppDaoProperties;
import top.continew.starter.auth.openapi.model.OpenApiApp;
import top.continew.starter.cache.redisson.util.RedisUtils;

import java.time.Duration;

/**
 * 开放 API 应用 DAO - 使用 Redis
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class RedisOpenApiAppDao implements OpenApiAppDao {

    private final OpenApiAppDaoProperties appDaoProperties;

    public RedisOpenApiAppDao(OpenApiAppDaoProperties appDaoProperties) {
        this.appDaoProperties = appDaoProperties;
    }

    @Override
    public OpenApiApp getByAppId(String appId) {
        return RedisUtils.get(RedisUtils.formatKey(appDaoProperties.getPrefix(), "APP", appId));
    }

    @Override
    public boolean isNonceUsed(String nonce, String appId, long timeout) {
        return RedisUtils.exists(RedisUtils.formatKey(appDaoProperties.getPrefix(), "NONCE", appId, nonce));
    }

    @Override
    public void recordNonce(String nonce, String appId, long timeout) {
        RedisUtils.set(RedisUtils.formatKey(appDaoProperties.getPrefix(), "NONCE", appId, nonce), "1", Duration.ofMillis(timeout));
    }
}
