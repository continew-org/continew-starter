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

/**
 * 授权判定 SPI
 *
 * <p>验签通过只证明请求来自持密钥方，「有权访问哪些接口」须由使用方显式判定
 * （如查询应用-接口清单）。使用方定义同名 Bean 覆盖默认实现。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@FunctionalInterface
public interface AuthorizationChecker {

    /**
     * 判定应用是否有权访问该接口
     *
     * @param appId  应用 ID
     * @param method HTTP 方法（大写）
     * @param path   请求路径（不含 context-path）
     * @return 有权访问时返回 true
     */
    boolean check(Long appId, String method, String path);
}
