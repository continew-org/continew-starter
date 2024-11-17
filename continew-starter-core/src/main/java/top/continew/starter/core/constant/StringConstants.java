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

package top.continew.starter.core.constant;

/**
 * 字符串相关常量
 *
 * @author looly（<a href="https://gitee.com/dromara/hutool">Hutool</a>）
 * @author Charles7c
 * @see cn.hutool.core.text.StrPool
 * @since 1.0.0
 */
public class StringConstants {

    /**
     * 空字符串 {@code ""}
     */
    public static final String EMPTY = "";

    /**
     * 空格符 {@code " "}
     */
    public static final String SPACE = " ";

    /**
     * 制表符 {@code "\t"}
     */
    public static final String TAB = "	";

    /**
     * 空 JSON {@code "{}"}
     */
    public static final String EMPTY_JSON = "{}";

    /**
     * 点 {@code "."}
     */
    public static final String DOT = ".";

    /**
     * 双点 {@code ".."}
     * <p>
     * 作为指向上级文件夹的路径，如：{@code "../path"}
     * </p>
     */
    public static final String DOUBLE_DOT = "..";

    /**
     * 逗号 {@code ","}
     */
    public static final String COMMA = ",";

    /**
     * 中文逗号 {@code "，"}
     */
    public static final String CHINESE_COMMA = "，";

    /**
     * 冒号 {@code ":"}
     */
    public static final String COLON = ":";

    /**
     * 分号 {@code ";"}
     */
    public static final String SEMICOLON = ";";

    /**
     * 问号 {@code "?"}
     */
    public static final String QUESTION_MARK = "?";

    /**
     * 下划线 {@code "_"}
     */
    public static final String UNDERLINE = "_";

    /**
     * 减号（连接符） {@code "-"}
     */
    public static final String DASHED = "-";

    /**
     * 加号 {@code "+"}
     */
    public static final String PLUS = "+";

    /**
     * 等号 {@code "="}
     */
    public static final String EQUALS = "=";

    /**
     * 星号 {@code "*"}
     */
    public static final String ASTERISK = "*";

    /**
     * 斜杠 {@code "/"}
     */
    public static final String SLASH = "/";

    /**
     * 反斜杠 {@code "\\"}
     */
    public static final String BACKSLASH = "\\";

    /**
     * 管道符 {@code "|"}
     */
    public static final String PIPE = "|";

    /**
     * 艾特 {@code "@"}
     */
    public static final String AT = "@";

    /**
     * 与符号 {@code "&"}
     */
    public static final String AMP = "&";

    /**
     * 花括号（左） <code>"{"</code>
     */
    public static final String DELIM_START = "{";

    /**
     * 花括号（右） <code>"}"</code>
     */
    public static final String DELIM_END = "}";

    /**
     * 中括号（左） {@code "["}
     */
    public static final String BRACKET_START = "[";

    /**
     * 中括号（右） {@code "]"}
     */
    public static final String BRACKET_END = "]";

    /**
     * 圆括号（左） {@code "("}
     */
    public static final String ROUND_BRACKET_START = "(";

    /**
     * 圆括号（右） {@code ")"}
     */
    public static final String ROUND_BRACKET_END = ")";

    /**
     * 双引号 {@code "\""}
     */
    public static final String DOUBLE_QUOTES = "\"";

    /**
     * 单引号 {@code "'"}
     */
    public static final String SINGLE_QUOTE = "'";

    /**
     * 回车符 {@code "\r"}
     */
    public static final String CR = "\r";

    /**
     * 换行符 {@code "\n"}
     */
    public static final String LF = "\n";

    /**
     * 路径模式 {@code "/**"}
     */
    public static final String PATH_PATTERN = "/**";

    /**
     * 路径模式（仅匹配当前目录） {@code "/*"}
     */
    public static final String PATH_PATTERN_CURRENT_DIR = "/*";

    /**
     * HTML 不间断空格转义 {@code "&nbsp;" -> " "}
     */
    public static final String HTML_NBSP = "&nbsp;";

    /**
     * HTML And 符转义 {@code "&amp;" -> "&"}
     */
    public static final String HTML_AMP = "&amp;";

    /**
     * HTML 双引号转义 {@code "&quot;" -> "\""}
     */
    public static final String HTML_QUOTE = "&quot;";

    /**
     * HTML 单引号转义 {@code "&apos" -> "'"}
     */
    public static final String HTML_APOS = "&apos;";

    /**
     * HTML 小于号转义 {@code "&lt;" -> "<"}
     */
    public static final String HTML_LT = "&lt;";

    /**
     * HTML 大于号转义 {@code "&gt;" -> ">"}
     */
    public static final String HTML_GT = "&gt;";

    private StringConstants() {
    }
}
