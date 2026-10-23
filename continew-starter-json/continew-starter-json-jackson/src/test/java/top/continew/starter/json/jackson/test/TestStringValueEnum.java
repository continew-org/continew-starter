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

package top.continew.starter.json.jackson.test;

import top.continew.starter.core.enums.BaseEnum;

/**
 * 测试用字符串值枚举
 *
 * @author Charles7c
 * @since 3.0.0
 */
public enum TestStringValueEnum implements BaseEnum<String> {

    /**
     * 启用
     */
    ACTIVE("1", "启用"),
    /**
     * 禁用
     */
    DISABLED("0", "禁用");

    private final String value;
    private final String description;

    TestStringValueEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getDescription() {
        return description;
    }
}
