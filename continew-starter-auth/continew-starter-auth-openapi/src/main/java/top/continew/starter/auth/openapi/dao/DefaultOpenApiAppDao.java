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

import top.continew.starter.auth.openapi.model.OpenApiApp;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 开放 API 应用 DAO - 使用内存
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class DefaultOpenApiAppDao implements OpenApiAppDao {

    private final Map<String, OpenApiApp> appMap = new ConcurrentHashMap<>();
    private final Map<String, Long> nonceMap = new ConcurrentHashMap<>();

    @Override
    public OpenApiApp getByAppId(String appId) {
        return appMap.get(appId);
    }

    @Override
    public boolean isNonceUsed(String nonce, String appId, long timeout) {
        String key = buildNonceKey(nonce, appId);
        Long timestamp = nonceMap.get(key);
        if (timestamp == null) {
            return false;
        }
        if (System.currentTimeMillis() - timestamp > timeout) {
            nonceMap.remove(key);
            return false;
        }
        return true;
    }

    @Override
    public void recordNonce(String nonce, String appId, long timeout) {
        String key = buildNonceKey(nonce, appId);
        nonceMap.put(key, System.currentTimeMillis());
    }

    private String buildNonceKey(String nonce, String appId) {
        return appId + ":" + nonce;
    }
}
