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

package top.continew.starter.data.mf.it.entity;

import com.mybatisflex.annotation.Column;
import com.mybatisflex.annotation.Id;
import com.mybatisflex.annotation.KeyType;
import com.mybatisflex.annotation.Table;

/**
 * 测试用户实体
 *
 * @author Charles7c
 * @since 3.1.0
 */
@Table("mf_it_user")
public class ItUser {

    /**
     * 主键（显式指定入库，保证断言确定性）
     */
    @Id(keyType = KeyType.None)
    private Long id;
    /**
     * 用户名
     */
    @Column("username")
    private String username;
    /**
     * 昵称
     */
    @Column("nickname")
    private String nickname;
    /**
     * 部门 ID
     */
    @Column("dept_id")
    private Long deptId;
    /**
     * 创建人（数据权限 SELF 范围比对列）
     */
    @Column("create_user")
    private Long createUser;
    /**
     * 逻辑删除标记位
     */
    @Column(isLogicDelete = true)
    private Integer deleted;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public Long getCreateUser() {
        return createUser;
    }

    public void setCreateUser(Long createUser) {
        this.createUser = createUser;
    }

    public Integer getDeleted() {
        return deleted;
    }

    public void setDeleted(Integer deleted) {
        this.deleted = deleted;
    }
}
