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

package top.continew.starter.auth.refresh.token.api;

/**
 * 认证会话撤销通知器。
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface AuthSessionRevocationNotifier {

    /**
     * 通知所有实时连接撤销指定登录会话。
     *
     * @param sessionId Refresh Session ID
     */
    void notifyRevoked(String sessionId);
}
