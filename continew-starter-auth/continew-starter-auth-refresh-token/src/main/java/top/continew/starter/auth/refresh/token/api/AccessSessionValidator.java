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
 * 校验 Access Token 是否仍绑定有效认证会话。
 *
 * @author luoqiz
 * @since 2.17.0
 */
public interface AccessSessionValidator {

    /**
     * 会话失效原因提示。
     *
     * @param accessToken Access Token
     * @return 失效提示；null 表示会话仍然有效
     */
    String getInvalidReason(String accessToken);

    /**
     * 判断 Access Token 绑定的认证会话是否已经失效。
     *
     * @param accessToken Access Token
     * @return 已失效返回 true
     */
    default boolean isInvalid(String accessToken) {
        return this.getInvalidReason(accessToken) != null;
    }
}
