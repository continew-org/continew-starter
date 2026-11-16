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

package top.continew.starter.extension.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.hutool.core.lang.tree.Tree;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.continew.starter.extension.crud.annotation.CrudApi;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.req.IdsReq;
import top.continew.starter.extension.crud.model.resp.BasePageResp;
import top.continew.starter.extension.crud.model.resp.IdResp;
import top.continew.starter.extension.crud.model.resp.LabelValueResp;
import top.continew.starter.extension.crud.service.CrudService;

import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.List;

/**
 * CRUD 控制器抽象基类测试
 *
 * <p>
 * 直连端点方法，验证各端点对 {@link CrudService} 的参数转发与返回值透传；{@code tree} 与
 * {@code treeDict} 共用服务端 {@code tree} 方法，以 {@code isSimple} 取值区分调用场景。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AbstractCrudControllerTest {

    private CrudService<String, Info, Query, Req> service;

    private TestCrudController controller;

    @BeforeEach
    void setUp() {
        this.service = mock(CrudService.class);
        this.controller = new TestCrudController(this.service);
    }

    @Test
    void shouldDelegatePageToService() {
        BasePageResp<String> resp = new BasePageResp<>(List.of("row"), 1);
        when(this.service.page(any(), any())).thenReturn(resp);
        Query query = new Query();
        PageQuery pageQuery = new PageQuery(2, 5);
        assertSame(resp, this.controller.page(query, pageQuery));
        verify(this.service).page(same(query), same(pageQuery));
    }

    @Test
    void shouldDelegateListToService() {
        List<String> rows = List.of("row");
        when(this.service.list(any(), any())).thenReturn(rows);
        Query query = new Query();
        SortQuery sortQuery = new SortQuery("id,asc");
        assertSame(rows, this.controller.list(query, sortQuery));
        verify(this.service).list(same(query), same(sortQuery));
    }

    @Test
    void shouldDelegateTreeWithComplexStructure() {
        List<Tree<Long>> rows = List.of();
        when(this.service.tree(any(), any(), anyBoolean())).thenReturn(rows);
        Query query = new Query();
        SortQuery sortQuery = new SortQuery();
        assertSame(rows, this.controller.tree(query, sortQuery));
        verify(this.service).tree(same(query), same(sortQuery), eq(false));
    }

    @Test
    void shouldDelegateGetByIdToService() {
        Info detail = new Info();
        when(this.service.get(9L)).thenReturn(detail);
        assertSame(detail, this.controller.get(9L));
        verify(this.service).get(9L);
    }

    @Test
    void shouldWrapCreatedIdIntoIdResp() {
        Req req = new Req();
        when(this.service.create(req)).thenReturn(100L);
        IdResp<Long> resp = this.controller.create(req);
        assertEquals(100L, resp.getId().longValue());
        verify(this.service).create(same(req));
    }

    @Test
    void shouldDelegateUpdateToService() {
        Req req = new Req();
        this.controller.update(req, 3L);
        verify(this.service).update(same(req), eq(3L));
    }

    @Test
    void shouldDelegateSingleDeleteAsSingletonIdList() {
        this.controller.delete(7L);
        verify(this.service).delete(List.of(7L));
    }

    @Test
    void shouldDelegateBatchDeleteWithRequestIds() {
        List<Long> ids = List.of(1L, 2L, 3L);
        IdsReq req = new IdsReq();
        req.setIds(ids);
        this.controller.batchDelete(req);
        verify(this.service).delete(same(ids));
    }

    @Test
    void shouldDelegateExportToService() {
        HttpServletResponse response = mock(HttpServletResponse.class);
        Query query = new Query();
        SortQuery sortQuery = new SortQuery();
        this.controller.export(query, sortQuery, response);
        verify(this.service).export(same(query), same(sortQuery), same(response));
    }

    @Test
    void shouldDelegateDictToService() {
        List<LabelValueResp<String>> dict = List.of(new LabelValueResp<>("label", "value"));
        // mock 的泛型经擦除后 stub 参数为 raw List，doReturn 绕过泛型类型检查
        doReturn(dict).when(this.service).dict(any(), any());
        Query query = new Query();
        SortQuery sortQuery = new SortQuery();
        assertSame(dict, this.controller.dict(query, sortQuery));
        verify(this.service).dict(same(query), same(sortQuery));
    }

    @Test
    void shouldDelegateTreeDictWithSimpleStructure() {
        List<Tree<Long>> rows = List.of();
        when(this.service.tree(any(), any(), anyBoolean())).thenReturn(rows);
        Query query = new Query();
        SortQuery sortQuery = new SortQuery();
        assertSame(rows, this.controller.treeDict(query, sortQuery));
        verify(this.service).tree(same(query), same(sortQuery), eq(true));
    }

    static class Info {
    }

    static class Query {
    }

    static class Req {
    }

    static class TestCrudController extends
        AbstractCrudController<CrudService<String, Info, Query, Req>, String, Info, Query, Req> {

        TestCrudController(CrudService<String, Info, Query, Req> baseService) {
            this.baseService = baseService;
        }

        @Override
        public void preHandle(CrudApi crudApi, Object[] args, Method targetMethod,
            Class<?> targetClass) {
            // 单测直连端点方法，不经过 CRUD 切面，前置处理置空
        }
    }
}
