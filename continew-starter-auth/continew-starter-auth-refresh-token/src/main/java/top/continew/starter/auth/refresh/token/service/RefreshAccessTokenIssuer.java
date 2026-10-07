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

import top.continew.starter.auth.refresh.token.model.IssuedAccessToken;
import top.continew.starter.auth.refresh.token.model.RefreshSession;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 刷新时重新装载主体状态并签发 Access Token 的业务适配点。
 *
 * <p>认证会话模块不依赖用户、客户端和租户实体；由业务系统实现本接口，确保刷新时
 * 使用最新的权限和业务状态。实现应在签发前完成账号禁用、凭证变更等状态复查，复查
 * 失败时抛出业务异常终止轮换。</p>
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.17.0
 */
public interface RefreshAccessTokenIssuer {

    /**
     * 根据旧会话重新签发 Access Token。
     *
     * @param session  旧会话
     * @param request  当前请求
     * @param response 当前响应
     * @return 签发结果
     */
    IssuedAccessToken issue(RefreshSession session, HttpServletRequest request,
        HttpServletResponse response);
}
