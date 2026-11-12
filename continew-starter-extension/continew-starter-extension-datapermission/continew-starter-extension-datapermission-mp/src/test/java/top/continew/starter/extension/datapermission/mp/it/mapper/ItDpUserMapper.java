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

package top.continew.starter.extension.datapermission.mp.it.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import top.continew.starter.extension.datapermission.mp.it.entity.ItDpUser;

import java.util.List;

/**
 * 测试用户 Mapper
 *
 * <p>{@code @DataPermission} 注解按 Mapper 方法粒度生效：带注解方法的查询语句被
 * {@code DefaultDataPermissionHandler} 按 mappedStatementId 反射定位注解并改写 WHERE，
 * 无注解方法保持原样。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public interface ItDpUserMapper extends BaseMapper<ItDpUser> {

    /**
     * 按数据权限注解查询用户列表。
     *
     * <p>部门子查询落到 {@code dp_it_dept}，自定义范围子查询落到 {@code dp_it_role_dept}。</p>
     *
     * @return 经数据权限改写后的真实用户列表
     */
    @Select("SELECT * FROM dp_it_user")
    @DataPermission(deptTableAlias = "dp_it_dept", roleDeptTableAlias = "dp_it_role_dept")
    List<ItDpUser> selectPermissionUsers();

    /**
     * 查询全部用户（无数据权限注解，语句保持原样）。
     *
     * @return 全量用户列表
     */
    @Select("SELECT * FROM dp_it_user")
    List<ItDpUser> selectUnrestrictedUsers();
}
