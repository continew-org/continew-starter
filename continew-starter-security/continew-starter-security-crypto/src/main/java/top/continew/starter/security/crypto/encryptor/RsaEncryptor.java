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

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;

/**
 * RSA 加/解密处理器
 * <p>
 * 非对称加密算法，由罗纳德·李维斯特（Ron Rivest）、阿迪·沙米尔（Adi Shamir）和伦纳德·阿德曼（Leonard Adleman）于1977年提出，安全性基于大数因子分解问题的困难性。
 * </p>
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class RsaEncryptor implements IEncryptor {

    @Override
    public String encrypt(String plaintext, String password, String publicKey) throws Exception {
        return Base64.encode(SecureUtil.rsa(null, publicKey).encrypt(plaintext, KeyType.PublicKey));
    }

    @Override
    public String decrypt(String ciphertext, String password, String privateKey) throws Exception {
        return new String(SecureUtil.rsa(privateKey, null).decrypt(Base64.decode(ciphertext), KeyType.PrivateKey));
    }
}
