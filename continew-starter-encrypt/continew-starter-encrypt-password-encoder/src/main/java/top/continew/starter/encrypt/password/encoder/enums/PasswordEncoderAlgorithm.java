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

package top.continew.starter.encrypt.password.encoder.enums;

import java.util.regex.Pattern;

/**
 * 密码编码器加密算法枚举
 *
 * @author Charles7c
 * @since 2.13.3
 */
public enum PasswordEncoderAlgorithm {

    /** BCrypt加密算法 */
    BCRYPT(Pattern.compile("\\A\\$2(a|y|b)?\\$(\\d\\d)\\$[./0-9A-Za-z]{53}")),

    /** SCrypt加密算法 */
    SCRYPT(Pattern.compile("\\A\\$s0\\$[0-9a-f]+\\$[0-9a-f]+\\$[0-9a-f]+")),

    /** PBKDF2加密算法 */
    PBKDF2(Pattern.compile("\\A\\$pbkdf2-sha256\\$\\d+\\$[0-9a-f]+\\$[0-9a-f]+")),

    /** Argon2加密算法 */
    ARGON2(Pattern.compile(
        "\\A\\$argon2(id|i|d)\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[0-9a-zA-Z+/]+\\$[0-9a-zA-Z+/]+"));

    /** 正则匹配 */
    private final Pattern pattern;

    PasswordEncoderAlgorithm(Pattern pattern) {
        this.pattern = pattern;
    }

    public Pattern getPattern() {
        return pattern;
    }
}
