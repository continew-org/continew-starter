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

package top.continew.starter.ratelimiter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;

import java.util.concurrent.TimeUnit;

/**
 * 限流器 Key 过期配置属性
 * <p>
 * 延迟过期语义：限流器每次被访问时刷新 Key 的 TTL，活跃限流器不失效，空闲超过过期时长后 Key 到期删除。
 *
 * @author Charles7c
 * @since 3.0.0
 */
@ConfigurationProperties(PropertiesConstants.RATE_LIMITER_EXPIRE)
public class RateLimiterExpireProperties {

    /**
     * 是否启用 Key 过期（默认关闭，保持与历史版本行为一致）
     */
    private boolean enabled = false;

    /**
     * Key 过期时长（小于速率间隔时按速率间隔兜底，避免限流被提前重置）
     */
    private long time = 1;

    /**
     * Key 过期时长单位（默认：小时）
     */
    private TimeUnit unit = TimeUnit.HOURS;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public TimeUnit getUnit() {
        return unit;
    }

    public void setUnit(TimeUnit unit) {
        this.unit = unit;
    }
}
