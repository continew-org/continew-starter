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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * 限流器 Key 过期配置属性默认值测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class RateLimiterExpirePropertiesTest {

    /**
     * 默认关闭过期，行为与历史版本一致
     */
    @Test
    void shouldKeepExpireDisabledByDefault() {
        RateLimiterExpireProperties properties = new RateLimiterExpireProperties();
        assertFalse(properties.isEnabled());
        assertEquals(1L, properties.getTime());
        assertEquals(TimeUnit.HOURS, properties.getUnit());
    }
}
