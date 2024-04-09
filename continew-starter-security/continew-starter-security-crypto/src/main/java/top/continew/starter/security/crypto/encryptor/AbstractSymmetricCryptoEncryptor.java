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

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.crypto.symmetric.SymmetricAlgorithm;
import cn.hutool.crypto.symmetric.SymmetricCrypto;
import top.continew.starter.core.constant.StringConstants;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 对称加/解密处理器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public abstract class AbstractSymmetricCryptoEncryptor implements IEncryptor {

    private static final Map<String, SymmetricCrypto> CACHE = new ConcurrentHashMap<>();

    @Override
    public String encrypt(String plaintext, String password, String publicKey) throws Exception {
        if (CharSequenceUtil.isBlank(plaintext)) {
            return plaintext;
        }
        return this.getCrypto(password).encryptHex(plaintext);
    }

    @Override
    public String decrypt(String ciphertext, String password, String privateKey) throws Exception {
        if (CharSequenceUtil.isBlank(ciphertext)) {
            return ciphertext;
        }
        return this.getCrypto(password).decryptStr(ciphertext);
    }

    /**
     * 获取对称加密算法
     *
     * @param password 密钥
     * @return 对称加密算法
     */
    protected SymmetricCrypto getCrypto(String password) {
        SymmetricAlgorithm algorithm = this.getAlgorithm();
        String key = algorithm + StringConstants.UNDERLINE + password;
        if (CACHE.containsKey(key)) {
            return CACHE.get(key);
        }
        SymmetricCrypto symmetricCrypto = new SymmetricCrypto(algorithm, password.getBytes(StandardCharsets.UTF_8));
        CACHE.put(key, symmetricCrypto);
        return symmetricCrypto;
    }

    /**
     * 获取对称加密算法类型
     *
     * @return 对称加密算法类型
     */
    protected abstract SymmetricAlgorithm getAlgorithm();
}
