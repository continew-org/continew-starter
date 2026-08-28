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

package top.continew.starter.encrypt.password.encoder.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.scrypt.SCryptPasswordEncoder;
import top.continew.starter.encrypt.password.encoder.exception.PasswordEncodeException;
import top.continew.starter.encrypt.password.encoder.enums.PasswordEncoderAlgorithm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 密码加密工具类
 * <p>
 * 支持多种加密算法，可通过编码ID动态选择加密方式
 * </p>
 *
 * @author Charles7c
 * @since 2.13.3
 */
public final class PasswordEncoderUtil {

    private static final Map<PasswordEncoderAlgorithm, PasswordEncoder> ENCODER_CACHE =
        new ConcurrentHashMap<>();

    static {
        // 初始化默认的加密算法实例
        ENCODER_CACHE.put(PasswordEncoderAlgorithm.BCRYPT, new BCryptPasswordEncoder());
        ENCODER_CACHE.put(PasswordEncoderAlgorithm.SCRYPT,
            SCryptPasswordEncoder.defaultsForSpringSecurity_v5_8());
        ENCODER_CACHE.put(PasswordEncoderAlgorithm.PBKDF2,
            Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8());
        ENCODER_CACHE.put(PasswordEncoderAlgorithm.ARGON2,
            Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8());
    }

    private PasswordEncoderUtil() {
    }

    /**
     * 使用指定的加密算法加密密码
     *
     * @param algorithm   加密算法
     * @param rawPassword 原始密码
     * @return 加密后的密码
     * @throws IllegalArgumentException 如果不支持指定的加密算法
     */
    public static String encode(PasswordEncoderAlgorithm algorithm, String rawPassword) {
        // 参数校验
        if (algorithm == null) {
            throw new IllegalArgumentException("加密算法不能为空");
        }
        if (rawPassword == null) {
            throw new IllegalArgumentException("原始密码不能为空");
        }

        // 获取对应的密码编码器
        PasswordEncoder encoder = ENCODER_CACHE.get(algorithm);
        if (encoder == null) {
            throw new IllegalArgumentException("不支持的加密算法: " + algorithm);
        }

        try {
            return encoder.encode(rawPassword);
        } catch (Exception e) {
            throw new PasswordEncodeException("密码加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证密码是否匹配
     *
     * @param algorithm       加密算法
     * @param rawPassword     原始密码
     * @param encodedPassword 加密后的密码
     * @return 是否匹配
     * @throws IllegalArgumentException 如果不支持指定的加密算法
     */
    public static boolean matches(PasswordEncoderAlgorithm algorithm, String rawPassword,
        String encodedPassword) {
        // 参数校验
        if (algorithm == null) {
            throw new IllegalArgumentException("加密算法不能为空");
        }
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }

        // 获取对应的密码编码器
        PasswordEncoder encoder = ENCODER_CACHE.get(algorithm);
        if (encoder == null) {
            throw new IllegalArgumentException("不支持的加密算法: " + algorithm);
        }

        try {
            return encoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取指定算法的密码编码器
     *
     * @param algorithm 加密算法
     * @return 密码编码器实例，不存在则返回null
     */
    public static PasswordEncoder getEncoder(PasswordEncoderAlgorithm algorithm) {
        if (algorithm == null) {
            return null;
        }
        return ENCODER_CACHE.get(algorithm);
    }
}
