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

package top.continew.starter.openapi.spi;

import top.continew.starter.openapi.model.OpenApiCredential;

import java.util.Optional;

/**
 * 凭证查询 SPI
 *
 * <p>由使用方实现：按 accessKey 查询业务存储（如应用表）并返回凭证与应用状态。
 * 使用方定义同名 Bean（标注 {@code @ConditionalOnMissingBean} 的默认实现会自动退位）。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@FunctionalInterface
public interface CredentialResolver {

    /**
     * 按 accessKey 查询凭证
     *
     * @param accessKey 应用访问标识
     * @return 凭证信息，查不到时返回空（验签链路将拒绝请求）
     */
    Optional<OpenApiCredential> resolve(String accessKey);
}
