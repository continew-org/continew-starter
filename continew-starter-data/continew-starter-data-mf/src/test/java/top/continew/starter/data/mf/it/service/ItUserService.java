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

package top.continew.starter.data.mf.it.service;

import com.mybatisflex.core.query.QueryWrapper;
import top.continew.starter.data.datapermission.DataPermission;
import top.continew.starter.data.mf.it.entity.ItUser;
import top.continew.starter.data.mf.it.mapper.ItUserMapper;
import top.continew.starter.data.service.impl.ServiceImpl;

import java.util.List;

/**
 * 测试用户业务实现
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class ItUserService extends ServiceImpl<ItUserMapper, ItUser> {

    /**
     * 按数据权限注解查询用户列表。
     *
     * <p>本方法触发真实 AOP 切面（{@code DataPermissionAspect}）与方言改写链路：
     * 部门子查询落到 {@code mf_it_dept}，自定义范围子查询落到 {@code mf_it_role_dept}。</p>
     *
     * @param wrapper 查询条件
     * @return 经数据权限改写后的真实用户列表
     */
    @DataPermission(deptTableAlias = "mf_it_dept", roleDeptTableAlias = "mf_it_role_dept")
    public List<ItUser> listWithPermission(QueryWrapper wrapper) {
        return list(wrapper);
    }
}
