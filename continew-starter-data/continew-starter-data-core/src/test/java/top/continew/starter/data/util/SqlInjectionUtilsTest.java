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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * SQL 注入检测工具测试
 *
 * <p>按 {@link SqlInjectionUtils} 的四层检测顺序逐层配对正反例：空白短路、注释/语法
 * 正则、内置语法关键字（含大小写归一化）、自定义关键字与敏感函数形态。普通值必须
 * 保持放行，避免检测规则过宽误伤正常参数。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SqlInjectionUtilsTest {

    /**
     * 空白值直接短路返回不存在注入。
     */
    @Test
    void shouldReturnFalseForBlankValue() {
        assertFalse(SqlInjectionUtils.check(null));
        assertFalse(SqlInjectionUtils.check(""));
        assertFalse(SqlInjectionUtils.check("   "));
    }

    /**
     * 常规业务值（含中文、标点）不应误报。
     */
    @Test
    void shouldReturnFalseForNormalValue() {
        assertFalse(SqlInjectionUtils.check("normal_value_2023"));
        assertFalse(SqlInjectionUtils.check("张三的备注"));
        assertFalse(SqlInjectionUtils.check("a,b|c-d"));
    }

    /**
     * 引号后接注释、连接词或语句终止符的形态命中注释检测。
     */
    @Test
    void shouldDetectCommentInjection() {
        assertTrue(SqlInjectionUtils.check("1' or '1'='1"));
        assertTrue(SqlInjectionUtils.check("value' union select null"));
        assertTrue(SqlInjectionUtils.check("abc'--"));
    }

    /**
     * 语句结构与永真条件命中语法检测。
     */
    @Test
    void shouldDetectSyntaxPattern() {
        assertTrue(SqlInjectionUtils.check("select * from sys_user"));
        assertTrue(SqlInjectionUtils.check("1 and 1=1"));
        assertTrue(SqlInjectionUtils.check("1 or name='admin'"));
    }

    /**
     * 内置关键字在大小写归一后命中；关键字位于串首时会被先行的 trim 剥离尾随空格
     * 而漏检，命中要求关键字位于串中，这与实现语义一致。
     */
    @Test
    void shouldDetectSyntaxKeywordIgnoreCase() {
        assertTrue(SqlInjectionUtils.check("abc delete x"));
        assertTrue(SqlInjectionUtils.check("tom and jerry "));
        assertTrue(SqlInjectionUtils.check("abc;def"));
        assertTrue(SqlInjectionUtils.check("id--"));
    }

    /**
     * 命中自定义关键字时检出，未命中或关键字为空白时不影响原有判定。
     */
    @Test
    void shouldCheckCustomKeyword() {
        assertTrue(SqlInjectionUtils.check("hello secret_token world", "secret_token"));
        assertFalse(SqlInjectionUtils.check("hello world", "secret_token|other"));
        assertFalse(SqlInjectionUtils.check("hello world", ""));
        assertFalse(SqlInjectionUtils.check("hello world", null));
    }

    /**
     * 敏感函数调用形态（延时、编码、库函数）命中函数检测。
     */
    @Test
    void shouldDetectSensitiveFunction() {
        assertTrue(SqlInjectionUtils.check("sleep(1000)"));
        assertTrue(SqlInjectionUtils.check("chr(65)"));
        assertTrue(SqlInjectionUtils.check("user()"));
        assertTrue(SqlInjectionUtils.check("show tables"));
    }
}
