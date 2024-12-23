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

package top.continew.starter.extension.tenant.handler;

/**
 * 租户处理器
 *
 * @author 小熊
 * @since 2.8.0
 */
public interface TenantHandler {

    /**
     * 在指定租户中执行
     *
     * @param tenantId 租户 ID
     * @param runnable 方法
     */
    void execute(Long tenantId, Runnable runnable);
}
