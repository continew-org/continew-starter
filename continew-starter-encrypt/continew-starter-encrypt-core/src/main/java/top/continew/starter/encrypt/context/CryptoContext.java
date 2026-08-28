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

package top.continew.starter.encrypt.context;

import top.continew.starter.encrypt.encryptor.IEncryptor;
import top.continew.starter.encrypt.enums.Algorithm;

import java.util.Objects;

/**
 * 加密上下文
 *
 * @author lishuyan
 * @since 2.13.2
 */
public class CryptoContext {

    /**
     * 默认算法
     */
    private Algorithm algorithm;

    /**
     * 加密/解密处理器
     * <p>
     * 优先级高于加密/解密算法
     * </p>
     */
    Class<? extends IEncryptor> encryptor;

    /**
     * 对称加密算法密钥
     */
    private String password;

    /**
     * 非对称加密算法公钥
     */
    private String publicKey;

    /**
     * 非对称加密算法私钥
     */
    private String privateKey;

    public Algorithm getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(Algorithm algorithm) {
        this.algorithm = algorithm;
    }

    public Class<? extends IEncryptor> getEncryptor() {
        return encryptor;
    }

    public void setEncryptor(Class<? extends IEncryptor> encryptor) {
        this.encryptor = encryptor;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CryptoContext that = (CryptoContext) o;
        return algorithm == that.algorithm && Objects.equals(encryptor, that.encryptor) && Objects
            .equals(password, that.password) && Objects.equals(publicKey, that.publicKey) && Objects
                .equals(privateKey, that.privateKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(algorithm, encryptor, password, publicKey, privateKey);
    }
}
