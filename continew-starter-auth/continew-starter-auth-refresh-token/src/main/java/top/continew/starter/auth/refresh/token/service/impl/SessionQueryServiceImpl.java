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

package top.continew.starter.auth.refresh.token.service.impl;

import top.continew.starter.auth.refresh.token.model.RefreshSession;
import top.continew.starter.auth.refresh.token.model.SessionView;
import top.continew.starter.auth.refresh.token.service.RefreshTokenService;
import top.continew.starter.auth.refresh.token.service.SessionQueryService;

import java.util.List;

/**
 * 认证会话查询服务实现。
 *
 * @author luoqiz
 * @since 2.17.0
 */
public class SessionQueryServiceImpl implements SessionQueryService {

    private final RefreshTokenService refreshTokenService;

    public SessionQueryServiceImpl(RefreshTokenService refreshTokenService) {
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    public SessionView getSession(String sessionId) {
        RefreshSession session = refreshTokenService.getSession(sessionId);
        return session == null ? null : SessionView.from(session);
    }

    @Override
    public List<SessionView> listSessions(Long tenantId) {
        return refreshTokenService.listSessions(tenantId).stream().map(SessionView::from).toList();
    }
}
