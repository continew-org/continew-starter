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
 * 认证会话撤销回调。
 *
 * <p>会话被撤销（强退、顶下线、策略失效、轮换判定等全部路径）时逐个调用全部实现。
 * 典型用途：实时推送撤销消息（如 WebSocket）、清理会话关联的旁路数据（如活跃记录）。
 * 实现应幂等、快速返回；单个实现抛出异常只记日志，不影响其余实现与会话撤销本身。
 * 实现间默认无顺序保证，需要顺序时用 {@code @Order} 声明。</p>
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface AuthSessionRevocationNotifier {

    /**
     * 会话撤销回调
     *
     * @param sessionId Refresh Session ID
     */
    void notifyRevoked(String sessionId);
}
