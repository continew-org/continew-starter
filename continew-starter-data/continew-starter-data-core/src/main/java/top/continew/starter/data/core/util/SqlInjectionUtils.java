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

package top.continew.starter.data.core.util;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * SQL 注入验证工具类
 *
 * @author hubin
 * @since 2.5.2
 */
public class SqlInjectionUtils {

    /**
     * SQL语法检查正则：符合两个关键字（有先后顺序）才算匹配
     */
    private static final Pattern SQL_SYNTAX_PATTERN = Pattern
        .compile("(insert|delete|update|select|create|drop|truncate|grant|alter|deny|revoke|call|execute|exec|declare|show|rename|set)" + "\\s+.*(into|from|set|where|table|database|view|index|on|cursor|procedure|trigger|for|password|union|and|or)|(select\\s*\\*\\s*from\\s+)|(and|or)\\s+.*", Pattern.CASE_INSENSITIVE);

    /**
     * 使用'、;或注释截断SQL检查正则
     */
    private static final Pattern SQL_COMMENT_PATTERN = Pattern
        .compile("'.*(or|union|--|#|/\\*|;)", Pattern.CASE_INSENSITIVE);

    /**
     * 检查参数是否存在 SQL 注入
     *
     * @param value 检查参数
     * @return true：非法；false：合法
     */
    public static boolean check(String value) {
        Objects.requireNonNull(value);
        // 处理是否包含 SQL 注释字符 || 检查是否包含 SQ L注入敏感字符
        return SQL_COMMENT_PATTERN.matcher(value).find() || SQL_SYNTAX_PATTERN.matcher(value).find();
    }
}