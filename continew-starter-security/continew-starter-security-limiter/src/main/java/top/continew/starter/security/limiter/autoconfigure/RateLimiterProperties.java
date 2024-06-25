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

package top.continew.starter.security.limiter.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * 限流器配置属性
 * 
 * @author KAI
 * @since 2.2.0
 */
@ConfigurationProperties(prefix = "continew-starter.security.limiter")
public class RateLimiterProperties {
    private boolean enabled = false;

    private String limiterKey = "RateLimiter:";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLimiterKey() {
        return limiterKey;
    }

    public void setLimiterKey(String limiterKey) {
        //不为空且不以":"结尾，则添加":"
        if (StringUtils.hasText(limiterKey)) {
            if (!limiterKey.endsWith(":")) {
                limiterKey = limiterKey + ":";
            }
        }
        this.limiterKey = limiterKey;
    }

}
