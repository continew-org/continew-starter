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

package top.continew.starter.extension.datapermission.filter;

import top.continew.starter.extension.datapermission.model.UserContext;

/**
 * 数据权限用户上下文提供者
 *
 * @author Charles7c
 * @since 1.1.0
 */
public interface DataPermissionUserContextProvider {

    /**
     * 是否过滤
     *
     * @return true：过滤；false：不过滤
     */
    boolean isFilter();

    /**
     * 获取用户上下文
     *
     * @return 用户上下文
     */
    UserContext getUserContext();
}
