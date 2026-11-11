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

package top.continew.starter.idempotent.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * {@link DefaultIdempotentNameGenerator} 默认幂等名称生成器测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DefaultIdempotentNameGeneratorTest {

    private final DefaultIdempotentNameGenerator generator = new DefaultIdempotentNameGenerator();

    /**
     * 参数哈希段固定为 32 位小写十六进制（MD5），用于弱化断言而不复述生成算法。
     */
    private static final Pattern ARG_HASH_PATTERN = Pattern.compile("^.+:[A-Za-z]+:[0-9a-f]{32}$");

    private Method method(String name) throws NoSuchMethodException {
        return SampleService.class.getMethod(name, String.class);
    }

    @Test
    void shouldContainClassAndMethodWhenArgsNull() throws NoSuchMethodException {
        String name = generator.generate(new SampleService(), method("create"), null);
        assertThat(name).isEqualTo(SampleService.class.getName() + ":create");
    }

    @Test
    void shouldContainClassAndMethodWhenNoArgs() throws NoSuchMethodException {
        String name = generator.generate(new SampleService(), method("create"));
        assertThat(name).isEqualTo(SampleService.class.getName() + ":create");
    }

    @Test
    void shouldAppendArgHashWhenArgsPresent() throws NoSuchMethodException {
        String name = generator.generate(new SampleService(), method("create"), "A123");
        assertThat(name).matches(ARG_HASH_PATTERN);
    }

    @Test
    void shouldBeStableForSameArgs() throws NoSuchMethodException {
        String first = generator.generate(new SampleService(), method("create"), "A123");
        String second = generator.generate(new SampleService(), method("create"), "A123");
        assertThat(second).isEqualTo(first);
    }

    @Test
    void shouldDifferForDifferentArgs() throws NoSuchMethodException {
        String first = generator.generate(new SampleService(), method("create"), "A123");
        String second = generator.generate(new SampleService(), method("create"), "B456");
        assertThat(second).isNotEqualTo(first);
    }

    /**
     * 生成器反射的目标样例类。
     */
    static class SampleService {

        public void create(String orderNo) {
            // 生成器只反射方法签名，方法体不会执行
        }
    }
}
