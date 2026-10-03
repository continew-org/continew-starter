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

package top.continew.starter.auth.refresh.token.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.auth.refresh.token.model.AuthSecurityVersion;
import top.continew.starter.auth.refresh.token.model.IssuedAccessToken;
import top.continew.starter.auth.refresh.token.model.RefreshClientPolicy;
import top.continew.starter.auth.refresh.token.model.RefreshIssueResult;
import top.continew.starter.auth.refresh.token.model.RefreshSession;
import top.continew.starter.auth.refresh.token.model.RefreshSessionPrincipal;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Refresh Token 会话服务。
 *
 * <p>负责登录会话的创建、原子轮换、重放保护、查询与统一撤销。Sa-Token 仅管理短期
 * Access Token；并发登录、顶人下线和最大登录数量以 Refresh Session 为唯一事实源。</p>
 *
 * <p><b>刷新端点调用契约：</b>登录/刷新端点必须按固定顺序串起安全钩子——
 * {@link #resolve(String, HttpServletRequest)} 提取令牌 →
 * {@link #checkRequestRateLimit(HttpServletRequest)} 限流 →
 * {@link #validateRequest(String, HttpServletRequest)} 校验 Cookie 来源（CSRF）→
 * {@link #rotate(String, HttpServletResponse, Function)} 原子轮换。前三步是独立方法，
 * 任意一步漏调用都会敞开对应攻击面（限流绕过 / 跨站伪造刷新），请务必完整调用。</p>
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
public interface RefreshTokenService {

    /**
     * 为一次新登录生成 Session ID，供 Access Token 和 Refresh Token 共同绑定。
     *
     * @return 新生成的 Session ID
     */
    String newSessionId();

    /**
     * 在固定作用域锁内复查登录状态、执行 Session 数量策略并创建会话。
     *
     * <p>并发登录和最大登录数以 Refresh Session 为唯一事实源，不能依赖生命周期更短的
     * Access Token。当前浏览器中将被新 Cookie 覆盖的旧会话也在同一临界区内撤销。
     * 锁顺序固定为用户、租户、客户端，与安全配置失效共用同一组锁；状态复查函数也在
     * 锁内执行，调用方无法绕过临界区单独调用登录策略。</p>
     *
     * @param userId         初步认证得到的用户 ID，仅用于确定锁范围
     * @param clientId       初步认证得到的客户端 ID，仅用于确定锁范围
     * @param tenantId       租户 ID
     * @param attemptFactory 锁内最终状态复查函数；接收需要固化到新 Session 的安全版本
     * @return 签发结果
     */
    <T> T executeLoginPolicy(Long userId, String clientId, Long tenantId,
        Function<AuthSecurityVersion, LoginAttempt<T>> attemptFactory);

    /**
     * 创建登录会话的 Refresh Token。
     *
     * @param sessionId        会话 ID（同时写入 Access Token 的 {@code sid} 声明）
     * @param principal        登录主体快照
     * @param clientPolicy     客户端 Refresh Token 策略
     * @param securityVersion  创建会话时固化的安全版本
     * @param response         当前响应，COOKIE 模式下写入 HttpOnly Cookie
     * @return BODY 模式需要返回给客户端的明文 Token；COOKIE 模式返回值仅供内部使用
     */
    String issue(String sessionId, RefreshSessionPrincipal principal,
        RefreshClientPolicy clientPolicy, AuthSecurityVersion securityVersion,
        HttpServletResponse response);

    /**
     * 原子轮换 Refresh Token。
     *
     * <p>旧令牌作废、签发新令牌，切换由指纹门禁原子完成；宽限期内同一旧令牌的并发或
     * 重复请求幂等返回首次轮换结果。宽限期外的上一代令牌属于明确重放，整个登录会话
     * 将被撤销。</p>
     *
     * @param rawRefreshToken   客户端提交的明文 Refresh Token
     * @param response          当前响应
     * @param accessTokenIssuer 根据旧会话重新签发 Access Token 的函数
     * @return 签发结果
     */
    RefreshIssueResult rotate(String rawRefreshToken, HttpServletResponse response,
        Function<RefreshSession, IssuedAccessToken> accessTokenIssuer);

    /**
     * 完整执行一次安全的刷新端点流程（四步安全契约固化版）。
     *
     * <p>本方法是 {@code resolve → checkRequestRateLimit → validateRequest → rotate}
     * 四步安全钩子的固定编排，漏调用任何一步（限流绕过 / 跨站伪造刷新）都会导致
     * 防护静默失效，因此业务刷新端点<b>应当调用本方法</b>，把顺序交由框架保证；
     * 四个原子方法仅保留给需要自定义编排的场景。</p>
     *
     * @param bodyRefreshToken  BODY 模式提交的明文 Refresh Token，可为空
     * @param request           当前请求
     * @param response          当前响应
     * @param accessTokenIssuer 根据旧会话重新签发 Access Token 的函数
     * @return 签发结果
     */
    default RefreshIssueResult refresh(String bodyRefreshToken, HttpServletRequest request,
        HttpServletResponse response,
        Function<RefreshSession, IssuedAccessToken> accessTokenIssuer) {
        String rawRefreshToken = this.resolve(bodyRefreshToken, request);
        this.checkRequestRateLimit(request);
        this.validateRequest(rawRefreshToken, request);
        return this.rotate(rawRefreshToken, response, accessTokenIssuer);
    }

    /**
     * 从 Cookie 或 BODY 中读取 Refresh Token；同时出现两种来源时拒绝请求。
     *
     * @param bodyRefreshToken BODY 模式提交的明文 Refresh Token，可为空
     * @param request          当前请求，用于读取 Cookie
     * @return 提取到的明文 Refresh Token，两种来源均不存在时返回 {@code null}
     */
    String resolve(String bodyRefreshToken, HttpServletRequest request);

    /**
     * 在解析不可信 Token 之前按可信客户端地址执行刷新限流。
     *
     * @param request 当前请求
     */
    void checkRequestRateLimit(HttpServletRequest request);

    /**
     * 校验 Cookie 模式请求来源，防止跨站请求伪造刷新或退出当前登录。
     *
     * @param rawRefreshToken 客户端提交的明文 Refresh Token
     * @param request         当前请求
     */
    void validateRequest(String rawRefreshToken, HttpServletRequest request);

    /**
     * 请求携带 Refresh Token Cookie 时，在解析 Cookie 前校验请求来源。
     *
     * @param request 当前请求
     */
    void validateCookieOrigin(HttpServletRequest request);

    /**
     * 撤销当前 Access Token 对应的 Refresh Session。
     *
     * @param accessToken  当前 Access Token
     * @param refreshToken 当前 Refresh Token
     */
    void revokeCurrent(String accessToken, String refreshToken);

    /**
     * 撤销用户的全部 Refresh Session。
     *
     * @param userId 用户 ID
     */
    void revokeByUser(Long userId);

    /**
     * 撤销租户的全部 Refresh Session。
     *
     * @param tenantId 租户 ID
     */
    void revokeByTenant(Long tenantId);

    /**
     * 撤销客户端的全部 Refresh Session。
     *
     * @param clientId 客户端 ID
     */
    void revokeByClient(String clientId);

    /**
     * 查询有效登录会话；tenantId 为空时查询全部租户。
     *
     * @param tenantId 租户 ID，为空时查询全部租户
     * @return 有效登录会话列表
     */
    List<RefreshSession> listSessions(Long tenantId);

    /**
     * 查询指定有效登录会话。
     *
     * @param sessionId 会话 ID
     * @return 有效登录会话，不存在或已失效时返回 {@code null}
     */
    RefreshSession getSession(String sessionId);

    /**
     * 撤销指定 Refresh Session。
     *
     * @param sessionId 会话 ID
     */
    void revokeBySessionId(String sessionId);

    /**
     * 清理浏览器 Refresh Token Cookie。
     *
     * @param response 当前响应
     */
    void clearCookie(HttpServletResponse response);

    /**
     * 已在策略锁内完成最终状态复查的一次登录尝试。
     *
     * @param userId              最终确认的用户 ID
     * @param clientPolicy        最终确认的客户端配置
     * @param currentAccessToken  当前请求携带的 Access Token
     * @param currentRefreshToken 当前请求携带的 Refresh Token
     * @param issuer              新令牌签发函数
     */
    record LoginAttempt<T>(Long userId, RefreshClientPolicy clientPolicy, String currentAccessToken,
        String currentRefreshToken, Supplier<T> issuer) {
    }
}
