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

import cn.hutool.crypto.symmetric.SymmetricAlgorithm;

/**
 * AES（Advanced Encryption Standard） 加/解密处理器
 * <p>
 * 美国国家标准与技术研究院(NIST)采纳的对称加密算法标准，提供128位、192位和256位三种密钥长度，以高效和安全性著称。
 * </p>
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class AesEncryptor extends AbstractSymmetricCryptoEncryptor {

    @Override
    protected SymmetricAlgorithm getAlgorithm() {
        return SymmetricAlgorithm.AES;
    }
}
