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

package top.continew.starter.auth.openapi.enums;

/**
 * 签名算法枚举
 *
 * @author Charles7c
 * @since 2.16.0
 */
public enum SignAlgorithm {

    /**
     * MD5 算法
     */
    MD5("MD5"),

    /**
     * SHA256 算法
     */
    SHA256("SHA-256"),

    /**
     * HmacMD5 算法
     */
    HMAC_MD5("HmacMD5"),

    /**
     * HmacSHA256 算法
     */
    HMAC_SHA256("HmacSHA256");

    private final String algorithm;

    SignAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
