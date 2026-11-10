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

package top.continew.starter.core.util.expression;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

/**
 * 表达式解析工具类测试
 *
 * <p>
 * 表达式统一经 {@code #root.args[...]} 与 {@code #root.target} 访问上下文，
 * 避免依赖编译器 {@code -parameters} 开关的参数名发现能力。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExpressionUtilsTest {

    /**
     * 构造测试方法 add 的反射引用
     *
     * @return add(int, int) 方法
     */
    private static Method addMethod() throws NoSuchMethodException {
        return SampleService.class.getMethod("add", int.class, int.class);
    }

    @Test
    @DisplayName("eval 对空脚本与 null 脚本返回 null")
    void shouldReturnNullForBlankScript() throws NoSuchMethodException {
        Method method = addMethod();
        assertThat(ExpressionUtils.eval(null, new SampleService(), method, 1, 2)).isNull();
        assertThat(ExpressionUtils.eval(" ", new SampleService(), method, 1, 2)).isNull();
    }

    @Test
    @DisplayName("eval 按方法实参求值算术表达式")
    void shouldEvalArithmeticExpression() throws NoSuchMethodException {
        Object result = ExpressionUtils.eval("#root.args[0] + #root.args[1]", new SampleService(),
            addMethod(), 2, 3);
        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("eval 可通过 #root.target 访问目标对象属性")
    void shouldAccessTargetProperty() throws NoSuchMethodException {
        Object result = ExpressionUtils
            .eval("#root.target.prefix + #root.args[0]", new SampleService(), addMethod(), 10, 20);
        assertThat(result).isEqualTo("id-10");
    }

    @Test
    @DisplayName("eval 支持按方法参数名引用实参")
    void shouldEvalByParameterNames() throws NoSuchMethodException {
        Object result = ExpressionUtils.eval("#a + #b", new SampleService(), addMethod(), 2, 3);
        assertThat(result).isEqualTo(5);
    }

    @Test
    @DisplayName("eval 对非法表达式吞掉异常返回 null")
    void shouldReturnNullForInvalidExpression() throws NoSuchMethodException {
        assertThat(ExpressionUtils.eval("#root.args[0] +", new SampleService(), addMethod(), 1, 2))
            .isNull();
    }

    @Test
    @DisplayName("eval 对求值失败的表达式吞掉异常返回 null")
    void shouldReturnNullWhenEvaluationFails() throws NoSuchMethodException {
        // 目标对象不存在 nonExist 属性，SpEL 求值阶段抛 SpelEvaluationException
        assertThat(
            ExpressionUtils.eval("#root.target.nonExist", new SampleService(), addMethod(), 1, 2))
            .isNull();
    }

    @Test
    @DisplayName("ExpressionEvaluator 直接 apply 与 ExpressionUtils.eval 结果一致")
    void shouldApplyEvaluatorDirectly() throws NoSuchMethodException {
        Method method = addMethod();
        ExpressionEvaluator evaluator =
            new ExpressionEvaluator("#root.args[0] + #root.args[1]", method);
        ExpressionInvokeContext context =
            new ExpressionInvokeContext(method, new Object[] {2, 3}, new SampleService());
        assertThat(evaluator.apply(context)).isEqualTo(5);
    }

    @Test
    @DisplayName("ExpressionInvokeContext 读写 method、args 与 target")
    void shouldAccessInvokeContextFields() throws NoSuchMethodException {
        Method method = addMethod();
        Object[] args = {1, 2};
        Object target = new SampleService();
        ExpressionInvokeContext context = new ExpressionInvokeContext(method, args, target);
        assertThat(context.getMethod()).isSameAs(method);
        assertThat(context.getArgs()).isSameAs(args);
        assertThat(context.getTarget()).isSameAs(target);
        context.setMethod(null);
        context.setArgs(null);
        context.setTarget(null);
        assertThat(context.getMethod()).isNull();
        assertThat(context.getArgs()).isNull();
        assertThat(context.getTarget()).isNull();
    }

    /**
     * 表达式求值样例服务
     */
    private static class SampleService {

        public String getPrefix() {
            return "id-";
        }

        public int add(int a, int b) {
            return a + b;
        }
    }
}
