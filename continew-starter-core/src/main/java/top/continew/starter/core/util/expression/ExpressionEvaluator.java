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

import java.lang.reflect.Method;
import java.util.function.Function;

/**
 * 表达式解析器
 *
 * @author Charles7c
 * @since 2.2.0
 */
public class ExpressionEvaluator implements Function<Object, Object> {

    private final Function<Object, Object> evaluator;

    public ExpressionEvaluator(String script, Method defineMethod) {
        this.evaluator = new SpelEvaluator(script, defineMethod);
    }

    @Override
    public Object apply(Object rootObject) {
        return evaluator.apply(rootObject);
    }

    Function<Object, Object> getEvaluator() {
        return evaluator;
    }
}
