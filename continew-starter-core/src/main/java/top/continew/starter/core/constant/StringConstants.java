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

import cn.hutool.core.text.CharPool;
import cn.hutool.core.text.StrPool;
import cn.hutool.core.util.XmlUtil;

/**
 * 字符串相关常量
 *
 * @author looly
 * @author Charles7c
 * @since 1.0.0
 */
public class StringConstants {

    /**
     * 字符常量：空格符 {@code ' '}
     */
    public static final char C_SPACE = CharPool.SPACE;

    /**
     * 字符常量：制表符 {@code '\t'}
     */
    public static final char C_TAB = CharPool.TAB;

    /**
     * 字符常量：点 {@code '.'}
     */
    public static final char C_DOT = CharPool.DOT;

    /**
     * 字符常量：斜杠 {@code '/'}
     */
    public static final char C_SLASH = CharPool.SLASH;

    /**
     * 字符常量：反斜杠 {@code '\\'}
     */
    public static final char C_BACKSLASH = CharPool.BACKSLASH;

    /**
     * 字符常量：回车符 {@code '\r'}
     */
    public static final char C_CR = CharPool.CR;

    /**
     * 字符常量：换行符 {@code '\n'}
     */
    public static final char C_LF = CharPool.LF;

    /**
     * 字符常量：下划线 {@code '_'}
     */
    public static final char C_UNDERLINE = CharPool.UNDERLINE;

    /**
     * 字符常量：逗号 {@code ','}
     */
    public static final char C_COMMA = CharPool.COMMA;

    /**
     * 字符常量：花括号（左） <code>'{'</code>
     */
    public static final char C_DELIM_START = CharPool.DELIM_START;

    /**
     * 字符常量：花括号（右） <code>'}'</code>
     */
    public static final char C_DELIM_END = CharPool.DELIM_END;

    /**
     * 字符常量：中括号（左） {@code '['}
     */
    public static final char C_BRACKET_START = CharPool.BRACKET_START;

    /**
     * 字符常量：中括号（右） {@code ']'}
     */
    public static final char C_BRACKET_END = CharPool.BRACKET_END;

    /**
     * 字符常量：冒号 {@code ':'}
     */
    public static final char C_COLON = CharPool.COLON;

    /**
     * 字符常量：艾特 {@code '@'}
     */
    public static final char C_AT = CharPool.AT;

    /**
     * 字符常量：星号 {@code '*'}
     */
    public static final char C_ASTERISK = '*';

    /**
     * 字符串常量：制表符 {@code "\t"}
     */
    public static final String TAB = StrPool.TAB;

    /**
     * 字符串常量：点 {@code "."}
     */
    public static final String DOT = StrPool.DOT;

    /**
     * 字符串常量：双点 {@code ".."} <br>
     * 用途：作为指向上级文件夹的路径，如：{@code "../path"}
     */
    public static final String DOUBLE_DOT = StrPool.DOUBLE_DOT;

    /**
     * 字符串常量：斜杠 {@code "/"}
     */
    public static final String SLASH = StrPool.SLASH;

    /**
     * 字符串常量：反斜杠 {@code "\\"}
     */
    public static final String BACKSLASH = StrPool.BACKSLASH;

    /**
     * 字符串常量：回车符 {@code "\r"} <br>
     * 解释：该字符常用于表示 Linux 系统和 MacOS 系统下的文本换行
     */
    public static final String CR = StrPool.CR;

    /**
     * 字符串常量：换行符 {@code "\n"}
     */
    public static final String LF = StrPool.LF;

    /**
     * 字符串常量：Windows 换行 {@code "\r\n"} <br>
     * 解释：该字符串常用于表示 Windows 系统下的文本换行
     */
    public static final String CRLF = StrPool.CRLF;

    /**
     * 字符串常量：下划线 {@code "_"}
     */
    public static final String UNDERLINE = StrPool.UNDERLINE;

    /**
     * 字符串常量：减号（连接符） {@code "-"}
     */
    public static final String DASHED = StrPool.DASHED;

    /**
     * 字符串常量：逗号 {@code ","}
     */
    public static final String COMMA = StrPool.COMMA;

    /**
     * 字符串常量：花括号（左） <code>"{"</code>
     */
    public static final String DELIM_START = StrPool.DELIM_START;

    /**
     * 字符串常量：花括号（右） <code>"}"</code>
     */
    public static final String DELIM_END = StrPool.DELIM_END;

    /**
     * 字符串常量：中括号（左） {@code "["}
     */
    public static final String BRACKET_START = StrPool.BRACKET_START;

    /**
     * 字符串常量：中括号（右） {@code "]"}
     */
    public static final String BRACKET_END = StrPool.BRACKET_END;

    /**
     * 字符串常量：冒号 {@code ":"}
     */
    public static final String COLON = StrPool.COLON;

    /**
     * 字符串常量：艾特 {@code "@"}
     */
    public static final String AT = StrPool.AT;

    /**
     * 字符串常量：HTML 不间断空格转义 {@code "&nbsp;" -> " "}
     */
    public static final String HTML_NBSP = XmlUtil.NBSP;

    /**
     * 字符串常量：HTML And 符转义 {@code "&amp;" -> "&"}
     */
    public static final String HTML_AMP = XmlUtil.AMP;

    /**
     * 字符串常量：HTML 双引号转义 {@code "&quot;" -> "\""}
     */
    public static final String HTML_QUOTE = XmlUtil.QUOTE;

    /**
     * 字符串常量：HTML 单引号转义 {@code "&apos" -> "'"}
     */
    public static final String HTML_APOS = XmlUtil.APOS;

    /**
     * 字符串常量：HTML 小于号转义 {@code "&lt;" -> "<"}
     */
    public static final String HTML_LT = XmlUtil.LT;

    /**
     * 字符串常量：HTML 大于号转义 {@code "&gt;" -> ">"}
     */
    public static final String HTML_GT = XmlUtil.GT;

    /**
     * 字符串常量：空 JSON {@code "{}"}
     */
    public static final String EMPTY_JSON = StrPool.EMPTY_JSON;

    /**
     * 空字符串
     */
    public static final String EMPTY = "";

    /**
     * 空格
     */
    public static final String SPACE = " ";

    /**
     * 分号
     */
    public static final String SEMICOLON = ";";

    /**
     * 星号
     */
    public static final String ASTERISK = "*";

    /**
     * 问号
     */
    public static final String QUESTION_MARK = "?";

    /**
     * 中文逗号
     */
    public static final String CHINESE_COMMA = "，";

    /**
     * 路径模式
     */
    public static final String PATH_PATTERN = "/**";

    /**
     * 路径模式（仅匹配当前目录）
     */
    public static final String PATH_PATTERN_CURRENT_DIR = "/*";

    private StringConstants() {
    }
}
