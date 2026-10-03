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

import top.continew.starter.auth.refresh.token.model.SessionView;

import java.util.List;

/**
 * 认证会话安全查询入口。
 *
 * <p>返回 {@link SessionView}，不暴露指纹、安全版本与轮换快照。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface SessionQueryService {

    /**
     * 查询指定有效登录会话。
     *
     * @param sessionId 会话 ID
     * @return 会话视图，不存在或已失效时返回 {@code null}
     */
    SessionView getSession(String sessionId);

    /**
     * 查询有效登录会话。
     *
     * @param tenantId 租户 ID，为空时查询全部租户
     * @return 会话视图列表
     */
    List<SessionView> listSessions(Long tenantId);
}
