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

package top.continew.starter.auth.refresh.token.support;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.convert.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.auth.refresh.token.api.AuthSessionConstants;

/**
 * Access Token 声明读取器。
 *
 * <p>Access Token 由 Sa-Token 签发，本类是认证会话模块读取其声明的唯一入口：会话
 * 声明（{@code sid}）与登录主体 ID 都在此统一解析，过期、被篡改或格式非法的 Token
 * 一律静默降级为 {@code null}——会话已失效的热路径不应因解析异常冒泡成 500，且
 * Sa-Token 对无效 Token 的行为（返回 {@code null} 或抛 {@code NotLoginException}）
 * 取决于实现与版本，调用方不应各自假设。后续如需支持其它认证框架，只需替换本类。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
public final class AccessTokenClaimsReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AccessTokenClaimsReader.class);

    private AccessTokenClaimsReader() {
    }

    /**
     * 读取 Access Token 绑定的 Refresh Session ID。
     *
     * @param accessToken 明文 Access Token
     * @return 会话 ID；为空或无法解析时返回 {@code null}
     */
    public static String resolveSessionIdQuietly(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            return Convert.toStr(StpUtil.getExtra(accessToken,
                AuthSessionConstants.SESSION_ID_CLAIM));
        } catch (RuntimeException e) {
            LOGGER.debug("忽略无法解析会话声明的 Access Token", e);
            return null;
        }
    }

    /**
     * 读取 Access Token 的登录主体 ID。
     *
     * @param accessToken 明文 Access Token
     * @return 登录主体 ID；为空或无法解析时返回 {@code null}
     */
    public static Long resolveLoginIdQuietly(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return null;
        }
        try {
            return Convert.toLong(StpUtil.getLoginIdByToken(accessToken));
        } catch (RuntimeException e) {
            LOGGER.debug("忽略无法解析登录主体的 Access Token", e);
            return null;
        }
    }
}
