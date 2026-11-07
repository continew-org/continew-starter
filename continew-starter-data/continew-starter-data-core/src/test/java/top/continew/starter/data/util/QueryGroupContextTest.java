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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * {@link QueryGroupContext} 分组上下文测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class QueryGroupContextTest {

    @AfterEach
    void tearDown() {
        QueryGroupContext.runWith(null, () -> {
        });
    }

    /**
     * callWith 执行期间应绑定分组，结束后恢复原值。
     */
    @Test
    void shouldBindGroupDuringCallAndRestoreAfter() {
        String result = QueryGroupContext.callWith("list", () -> QueryGroupContext.get());
        assertEquals("list", result);
        assertNull(QueryGroupContext.get());
    }

    /**
     * 嵌套设置时内层结束后应恢复外层分组而非清空，外层结束后恢复进入前的原值。
     */
    @Test
    void shouldRestoreOuterGroupOnNestedCall() {
        String result = QueryGroupContext.callWith("export", () -> {
            String inner = QueryGroupContext.callWith("list", QueryGroupContext::get);
            assertEquals("export", QueryGroupContext.get());
            return inner;
        });
        assertEquals("list", result);
        assertNull(QueryGroupContext.get());
    }

    /**
     * 动作抛出异常时分组同样应恢复，不向上残留。
     */
    @Test
    void shouldRestoreGroupWhenActionThrows() {
        try {
            QueryGroupContext.runWith("list", () -> {
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException e) {
            // 抛出即验证目标，无需额外断言
        }
        assertNull(QueryGroupContext.get());
    }

    /**
     * 动作返回值应原样透传。
     */
    @Test
    void shouldReturnValueFromAction() {
        String value = QueryGroupContext.callWith("export", () -> "done");
        assertEquals("done", value);
    }
}
