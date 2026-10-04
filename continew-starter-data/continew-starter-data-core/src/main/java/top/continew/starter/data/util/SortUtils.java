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

import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.util.validation.ValidationUtils;

import java.util.regex.Pattern;

/**
 * 排序工具类
 *
 * @author Charles7c
 * @since 2.16.1
 */
public class SortUtils {

    /**
     * 合法排序字段的单个标识符片段：字母、数字或下划线。
     *
     * <p>刻意不采用「标识符 + 分隔符」形式的整体正则（如 {@code \w+(\.\w+)*}）：
     * 该形式在 Java 正则引擎中每次分组迭代都会递归消耗栈，点分段输入达到约 2000 段
     * 即可触发 StackOverflowError，而排序字段是请求参数可直接控制的，存在可用性风险。
     * 改为按 {@code '.'} 线性切分后逐段匹配单量词正则，彻底消除递归。</p>
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("\\w+");

    private SortUtils() {
    }

    /**
     * 校验排序字段是否为合法标识符。
     *
     * <p>
     * 排序字段会被直接拼接进 ORDER BY，仅靠 SQL 注入关键字黑名单无法穷举所有
     * 攻击特征（如 {@code case when} 表达式配合无引号的注释符），因此必须按
     * 标识符白名单校验整个字段（含表别名前缀），而非只校验部分片段。
     * </p>
     *
     * @param property 排序字段
     * @throws BadRequestException 排序字段不是合法标识符时抛出
     * @since 2.16.1
     */
    public static void validateProperty(String property) {
        boolean isInvalid = property == null || property.isEmpty();
        if (!isInvalid) {
            // 线性切分 + 逐段单量词匹配：不存在嵌套量词，输入长度与耗时均呈线性
            for (String segment : property.split("\\.", -1)) {
                if (!IDENTIFIER_PATTERN.matcher(segment).matches()) {
                    isInvalid = true;
                    break;
                }
            }
        }
        ValidationUtils.throwIf(isInvalid, "无效的排序字段 [{}]", property);
    }
}
