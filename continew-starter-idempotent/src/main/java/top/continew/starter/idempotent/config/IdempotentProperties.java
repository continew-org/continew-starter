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

package top.continew.starter.idempotent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 属性配置类
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 19:27
 * @Package top.continew.starter.idempotent.config.IdempotentProperties
 */
@ConfigurationProperties(prefix = "idempotent")
public class IdempotentProperties {

    // 内存实现清理过期 key 的间隔（毫秒）默认5分钟
    private long cleanInterval = 300000;

    // redis实现过期 key 的间隔（毫秒）默认5分钟
    private long redisTimeout = 300000;

    public long getCleanInterval() {
        return cleanInterval;
    }

    public void setCleanInterval(long cleanInterval) {
        this.cleanInterval = cleanInterval;
    }

    public long getRedisTimeout() {
        return redisTimeout;
    }

    public void setRedisTimeout(long redisTimeout) {
        this.redisTimeout = redisTimeout;
    }
}
