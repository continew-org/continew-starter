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

package top.continew.starter.security.crypto.encryptor;

/**
 * 加/解密接口
 *
 * @author Charles7c
 * @since 1.4.0
 */
public interface IEncryptor {

    /**
     * 加密
     *
     * @param plaintext 明文
     * @param password  对称加密算法密钥
     * @param publicKey 非对称加密算法公钥
     * @return 加密后的文本
     * @throws Exception /
     */
    String encrypt(String plaintext, String password, String publicKey) throws Exception;

    /**
     * 解密
     *
     * @param ciphertext 密文
     * @param password   对称加密算法密钥
     * @param privateKey 非对称加密算法私钥
     * @return 解密后的文本
     * @throws Exception /
     */
    String decrypt(String ciphertext, String password, String privateKey) throws Exception;
}
