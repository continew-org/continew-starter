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
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.CryptoException;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.crypto.asymmetric.KeyType;
import top.continew.starter.encrypt.context.CryptoContext;

import java.nio.charset.StandardCharsets;

/**
 * RSA 加密器
 * <p>
 * 非对称加密算法，由罗纳德·李维斯特（Ron Rivest）、阿迪·沙米尔（Adi Shamir）和伦纳德·阿德曼（Leonard Adleman）于1977年提出，安全性基于大数因子分解问题的困难性。
 * </p>
 * <p>
 * 加密依赖公钥、解密依赖私钥，对应方向密钥未配置时快速失败，不自动生成随机密钥。
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
        String publicKey = context.getPublicKey();
        if (CharSequenceUtil.isBlank(publicKey)) {
            throw new CryptoException("RSA 公钥未配置，无法加密，请为加密上下文配置公钥");
        }
        return Base64.encode(SecureUtil.rsa(null, publicKey).encrypt(plaintext, KeyType.PublicKey));
    }

    @Override
    public String decrypt(String ciphertext) {
        String privateKey = context.getPrivateKey();
        if (CharSequenceUtil.isBlank(privateKey)) {
            throw new CryptoException("RSA 私钥未配置，无法解密，请为加密上下文配置私钥");
        }
        return new String(SecureUtil.rsa(privateKey, null)
            .decrypt(Base64.decode(ciphertext), KeyType.PrivateKey), StandardCharsets.UTF_8);
    }
}
