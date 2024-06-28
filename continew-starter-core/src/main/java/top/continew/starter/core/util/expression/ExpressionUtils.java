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

import cn.hutool.core.text.CharSequenceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * 表达式解析工具类
 *
 * @author Charles7c
 * @since 2.2.0
 */
public class ExpressionUtils {

    private static final Logger log = LoggerFactory.getLogger(ExpressionUtils.class);

    private ExpressionUtils() {
    }

    /**
     * 解析
     *
     * @param script 表达式
     * @param target 目标对象
     * @param method 目标方法
     * @param args   方法参数
     * @return 解析结果
     */
    public static Object eval(String script, Object target, Method method, Object... args) {
        try {
            if (CharSequenceUtil.isBlank(script)) {
                return null;
            }
            ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(script, method);
            ExpressionInvokeContext invokeContext = new ExpressionInvokeContext(method, args, target);
            return expressionEvaluator.apply(invokeContext);
        } catch (Exception e) {
            log.error("Error occurs when eval script \"{}\" in {} : {}", script, method, e.getMessage(), e);
            return null;
        }
    }
}
