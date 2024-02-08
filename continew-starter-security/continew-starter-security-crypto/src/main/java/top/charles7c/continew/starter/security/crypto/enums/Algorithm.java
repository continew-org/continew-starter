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

package top.charles7c.continew.starter.security.crypto.enums;

import top.charles7c.continew.starter.security.crypto.encryptor.AesEncryptor;
import top.charles7c.continew.starter.security.crypto.encryptor.Base64Encryptor;
import top.charles7c.continew.starter.security.crypto.encryptor.IEncryptor;
import top.charles7c.continew.starter.security.crypto.encryptor.RsaEncryptor;

/**
 * 加密/解密算法枚举
 *
 * @author Charles7c
 * @since 1.4.0
 */
public enum Algorithm {

    /**
     * AES
     */
    AES(AesEncryptor.class),

    /**
     * RSA
     */
    RSA(RsaEncryptor.class),

    /**
     * Base64
     */
    BASE64(Base64Encryptor.class),;

    /**
     * 加密/解密处理器
     */
    private final Class<? extends IEncryptor> encryptor;

    Algorithm(Class<? extends IEncryptor> encryptor) {
        this.encryptor = encryptor;
    }

    public Class<? extends IEncryptor> getEncryptor() {
        return encryptor;
    }
}
