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

package top.continew.starter.ratelimiter.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解 Key 过期参数默认值测试
 *
 * @author Charles7c
 * @since 3.0.0
 */
class RateLimiterAnnotationDefaultsTest {

    /**
     * 未显式配置时过期参数取约定默认值，保证行为与历史版本一致
     */
    @Test
    void shouldKeepExpireDefaults() throws NoSuchMethodException {
        RateLimiter annotation = this.annotationOf("legacyUsage");
        assertEquals(-1L, annotation.expire());
        assertEquals(TimeUnit.HOURS, annotation.expireUnit());
    }

    /**
     * 历史用法仅声明 name 即可编译使用，不因新增参数而破坏调用方
     */
    @Test
    void shouldRemainUsableWithOnlyNameDeclared() throws NoSuchMethodException {
        RateLimiter annotation = this.annotationOf("legacyUsage");
        assertEquals("legacy", annotation.name());
        assertEquals(TimeUnit.MILLISECONDS, annotation.unit());
        assertEquals(0, annotation.interval());
    }

    private RateLimiter annotationOf(String name) throws NoSuchMethodException {
        Method method = RateLimiterAnnotationDefaultsTest.class.getDeclaredMethod(name);
        return method.getAnnotation(RateLimiter.class);
    }

    @RateLimiter(name = "legacy")
    private void legacyUsage() {
    }
}
