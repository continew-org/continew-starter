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
 * PBEWithMD5AndDES（Password Based Encryption With MD5 And DES） 加/解密处理器
 * <p>
 * 混合加密算法，结合了 MD5 散列算法和 DES（Data Encryption Standard）加密算法
 * </p>
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class PbeWithMd5AndDesEncryptor extends AbstractSymmetricCryptoEncryptor {

    @Override
    protected SymmetricAlgorithm getAlgorithm() {
        return SymmetricAlgorithm.PBEWithMD5AndDES;
    }
}
