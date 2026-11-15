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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import top.continew.starter.core.exception.BadRequestException;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.annotation.Query;
import top.continew.starter.data.enums.QueryType;
import top.continew.starter.data.mapper.BaseMapper;
import top.continew.starter.extension.crud.annotation.DictModel;
import top.continew.starter.extension.crud.annotation.TreeField;
import top.continew.starter.extension.crud.autoconfigure.CrudProperties;
import top.continew.starter.extension.crud.model.entity.BaseIdDO;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.resp.LabelValueResp;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link CrudServiceImpl} CRUD 业务实现基类单元测试。
 *
 * <p>基于 Mockito 模拟 Mapper（不起真实数据库），覆盖分页、查询、详情、新增、修改、删除、导出、字典与树构建主流程及边界路径。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CrudServiceImplTest {

    private TestMapper mapper;
    private TestCrudServiceImpl service;

    /**
     * Validator/CheckUtils/ValidationUtils 的静态初始化经 SpringUtil 定位 jakarta Validator，
     * 测试侧用最小上下文满足该约定（与 RefreshTokenRedisSupport 先例一致）。
     */
    @BeforeAll
    static void setUpSpringContext() {
        if (SpringUtil.getApplicationContext() != null) {
            return;
        }
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.registerBean(SpringUtil.class);
        context.registerBean(jakarta.validation.Validator.class, LocalValidatorFactoryBean::new);
        context.refresh();
    }

    @BeforeEach
    void setUp() {
        mapper = mock(TestMapper.class);
        service = new TestCrudServiceImpl(mapper);
    }

    /**
     * 分页应转换记录类型、保留总数、按页参调用 Mapper 并逐条触发 fill 钩子。
     */
    @Test
    void shouldPageAndConvertRecordsWithFillHook() {
        Page<TestDO> page = new Page<>(2, 5, 12);
        page.setRecords(List.of(createEntity(1L, "alice"), createEntity(2L, "bob")));
        when(mapper.selectPage(any(), any())).thenReturn(page);

        PageResp<TestListVO> pageResp = service.page(null, new PageQuery(2, 5));

        assertThat(pageResp.getTotal()).isEqualTo(12L);
        assertThat(pageResp.getList()).extracting(TestListVO::getName).containsExactly("alice",
            "bob");
        assertThat(service.fillCalls).containsExactly("alice", "bob");
        verify(mapper).selectPage(argThat(p -> p.getCurrent() == 2L && p.getSize() == 5L), any());
    }

    /**
     * 空结果分页应返回空列表且不触发 fill 钩子。
     */
    @Test
    void shouldReturnEmptyPageWhenNoRecords() {
        Page<TestDO> page = new Page<>(1, 10, 0);
        page.setRecords(List.of());
        when(mapper.selectPage(any(), any())).thenReturn(page);

        PageResp<TestListVO> pageResp = service.page(null, new PageQuery());

        assertThat(pageResp.getTotal()).isZero();
        assertThat(pageResp.getList()).isEmpty();
        assertThat(service.fillCalls).isEmpty();
    }

    /**
     * 列表查询应转换为目标列表类型并逐条触发 fill 钩子。
     */
    @Test
    void shouldListAndConvertToTargetType() {
        when(mapper.selectList(any())).thenReturn(List.of(createEntity(1L, "alice")));

        List<TestListVO> list = service.list((TestQuery) null, (SortQuery) null);

        assertThat(list).extracting(TestListVO::getName).containsExactly("alice");
        assertThat(service.fillCalls).containsExactly("alice");
    }

    /**
     * 查询条件对象字段应经 @Query 注解拼入 QueryWrapper（LIKE 条件 + 列名）。
     */
    @Test
    void shouldBuildQueryWrapperFromQueryFields() {
        when(mapper.selectList(any())).thenReturn(List.of());
        TestQuery query = new TestQuery();
        query.setName("ali");

        service.list(query, null);

        verify(mapper).selectList(argThat(wrapper -> {
            String sqlSegment = wrapper.getSqlSegment();
            return sqlSegment.contains("LIKE") && sqlSegment.contains("name");
        }));
    }

    /**
     * 合法排序条件应拼入 ORDER BY（含多字段）。
     */
    @Test
    void shouldApplySortToQueryWrapper() {
        when(mapper.selectList(any())).thenReturn(List.of());

        service.list(null, new SortQuery("name,asc", "id,desc"));

        verify(mapper).selectList(argThat(wrapper -> {
            String sqlSegment = wrapper.getSqlSegment();
            return sqlSegment.contains("ORDER BY") && sqlSegment.contains("name") && sqlSegment
                .contains("id");
        }));
    }

    /**
     * 排序字段含非标识符字符时应抛出 400 异常且不触发查询。
     */
    @Test
    void shouldThrowWhenSortPropertyIsNotIdentifier() {
        SortQuery sortQuery = new SortQuery("na me,asc", "id,desc");

        assertThatThrownBy(() -> service.list(null, sortQuery))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("na me");
        verify(mapper, never()).selectList(any());
    }

    /**
     * 排序字段不是实体字段时应抛出 400 异常且不触发查询。
     */
    @Test
    void shouldThrowWhenSortPropertyNotInEntityFields() {
        SortQuery sortQuery = new SortQuery("nickname,asc", "id,desc");

        assertThatThrownBy(() -> service.list(null, sortQuery))
            .isInstanceOf(BadRequestException.class)
            .hasMessageContaining("nickname");
        verify(mapper, never()).selectList(any());
    }

    /**
     * 排序字段注入载荷必须在进入 ORDER BY 拼接前被拒绝：命中 SQL 注入黑名单的载荷由
     * {@code SqlInjectionUtils.check} 拦下，绕过黑名单的载荷由 {@code SortUtils}
     * 标识符白名单兜底，与 SortQueryTest 固化的「入口即拒绝」不变量在本服务入口等价。
     *
     * @param payload 排序字段注入载荷
     */
    @ParameterizedTest
    @ValueSource(strings = {"id;drop table sys_user,asc", "id --,asc", "id/*x*/,asc",
        "(select 1 from dual)#x.id,desc", "if(1=1,id,name),asc", "id' ,asc", "id asc,desc"})
    void shouldRejectOrderByInjectionPayloads(String payload) {
        SortQuery sortQuery = new SortQuery(payload, "id,asc");

        assertThatThrownBy(() -> service.list(null, sortQuery))
            .isInstanceOf(BadRequestException.class);
        verify(mapper, never()).selectList(any());
    }

    /**
     * 详情查询应转换为目标详情类型。
     */
    @Test
    void shouldGetAndConvertToDetailType() {
        when(mapper.selectById(1L)).thenReturn(createEntity(1L, "alice"));

        TestDetailVO detail = service.get(1L);

        assertThat(detail).isNotNull();
        assertThat(detail.getName()).isEqualTo("alice");
    }

    /**
     * 详情对应实体不存在时当前实现不做存在性校验，锁定返回 null 行为。
     */
    @Test
    void shouldReturnNullWhenDetailEntityMissing() {
        when(mapper.selectById(404L)).thenReturn(null);

        assertThat(service.get(404L)).isNull();
    }

    /**
     * 新增应拷贝请求参数到实体、回填自增 ID 并按序触发前后钩子。
     */
    @Test
    void shouldCreateCopyReqAndReturnGeneratedId() {
        TestCreateReq req = new TestCreateReq();
        req.setName("alice");
        doAnswer(invocation -> {
            TestDO entity = invocation.getArgument(0);
            entity.setId(99L);
            return 1;
        }).when(mapper).insert(any(TestDO.class));

        Long id = service.create(req);

        assertThat(id).isEqualTo(99L);
        ArgumentCaptor<TestDO> entityCaptor = ArgumentCaptor.forClass(TestDO.class);
        verify(mapper).insert(entityCaptor.capture());
        assertThat(entityCaptor.getValue().getName()).isEqualTo("alice");
        assertThat(service.hookCalls).containsExactly("beforeCreate", "afterCreate:99");
    }

    /**
     * 插入失败时应向后传播异常且不触发 afterCreate 钩子。
     */
    @Test
    void shouldSkipAfterCreateHookWhenInsertFails() {
        TestCreateReq req = new TestCreateReq();
        req.setName("alice");
        doThrow(new RuntimeException("insert failed")).when(mapper).insert(any(TestDO.class));

        assertThatThrownBy(() -> service.create(req)).isInstanceOf(RuntimeException.class)
            .hasMessage("insert failed");
        assertThat(service.hookCalls).containsExactly("beforeCreate");
    }

    /**
     * 修改应在既有实体实例上拷贝非空字段并原实例更新。
     */
    @Test
    void shouldUpdateCopyNonNullFieldsOnExistingEntity() {
        TestDO existing = createEntity(5L, "old");
        when(mapper.selectById(5L)).thenReturn(existing);
        TestCreateReq req = new TestCreateReq();
        req.setName("new");

        service.update(req, 5L);

        ArgumentCaptor<TestDO> entityCaptor = ArgumentCaptor.forClass(TestDO.class);
        verify(mapper).updateById(entityCaptor.capture());
        assertThat(entityCaptor.getValue()).isSameAs(existing);
        assertThat(existing.getName()).isEqualTo("new");
        assertThat(service.hookCalls).containsExactly("beforeUpdate", "afterUpdate");
    }

    /**
     * 修改请求参数中的 null 字段应被忽略，不覆盖既有值。
     */
    @Test
    void shouldUpdateIgnoreNullFieldsInReq() {
        TestDO existing = createEntity(5L, "old");
        when(mapper.selectById(5L)).thenReturn(existing);
        TestCreateReq req = new TestCreateReq();
        req.setDescription("d1");

        service.update(req, 5L);

        assertThat(existing.getName()).isEqualTo("old");
        assertThat(existing.getDescription()).isEqualTo("d1");
    }

    /**
     * 修改不存在的记录应抛出业务异常且不触发更新。
     */
    @Test
    void shouldThrowWhenUpdateEntityMissing() {
        when(mapper.selectById(6L)).thenReturn(null);
        TestCreateReq req = new TestCreateReq();

        assertThatThrownBy(() -> service.update(req, 6L)).isInstanceOf(BusinessException.class)
            .hasMessageContaining("6");
        verify(mapper, never()).updateById(any(TestDO.class));
        assertThat(service.hookCalls).containsExactly("beforeUpdate");
    }

    /**
     * 删除应按 ID 列表委托 Mapper 并按序触发前后钩子。
     */
    @Test
    void shouldDeleteByIdsAndFireHooks() {
        service.delete(List.of(1L, 2L));

        verify(mapper).deleteByIds(List.of(1L, 2L));
        assertThat(service.hookCalls).containsExactly("beforeDelete", "afterDelete");
    }

    /**
     * 删除 Mapper 失败时应向后传播异常且不触发 afterDelete 钩子。
     */
    @Test
    void shouldSkipAfterDeleteHookWhenMapperFails() {
        doThrow(new RuntimeException("db down")).when(mapper).deleteByIds(anyCollection());
        List<Long> ids = List.of(1L);

        assertThatThrownBy(() -> service.delete(ids)).isInstanceOf(RuntimeException.class)
            .hasMessage("db down");
        assertThat(service.hookCalls).containsExactly("beforeDelete");
        verify(mapper).deleteByIds(ids);
    }

    /**
     * 导出应经详情类型转换后写入响应流（真实 xlsx 字节 + 响应头）。
     */
    @Test
    void shouldExportDetailListToResponse() {
        when(mapper.selectList(any())).thenReturn(List.of(createEntity(1L, "alice")));
        MockHttpServletResponse response = new MockHttpServletResponse();

        service.export(null, null, response);

        assertThat(response.getContentAsByteArray()).isNotEmpty();
        assertThat(response.getHeader("Content-disposition")).contains(".xlsx");
        assertThat(response.getContentType()).contains("spreadsheetml");
    }

    /**
     * 泛型参数应从父类解析：列表/详情/查询类型与查询字段。
     */
    @Test
    void shouldResolveTypeArgumentsFromGenericSuperclass() {
        assertThat(service.getListClass()).isEqualTo(TestListVO.class);
        assertThat(service.getDetailClass()).isEqualTo(TestDetailVO.class);
        assertThat(service.getQueryClass()).isEqualTo(TestQuery.class);
        assertThat(service.getQueryFields()).extracting(Field::getName).contains("name");
    }

    /**
     * 实体缺少 @DictModel 注解时应抛出业务异常且不触发查询。
     */
    @Test
    void shouldThrowWhenDictModelAnnotationMissing() {
        assertThatThrownBy(() -> service.dict(null, null)).isInstanceOf(BusinessException.class)
            .hasMessageContaining("@DictModel");
        verify(mapper, never()).selectList(any());
    }

    /**
     * 带 @DictModel 的实体应构建 label/value 并解析额外字段映射。
     */
    @Test
    void shouldBuildDictWithLabelValueAndExtra() {
        DictTestMapper dictMapper = mock(DictTestMapper.class);
        DictCrudServiceImpl dictService = new DictCrudServiceImpl(dictMapper);
        DictTestDO entity = new DictTestDO();
        entity.setId(1L);
        entity.setName("alice");
        entity.setDescription("d1");
        when(dictMapper.selectList(any())).thenReturn(List.of(entity));

        List<LabelValueResp> respList = dictService.dict(null, null);

        assertThat(respList).hasSize(1);
        LabelValueResp resp = respList.get(0);
        assertThat(resp.getLabel()).isEqualTo("alice");
        assertThat(resp.getValue()).isEqualTo(1L);
        assertThat((Map<String, Object>) (Map<?, ?>) resp.getExtra()).containsEntry("description",
            "d1");
    }

    /**
     * 树查询无数据时应直接返回空列表（早于 Spring 上下文依赖）。
     */
    @Test
    void shouldReturnEmptyTreeWhenNoData() {
        when(mapper.selectList(any())).thenReturn(List.of());

        assertThat(service.tree(null, null, true)).isEmpty();
    }

    /**
     * 单根模式应按 parentId 归组构建一棵树并映射 ID 与名称字段。
     */
    @Test
    void shouldBuildSingleRootTree() {
        TreeTestMapper treeMapper = mock(TreeTestMapper.class);
        TreeCrudServiceImpl treeService = new TreeCrudServiceImpl(treeMapper);
        when(treeMapper.selectList(any())).thenReturn(List.of(createTreeNode(1L, 0L, "Root", 1),
            createTreeNode(2L, 1L, "Child", 2)));

        List<Tree<Long>> tree;
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean(CrudProperties.class))
                .thenReturn(new CrudProperties());
            tree = treeService.tree(null, null, true, true);
        }

        assertThat(tree).hasSize(1);
        Tree<Long> root = tree.get(0);
        assertThat(root.getId()).isEqualTo(1L);
        assertThat(root.getName()).isEqualTo("Root");
        assertThat(root.getChildren()).hasSize(1);
        assertThat(root.getChildren().get(0).getId()).isEqualTo(2L);
        assertThat(root.getChildren().get(0).getName()).isEqualTo("Child");
    }

    /**
     * 多根模式应分别构建各根节点子树。
     */
    @Test
    void shouldBuildMultiRootTree() {
        TreeTestMapper treeMapper = mock(TreeTestMapper.class);
        TreeCrudServiceImpl treeService = new TreeCrudServiceImpl(treeMapper);
        when(treeMapper.selectList(any())).thenReturn(List.of(createTreeNode(1L, 0L, "Root1", 1),
            createTreeNode(2L, 0L, "Root2", 2), createTreeNode(3L, 1L, "ChildOfRoot1", 3)));

        List<Tree<Long>> tree;
        try (MockedStatic<SpringUtil> springUtilMock = mockStatic(SpringUtil.class)) {
            springUtilMock.when(() -> SpringUtil.getBean(CrudProperties.class))
                .thenReturn(new CrudProperties());
            tree = treeService.tree(null, null, true, false);
        }

        assertThat(tree).extracting(Tree::getId).containsExactlyInAnyOrder(1L, 2L);
        Tree<Long> root1 = tree.stream().filter(node -> Long.valueOf(1L).equals(node.getId()))
            .findFirst()
            .orElseThrow();
        assertThat(root1.getChildren()).hasSize(1);
        assertThat(root1.getChildren().get(0).getId()).isEqualTo(3L);
    }

    private TestDO createEntity(Long id, String name) {
        TestDO entity = new TestDO();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private TreeTestDO createTreeNode(Long id, Long parentId, String title, int sort) {
        TreeTestDO node = new TreeTestDO();
        node.setId(id);
        node.setParentId(parentId);
        node.setTitle(title);
        node.setSort(sort);
        return node;
    }

    /**
     * 最小测试实体
     */
    static class TestDO extends BaseIdDO {

        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 测试列表类型
     */
    static class TestListVO {

        private Long id;
        private String name;
        private String description;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 测试详情类型
     */
    static class TestDetailVO {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 测试查询条件类型
     */
    static class TestQuery {

        @Query(type = QueryType.LIKE)
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 测试创建或修改请求参数类型
     */
    static class TestCreateReq {

        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 主流程测试 Mapper
     */
    interface TestMapper extends BaseMapper<TestDO> {
    }

    /**
     * 主流程测试服务
     */
    static class TestCrudServiceImpl extends
        CrudServiceImpl<TestMapper, TestDO, TestListVO, TestDetailVO, TestQuery, TestCreateReq> {

        private final List<String> hookCalls = new ArrayList<>();
        private final List<String> fillCalls = new ArrayList<>();

        TestCrudServiceImpl(TestMapper baseMapper) {
            this.baseMapper = baseMapper;
        }

        @Override
        public Class<TestMapper> getMapperClass() {
            return TestMapper.class;
        }

        @Override
        protected void fill(Object obj) {
            if (obj instanceof TestListVO listVO) {
                fillCalls.add(listVO.getName());
            }
        }

        @Override
        protected void beforeCreate(TestCreateReq req) {
            hookCalls.add("beforeCreate");
        }

        @Override
        protected void afterCreate(TestCreateReq req, TestDO entity) {
            hookCalls.add("afterCreate:" + entity.getId());
        }

        @Override
        protected void beforeUpdate(TestCreateReq req, Long id) {
            hookCalls.add("beforeUpdate");
        }

        @Override
        protected void afterUpdate(TestCreateReq req, TestDO entity) {
            hookCalls.add("afterUpdate");
        }

        @Override
        protected void beforeDelete(List<Long> ids) {
            hookCalls.add("beforeDelete");
        }

        @Override
        protected void afterDelete(List<Long> ids) {
            hookCalls.add("afterDelete");
        }
    }

    /**
     * 带 @DictModel 映射的字典测试实体
     */
    @DictModel(labelKey = "name", valueKey = "id", extraKeys = {"description"})
    static class DictTestDO extends BaseIdDO {

        private String name;
        private String description;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    /**
     * 字典测试 Mapper
     */
    interface DictTestMapper extends BaseMapper<DictTestDO> {
    }

    /**
     * 字典测试服务
     */
    static class DictCrudServiceImpl extends
        CrudServiceImpl<DictTestMapper, DictTestDO, TestListVO, TestDetailVO, TestQuery, TestCreateReq> {

        DictCrudServiceImpl(DictTestMapper baseMapper) {
            this.baseMapper = baseMapper;
        }

        @Override
        public Class<DictTestMapper> getMapperClass() {
            return DictTestMapper.class;
        }
    }

    /**
     * 树测试实体
     */
    static class TreeTestDO extends BaseIdDO {

        private Long parentId;
        private String title;
        private Integer sort;

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }

    /**
     * 树测试列表类型（@TreeField 局部树结构映射）
     */
    @TreeField(value = "id", parentIdKey = "parentId", nameKey = "title", weightKey = "sort")
    static class TreeListVO {

        private Long id;
        private Long parentId;
        private String title;
        private Integer sort;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public Long getParentId() {
            return parentId;
        }

        public void setParentId(Long parentId) {
            this.parentId = parentId;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public Integer getSort() {
            return sort;
        }

        public void setSort(Integer sort) {
            this.sort = sort;
        }
    }

    /**
     * 树测试 Mapper
     */
    interface TreeTestMapper extends BaseMapper<TreeTestDO> {
    }

    /**
     * 树测试服务
     */
    static class TreeCrudServiceImpl extends
        CrudServiceImpl<TreeTestMapper, TreeTestDO, TreeListVO, TestDetailVO, TestQuery, TestCreateReq> {

        TreeCrudServiceImpl(TreeTestMapper baseMapper) {
            this.baseMapper = baseMapper;
        }

        @Override
        public Class<TreeTestMapper> getMapperClass() {
            return TreeTestMapper.class;
        }
    }
}
