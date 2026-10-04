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

package top.continew.starter.data.util;

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import top.continew.starter.core.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

/**
 * 排序字段白名单校验测试。
 *
 * <p>覆盖两类场景：其一为 GHSA-g8qc-r85v-gqpp 描述的 ORDER BY 注入绕过形态——旧实现
 * 只校验最后一个点分段之后的字段名，攻击者可用「表达式 + {@code #} 注释」构造出同时
 * 骗过字段名校验与黑名单的 payload；其二为 java:S5998 报告的嵌套量词栈溢出——点分段
 * 输入在旧正则 {@code \w+(\.\w+)*} 上会逐段递归并耗尽线程栈。</p>
 *
 * <p>{@code ValidationUtils} 通过 {@code Validator} 的静态字段经 Hutool
 * {@code SpringUtil} 获取 {@code jakarta.validation.Validator}，无法脱离 Spring 上下文
 * 使用；此处以最小上下文注册校验器与 {@code SpringUtil}（与
 * {@code ApplicationAutoConfiguration} 相同的方式），而非启动完整应用。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
@SpringJUnitConfig(SortUtilsTest.ValidatorConfig.class)
class SortUtilsTest {

    /**
     * 仅为单元测试提供校验器实现与 Hutool Spring 上下文的最小配置。
     */
    @Configuration(proxyBeanMethods = false)
    @Import(SpringUtil.class)
    static class ValidatorConfig {

        /**
         * 排序字段校验走的是纯正则路径，不会调用校验器实例；此处仅提供占位 Bean，
         * 满足 {@code Validator} 静态初始化的依赖要求。
         *
         * @return 校验器占位实现
         */
        @Bean
        jakarta.validation.Validator jakartaValidator() {
            return mock(jakarta.validation.Validator.class);
        }
    }

    /**
     * 常规字段名与表别名应通过校验。
     */
    @Test
    void shouldAcceptPlainFieldName() {
        assertDoesNotThrow(() -> SortUtils.validateProperty("create_time"));
        assertDoesNotThrow(() -> SortUtils.validateProperty("id"));
        assertDoesNotThrow(() -> SortUtils.validateProperty("createTime"));
    }

    /**
     * 带表别名的字段名应通过校验（与旧正则语义保持一致）。
     */
    @Test
    void shouldAcceptTableAliasPrefixedField() {
        assertDoesNotThrow(() -> SortUtils.validateProperty("u.create_time"));
        assertDoesNotThrow(() -> SortUtils.validateProperty("t1.createTime"));
        assertDoesNotThrow(() -> SortUtils.validateProperty("a.b.c"));
    }

    /**
     * 大小写字母、数字与下划线混合的标识符应通过校验。
     */
    @Test
    void shouldAcceptMixedCaseIdentifier() {
        assertDoesNotThrow(() -> SortUtils.validateProperty("T1.Create_Time2"));
    }

    /**
     * GHSA-g8qc-r85v-gqpp 的核心 payload：仅校验最后一段字段名时，「表达式 + {@code #}
     * 注释」可同时骗过实体字段名校验与关键字黑名单，整串被原样拼入 ORDER BY。
     */
    @Test
    void shouldRejectExpressionWithHashCommentSuffix() {
        assertThrows(BadRequestException.class,
            () -> SortUtils.validateProperty("(select 1 from dual)#x.id"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id)#"));
        assertThrows(BadRequestException.class,
            () -> SortUtils.validateProperty("case when 1=1 then id end#t.id"));
    }

    /**
     * SQL 注释符、引号与语句终止符必须被拒绝。
     */
    @Test
    void shouldRejectSqlSyntaxCharacters() {
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id --"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id/*x*/"));
        assertThrows(BadRequestException.class,
            () -> SortUtils.validateProperty("id;drop table t"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id'"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id\""));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id`"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("*"));
    }

    /**
     * 含空白字符的字段名必须被拒绝。
     */
    @Test
    void shouldRejectWhitespace() {
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id asc"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty(" id"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("id\t"));
    }

    /**
     * 非 ASCII 字符不在 {@code \w}（Java 默认等价于 {@code [a-zA-Z0-9_]}）范围内，应被拒绝。
     */
    @Test
    void shouldRejectNonAsciiIdentifier() {
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("姓名"));
    }

    /**
     * 空值、空串与不完整的分段结构必须被拒绝。
     */
    @Test
    void shouldRejectEmptyAndMalformedSegments() {
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty(null));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty(""));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("a."));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty(".a"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("a..b"));
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty("."));
    }

    /**
     * 回归 java:S5998：点分段输入不得触发 StackOverflowError。
     *
     * <p>旧实现 {@code \w+(\.\w+)*} 在约 2000 段后即耗尽线程栈，此处用 5000 段
     * （约 10KB）覆盖真实攻击窗口；新实现为线性切分，耗时与输入长度成正比。</p>
     */
    @Test
    void shouldNotOverflowStackOnLongSegmentedInput() {
        assertDoesNotThrow(() -> SortUtils.validateProperty(this.buildSegmentedProperty(5000)));
    }

    /**
     * 超长点分段输入（2 万段，约 40KB）仍应稳定拒绝且不抛栈溢出。
     */
    @Test
    void shouldRejectLongSegmentedInputWithoutStackOverflow() {
        String property = this.buildSegmentedProperty(20_000) + "!";
        assertThrows(BadRequestException.class, () -> SortUtils.validateProperty(property));
    }

    /**
     * 构造 {@code a.a.a...a} 形式的点分字段。
     *
     * @param segments 段数
     * @return 点分字段字符串
     */
    private String buildSegmentedProperty(int segments) {
        StringBuilder builder = new StringBuilder(segments * 2);
        for (int i = 0; i < segments; i++) {
            if (i > 0) {
                builder.append('.');
            }
            builder.append('a');
        }
        return builder.toString();
    }
}
