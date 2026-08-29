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

package top.continew.starter.encrypt.encryptor;

import cn.hutool.core.codec.Base64;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import top.continew.starter.encrypt.context.CryptoContext;

import java.nio.charset.StandardCharsets;

/**
 * RSA 加密器
 * <p>
 * 非对称加密算法，由罗纳德·李维斯特（Ron Rivest）、阿迪·沙米尔（Adi Shamir）和伦纳德·阿德曼（Leonard Adleman）于1977年提出，安全性基于大数因子分解问题的困难性。
 * </p>
 *
 * @author Charles7c
 * @author lishuyan
 * @since 1.4.0
 */
public class RsaEncryptor extends AbstractEncryptor {

    /**
     * 加密上下文
     */
    private final CryptoContext context;

    public RsaEncryptor(CryptoContext context) {
        super(context);
        this.context = context;
    }

    @Override
    public String encrypt(String plaintext) {
        return Base64.encode(
            SecureUtil.rsa(null, context.getPublicKey()).encrypt(plaintext, KeyType.PublicKey));
    }

    @Override
    public String decrypt(String ciphertext) {
        return new String(SecureUtil.rsa(context.getPrivateKey(), null)
            .decrypt(Base64.decode(ciphertext), KeyType.PrivateKey), StandardCharsets.UTF_8);
    }
}
