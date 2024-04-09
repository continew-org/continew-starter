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
 * DES（Data Encryption Standard） 加/解密处理器
 * <p>
 * 一种对称加密算法，使用相同的密钥进行加密和解密。DES 使用 56 位密钥（实际上有 64 位，但有 8 位用于奇偶校验）和一系列置换和替换操作来加密数据。
 * </p>
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class DesEncryptor extends AbstractSymmetricCryptoEncryptor {

    @Override
    protected SymmetricAlgorithm getAlgorithm() {
        return SymmetricAlgorithm.DES;
    }
}
