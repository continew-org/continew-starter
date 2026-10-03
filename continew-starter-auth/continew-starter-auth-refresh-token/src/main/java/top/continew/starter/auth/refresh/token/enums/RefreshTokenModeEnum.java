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

package top.continew.starter.auth.refresh.token.enums;

import top.continew.starter.core.enums.BaseEnum;

/**
 * Refresh Token 传输模式。
 *
 * <p>COOKIE 适用于浏览器：长期凭证由 HttpOnly Cookie 承载，前端 JavaScript 无法读取。
 * BODY 适用于 App、小程序，长期凭证由客户端安全存储后通过请求体提交。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public enum RefreshTokenModeEnum implements BaseEnum<String> {

    /**
     * 浏览器 Cookie 模式
     */
    COOKIE("COOKIE", "Cookie"),

    /**
     * 原生 App / 小程序请求体模式
     */
    BODY("BODY", "请求体");

    private final String value;
    private final String description;

    RefreshTokenModeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @Override
    public String getValue() {
        return this.value;
    }

    @Override
    public String getDescription() {
        return this.description;
    }
}
