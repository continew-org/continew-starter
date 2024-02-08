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

package top.charles7c.continew.starter.security.crypto.encryptor;

import cn.hutool.core.codec.Base64;

/**
 * Base64 加/解密处理器
 * <p>
 * 一种用于编码二进制数据到文本格式的算法，常用于邮件附件、网页传输等场合，但它不是一种加密算法，只提供数据的编码和解码，不保证数据的安全性。
 * </p>
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class Base64Encryptor implements IEncryptor {

    @Override
    public String encrypt(String plaintext, String password, String publicKey) throws Exception {
        return Base64.encode(plaintext);
    }

    @Override
    public String decrypt(String ciphertext, String password, String privateKey) throws Exception {
        return Base64.decodeStr(ciphertext);
    }
}
