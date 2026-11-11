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

package top.continew.starter.ratelimiter.generator;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * 默认限流器名称生成器测试
 *
 * <p>锁定名称格式：包路径压缩为各段首字母（Hutool {@code ClassUtil.getShortClassName}
 * 语义）、参数按 JVM 描述符字符展开（long 为 J）、数组逐维前置「[」，以及同一
 * {@link Method} 的名称缓存复用。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DefaultRateLimiterNameGeneratorTest {

    private final DefaultRateLimiterNameGenerator generator = new DefaultRateLimiterNameGenerator();

    /**
     * 无参方法应生成「短类名.方法名()」形式。
     */
    @Test
    void shouldGenerateQualifiedNameForNoArgMethod() throws Exception {
        Method method = this.methodOf("noArgs");
        assertThat(this.generator.generate(this, method))
            .isEqualTo("t.c.s.r.g.DefaultRateLimiterNameGeneratorTest.noArgs()");
    }

    /**
     * 八种基本类型参数应按 JVM 描述符字符生成（long 为 J，其余取首字母）。
     */
    @Test
    void shouldGeneratePrimitiveTypeDescriptors() throws Exception {
        Method method = this.methodOf("withPrimitives", int.class, long.class, boolean.class,
            char.class, short.class, double.class, float.class, byte.class);
        assertThat(this.generator.generate(this, method))
            .isEqualTo("t.c.s.r.g.DefaultRateLimiterNameGeneratorTest.withPrimitives(IJZCSDFB)");
    }

    /**
     * 对象类型参数应生成「L 短类名 ;」形式。
     */
    @Test
    void shouldGenerateObjectTypeDescriptors() throws Exception {
        Method method = this.methodOf("withObjects", String.class, Integer.class);
        assertThat(this.generator.generate(this, method))
            .isEqualTo(
                "t.c.s.r.g.DefaultRateLimiterNameGeneratorTest.withObjects(Lj.l.String;Lj.l.Integer;)");
    }

    /**
     * 一维数组参数应对元素类型递归前置「[」。
     */
    @Test
    void shouldGenerateArrayDescriptors() throws Exception {
        Method method = this.methodOf("withArrays", String[].class, int[].class);
        assertThat(this.generator.generate(this, method))
            .isEqualTo("t.c.s.r.g.DefaultRateLimiterNameGeneratorTest.withArrays([Lj.l.String;[I)");
    }

    /**
     * 嵌套数组参数应逐维叠加「[」。
     */
    @Test
    void shouldGenerateNestedArrayDescriptor() throws Exception {
        Method method = this.methodOf("withNestedArray", int[][].class);
        assertThat(this.generator.generate(this, method))
            .isEqualTo("t.c.s.r.g.DefaultRateLimiterNameGeneratorTest.withNestedArray([[I)");
    }

    /**
     * 同一方法的重复生成应命中缓存，返回同一实例。
     */
    @Test
    void shouldReuseCachedNameForSameMethod() throws Exception {
        Method method = this.methodOf("noArgs");
        String first = this.generator.generate(this, method);
        String second = this.generator.generate(this, method);
        assertThat(second).isSameAs(first);
    }

    /**
     * 反射获取本类指定方法。
     *
     * @param name          方法名
     * @param parameterTypes 参数类型
     * @return 方法对象
     */
    private Method methodOf(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return DefaultRateLimiterNameGeneratorTest.class.getDeclaredMethod(name, parameterTypes);
    }

    private void noArgs() {
        // 仅承载方法签名，供反射生成名称
    }

    private void withPrimitives(int i, long l, boolean b, char c, short s, double d, float f,
        byte y) {
        // 仅承载方法签名，供反射生成名称
    }

    private void withObjects(String name, Integer count) {
        // 仅承载方法签名，供反射生成名称
    }

    private void withArrays(String[] names, int[] codes) {
        // 仅承载方法签名，供反射生成名称
    }

    private void withNestedArray(int[][] matrix) {
        // 仅承载方法签名，供反射生成名称
    }
}
