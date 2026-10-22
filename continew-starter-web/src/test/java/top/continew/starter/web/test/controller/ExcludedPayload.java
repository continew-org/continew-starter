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

package top.continew.starter.web.test.controller;

import top.continew.starter.web.annotation.ExcludeFromGlobalResponse;

/**
 * 排除全局响应的返回类型
 *
 * @param value 响应数据
 * @author Charles7c
 * @since 3.0.0
 */
@ExcludeFromGlobalResponse
public record ExcludedPayload(String value) {
}
