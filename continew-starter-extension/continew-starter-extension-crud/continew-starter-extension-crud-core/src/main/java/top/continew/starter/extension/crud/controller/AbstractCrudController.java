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

import cn.hutool.core.lang.tree.Tree;
import com.feiniaojin.gracefulresponse.api.ExcludeFromGracefulResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import top.continew.starter.extension.crud.annotation.CrudApi;
import top.continew.starter.extension.crud.enums.Api;
import top.continew.starter.extension.crud.handler.CrudApiHandler;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.req.IdsReq;
import top.continew.starter.extension.crud.model.resp.IdResp;
import top.continew.starter.extension.crud.model.resp.BasePageResp;
import top.continew.starter.extension.crud.model.resp.LabelValueResp;
import top.continew.starter.extension.crud.service.CrudService;
import top.continew.starter.extension.crud.validation.CrudValidationGroup;

import java.util.List;

/**
 * CRUD 控制器抽象基类
 *
 * @param <S> 业务接口
 * @param <L> 列表类型
 * @param <D> 详情类型
 * @param <Q> 查询条件类型
 * @param <C> 创建或修改请求参数类型
 * @author Charles7c
 * @since 1.0.0
 */
public abstract class AbstractCrudController<S extends CrudService<L, D, Q, C>, L, D, Q, C>
    implements CrudApiHandler {

    // 抽象模板基类供下游控制器继承，泛型服务由具体子类决定；沿用字段注入避免强制所有子类声明构造器
    @SuppressWarnings("java:S6813")
    @Autowired
    protected S baseService;

    /**
     * 分页查询列表
     *
     * @param query     查询条件
     * @param pageQuery 分页查询条件
     * @return 分页信息
     */
    @CrudApi(Api.PAGE)
    @Operation(summary = "分页查询列表", description = "分页查询列表")
    @ResponseBody
    @GetMapping
    public BasePageResp<L> page(@Valid Q query, @Valid PageQuery pageQuery) {
        return baseService.page(query, pageQuery);
    }

    /**
     * 查询列表
     *
     * @param query     查询条件
     * @param sortQuery 排序查询条件
     * @return 列表信息
     */
    @CrudApi(Api.LIST)
    @Operation(summary = "查询列表", description = "查询列表")
    @ResponseBody
    @GetMapping("/list")
    public List<L> list(@Valid Q query, @Valid SortQuery sortQuery) {
        return baseService.list(query, sortQuery);
    }

    /**
     * 查询树列表
     *
     * @param query     查询条件
     * @param sortQuery 排序查询条件
     * @return 树列表信息
     */
    @CrudApi(Api.TREE)
    @Operation(summary = "查询树列表", description = "查询树列表")
    @ResponseBody
    @GetMapping("/tree")
    public List<Tree<Long>> tree(@Valid Q query, @Valid SortQuery sortQuery) {
        return baseService.tree(query, sortQuery, false);
    }

    /**
     * 查询详情
     *
     * @param id ID
     * @return 详情信息
     */
    @CrudApi(Api.GET)
    @Operation(summary = "查询详情", description = "查询详情")
    @Parameter(name = "id", description = "ID", example = "1", in = ParameterIn.PATH)
    @ResponseBody
    @GetMapping("/{id}")
    public D get(@PathVariable("id") Long id) {
        return baseService.get(id);
    }

    /**
     * 创建
     *
     * @param req 创建请求参数
     * @return ID
     */
    @CrudApi(Api.CREATE)
    @Operation(summary = "创建数据", description = "创建数据")
    @ResponseBody
    @PostMapping
    @Validated(CrudValidationGroup.Create.class)
    public IdResp<Long> create(@RequestBody @Valid C req) {
        return new IdResp<>(baseService.create(req));
    }

    /**
     * 修改
     *
     * @param req 修改请求参数
     * @param id  ID
     */
    @CrudApi(Api.UPDATE)
    @Operation(summary = "修改数据", description = "修改数据")
    @Parameter(name = "id", description = "ID", example = "1", in = ParameterIn.PATH)
    @ResponseBody
    @PutMapping("/{id}")
    @Validated(CrudValidationGroup.Update.class)
    public void update(@RequestBody @Valid C req, @PathVariable("id") Long id) {
        baseService.update(req, id);
    }

    /**
     * 删除
     *
     * @param id ID
     */
    @CrudApi(Api.DELETE)
    @Operation(summary = "删除数据", description = "删除数据")
    @Parameter(name = "id", description = "ID", example = "1", in = ParameterIn.PATH)
    @ResponseBody
    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        baseService.delete(List.of(id));
    }

    /**
     * 批量删除
     *
     * @param req 删除请求参数
     */
    @CrudApi(Api.BATCH_DELETE)
    @Operation(summary = "批量删除数据", description = "批量删除数据")
    @ResponseBody
    @DeleteMapping
    public void batchDelete(@RequestBody @Valid IdsReq req) {
        baseService.delete(req.getIds());
    }

    /**
     * 导出
     *
     * @param query     查询条件
     * @param sortQuery 排序查询条件
     * @param response  响应对象
     */
    @CrudApi(Api.EXPORT)
    @ExcludeFromGracefulResponse
    @Operation(summary = "导出数据", description = "导出数据")
    @GetMapping("/export")
    public void export(@Valid Q query, @Valid SortQuery sortQuery, HttpServletResponse response) {
        baseService.export(query, sortQuery, response);
    }

    /**
     * 查询字典列表
     *
     * @param query     查询条件
     * @param sortQuery 排序查询条件
     * @return 字典列表信息
     */
    @CrudApi(Api.DICT)
    @Operation(summary = "查询字典列表", description = "查询字典列表（下拉选项等场景）")
    @GetMapping("/dict")
    public List<LabelValueResp> dict(@Valid Q query, @Valid SortQuery sortQuery) {
        return baseService.dict(query, sortQuery);
    }

    /**
     * 查询树型字典列表
     *
     * @param query     查询条件
     * @param sortQuery 排序查询条件
     * @return 树型字典列表信息
     */
    @CrudApi(Api.TREE_DICT)
    @Operation(summary = "查询树型字典列表", description = "查询树型结构字典列表（树型结构下拉选项等场景）")
    @GetMapping("/dict/tree")
    public List<Tree<Long>> treeDict(@Valid Q query, @Valid SortQuery sortQuery) {
        return baseService.tree(query, sortQuery, true);
    }
}
