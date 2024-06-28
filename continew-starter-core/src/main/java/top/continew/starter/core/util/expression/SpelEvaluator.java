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

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * Spring EL 表达式解析器
 *
 * @author Charles7c
 * @since 2.2.0
 */
public class SpelEvaluator implements Function<Object, Object> {

    private static final ExpressionParser PARSER;
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER;

    static {
        PARSER = new SpelExpressionParser();
        PARAMETER_NAME_DISCOVERER = new DefaultParameterNameDiscoverer();
    }

    private final Expression expression;
    private String[] parameterNames;

    public SpelEvaluator(String script, Method defineMethod) {
        expression = PARSER.parseExpression(script);
        if (defineMethod.getParameterCount() > 0) {
            parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(defineMethod);
        }
    }

    @Override
    public Object apply(Object rootObject) {
        EvaluationContext context = new StandardEvaluationContext(rootObject);
        ExpressionInvokeContext invokeContext = (ExpressionInvokeContext)rootObject;
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], invokeContext.getArgs()[i]);
            }
        }
        return expression.getValue(context);
    }
}