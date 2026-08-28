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

package top.continew.starter.encrypt.password.encoder.encryptor;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.continew.starter.core.util.SpringUtils;
import top.continew.starter.encrypt.context.CryptoContext;
import top.continew.starter.encrypt.encryptor.AbstractEncryptor;
import top.continew.starter.encrypt.password.encoder.autoconfigure.PasswordEncoderProperties;

/**
 * 密码编码器加密器
 *
 * <p>
 * 使用前必须注入 {@link PasswordEncoder}，此加密方式不可逆，适合于密码场景
 * </p>
 *
 * @see PasswordEncoder
 * @see PasswordEncoderProperties
 *
 * @author Charles7c
 * @since 2.13.3
 */
public class PasswordEncoderEncryptor extends AbstractEncryptor {

    private final PasswordEncoderProperties properties =
        SpringUtils.getBean(PasswordEncoderProperties.class, true);

    public PasswordEncoderEncryptor(CryptoContext context) {
        super(context);
    }

    @Override
    public String encrypt(String plaintext) {
        // 如果已经是加密格式，直接返回
        if (properties == null
            || properties.getAlgorithm().getPattern().matcher(plaintext).matches()) {
            return plaintext;
        }
        return SpringUtil.getBean(PasswordEncoder.class).encode(plaintext);
    }

    @Override
    public String decrypt(String ciphertext) {
        return ciphertext;
    }
}
