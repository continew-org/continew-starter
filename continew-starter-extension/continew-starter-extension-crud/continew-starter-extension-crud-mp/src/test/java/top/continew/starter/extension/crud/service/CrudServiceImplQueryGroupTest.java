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

package top.continew.starter.extension.crud.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.junit.jupiter.api.Test;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;
import top.continew.starter.data.mapper.BaseMapper;
import top.continew.starter.data.util.QueryGroupContext;
import top.continew.starter.extension.crud.constant.CrudQueryGroups;
import top.continew.starter.extension.crud.model.entity.BaseIdDO;

/**
 * {@link CrudServiceImpl} 查询分组传递测试。
 *
 * <p>纯单元测试，不依赖数据库：验证场景入口绑定的分组标识（{@code QueryGroupContext}）
 * 能够传递到 {@code buildQueryWrapper}，并按 {@code @Query} 分组声明选中对应查询条件；
 * 未绑定分组时回退既有行为。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CrudServiceImplQueryGroupTest {

    private final DeptCrudService service = new DeptCrudService();

    /**
     * 列表场景应命中等值声明。
     */
    @Test
    void shouldBuildEqConditionForListGroup() {
        QueryWrapper<?> wrapper = QueryGroupContext.callWith(CrudQueryGroups.LIST, () -> service
            .buildQueryWrapper(new DeptQuery("dev")));
        assertTrue(wrapper.getSqlSegment().contains("name ="), wrapper.getSqlSegment());
    }

    /**
     * 导出场景应命中右模糊声明。
     */
    @Test
    void shouldBuildLikeRightConditionForExportGroup() {
        QueryWrapper<?> wrapper = QueryGroupContext.callWith(CrudQueryGroups.EXPORT, () -> service
            .buildQueryWrapper(new DeptQuery("dev")));
        assertTrue(wrapper.getSqlSegment().contains("name LIKE"), wrapper.getSqlSegment());
    }

    /**
     * 未绑定分组时应回退既有行为：仅分组声明的字段不参与条件构建。
     */
    @Test
    void shouldFallbackToLegacyBehaviorWithoutBoundGroup() {
        QueryWrapper<?> wrapper = service.buildQueryWrapper(new DeptQuery("dev"));
        assertFalse(String.valueOf(wrapper.getSqlSegment()).contains("name"));
    }

    /**
     * 分组上下文在查询构建后不应残留到当前线程。
     */
    @Test
    void shouldNotLeakGroupContextAfterBuild() {
        QueryGroupContext.callWith(CrudQueryGroups.LIST, () -> service.buildQueryWrapper(null));
        assertNull(QueryGroupContext.get());
    }

    /**
     * 查询条件载体：列表等值、导出右模糊。
     */
    static class DeptQuery {

        @Query(type = QueryType.EQ, groups = "list")
        @Query(type = QueryType.LIKE_RIGHT, groups = "export")
        private String name;

        DeptQuery(String name) {
            this.name = name;
        }
    }

    /**
     * 测试实体。
     */
    static class DeptDO extends BaseIdDO {
    }

    /**
     * 最小 CRUD 业务实现：仅用于验证分组传递链，不涉及 Mapper 调用。
     */
    static class DeptCrudService extends
        CrudServiceImpl<BaseMapper<DeptDO>, DeptDO, DeptDO, DeptDO, DeptQuery, DeptDO> {
    }
}
