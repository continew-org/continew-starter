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
     * 合法排序字段正则：仅允许字母、数字、下划线，可携带表别名（如 u.create_time）
     */
    private static final Pattern SORT_PROPERTY_PATTERN = Pattern
        .compile("[a-zA-Z0-9_]+(\\.[a-zA-Z0-9_]+)*");

    private SortUtils() {
    }

    /**
     * 校验排序字段是否为合法标识符
     *
     * <p>
     * 排序字段会被直接拼接进 ORDER BY，仅靠 SQL 注入关键字黑名单无法穷举所有
     * 攻击特征（如 {@code case when} 表达式配合无单引号的注释符），因此必须按
     * 标识符白名单校验整个字段（含表别名前缀），而非只校验部分片段。
     * </p>
     *
     * @param property 排序字段
     * @throws BadRequestException 排序字段不是合法标识符时抛出
     * @since 2.16.1
     */
    public static void validateProperty(String property) {
        boolean isInvalid =
            property == null || !SORT_PROPERTY_PATTERN.matcher(property).matches();
        ValidationUtils.throwIf(isInvalid, "无效的排序字段 [{}]", property);
    }
}
