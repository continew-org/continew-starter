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

package top.continew.starter.extension.crud.constant;

/**
 * CRUD 查询分组常量
 *
 * <p>
 * 与 {@code @Query#groups} 配套使用：查询条件字段按分组声明查询类型后，CRUD 接口在
 * 构建查询条件时按自身所属分组选中对应声明。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class CrudQueryGroups {

    /**
     * 列表分组：分页列表（page）、列表（list）、树列表（tree）、字典列表（dict）共用。
     */
    public static final String LIST = "list";

    /**
     * 导出分组：导出（export）。
     */
    public static final String EXPORT = "export";

    private CrudQueryGroups() {
    }
}
