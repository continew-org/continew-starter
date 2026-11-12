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

package top.continew.starter.openapi.protocol;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 请求体摘要计算（SHA-256，64 位小写 hex）
 *
 * <p>摘要基于传输的原文字节计算，不做 JSON 解析、重排与字符集转换。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class ContentDigest {

    private static final String SHA_256 = "SHA-256";

    private ContentDigest() {
    }

    /**
     * 计算内容的 SHA-256 摘要
     *
     * @param content 原文字节
     * @return 64 位小写 hex 摘要
     */
    public static String sha256Hex(byte[] content) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            return HexFormat.of().formatHex(digest.digest(content));
        } catch (NoSuchAlgorithmException e) {
            // JDK 规范要求所有实现都提供 SHA-256，此处仅处理理论上的非法环境
            throw new IllegalStateException("JDK 缺少 SHA-256 实现", e);
        }
    }

    /**
     * 空内容的摘要值（GET 等无请求体请求使用）
     *
     * @return SHA-256 空串摘要，64 位小写 hex
     */
    public static String emptySha256Hex() {
        return sha256Hex(new byte[0]);
    }
}
