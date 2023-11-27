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

package top.charles7c.continew.starter.extension.crud.base;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.charles7c.continew.starter.core.constant.StringConstants;
import top.charles7c.continew.starter.extension.crud.annotation.CrudRequestMapping;
import top.charles7c.continew.starter.extension.crud.annotation.NoResponseAdvice;
import top.charles7c.continew.starter.extension.crud.enums.Api;
import top.charles7c.continew.starter.extension.crud.model.query.PageQuery;
import top.charles7c.continew.starter.extension.crud.model.query.SortQuery;
import top.charles7c.continew.starter.extension.crud.model.resp.PageDataResp;
import top.charles7c.continew.starter.extension.crud.model.resp.R;

import java.util.List;

/**
 * 控制器基类
 *
 * @param <S>
 *            业务接口
 * @param <L>
 *            列表信息
 * @param <D>
 *            详情信息
 * @param <Q>
 *            查询条件
 * @param <C>
 *            创建或修改信息
 * @author Charles7c
 * @since 1.0.0
 */
@NoArgsConstructor
public abstract class BaseController<S extends BaseService<L, D, Q, C>, L, D, Q, C extends BaseReq> {

    @Autowired
    protected S baseService;

    /**
     * 分页查询列表
     *
     * @param query
     *            查询条件
     * @param pageQuery
     *            分页查询条件
     * @return 分页信息
     */
    @Operation(summary = "分页查询列表", description = "分页查询列表")
    @ResponseBody
    @GetMapping
    public PageDataResp<L> page(Q query, @Validated PageQuery pageQuery) {
        this.checkPermission(Api.LIST);
        return baseService.page(query, pageQuery);
    }

    /**
     * 查询树列表
     *
     * @param query
     *            查询条件
     * @param sortQuery
     *            排序查询条件
     * @return 树列表信息
     */
    @Operation(summary = "查询树列表", description = "查询树列表")
    @ResponseBody
    @GetMapping("/tree")
    public List<Tree<Long>> tree(Q query, SortQuery sortQuery) {
        this.checkPermission(Api.LIST);
        return baseService.tree(query, sortQuery, false);
    }

    /**
     * 查询列表
     *
     * @param query
     *            查询条件
     * @param sortQuery
     *            排序查询条件
     * @return 列表信息
     */
    @Operation(summary = "查询列表", description = "查询列表")
    @ResponseBody
    @GetMapping("/list")
    public List<L> list(Q query, SortQuery sortQuery) {
        this.checkPermission(Api.LIST);
        return baseService.list(query, sortQuery);
    }

    /**
     * 查看详情
     *
     * @param id
     *            ID
     * @return 详情信息
     */
    @Operation(summary = "查看详情", description = "查看详情")
    @Parameter(name = "id", description = "ID", example = "1", in = ParameterIn.PATH)
    @ResponseBody
    @GetMapping("/{id}")
    public D get(@PathVariable Long id) {
        this.checkPermission(Api.LIST);
        return baseService.get(id);
    }

    /**
     * 新增
     *
     * @param req
     *            创建信息
     * @return 自增 ID
     */
    @Operation(summary = "新增数据", description = "新增数据")
    @ResponseBody
    @PostMapping
    public R<Long> add(@Validated(ValidateGroup.Crud.Add.class) @RequestBody C req) {
        this.checkPermission(Api.ADD);
        Long id = baseService.add(req);
        return R.ok("新增成功", id);
    }

    /**
     * 修改
     *
     * @param req
     *            修改信息
     * @param id
     *            ID
     * @return /
     */
    @Operation(summary = "修改数据", description = "修改数据")
    @Parameter(name = "id", description = "ID", example = "1", in = ParameterIn.PATH)
    @ResponseBody
    @PutMapping("/{id}")
    public R update(@Validated(ValidateGroup.Crud.Update.class) @RequestBody C req, @PathVariable Long id) {
        this.checkPermission(Api.UPDATE);
        baseService.update(req, id);
        return R.ok("修改成功");
    }

    /**
     * 删除
     *
     * @param ids
     *            ID 列表
     * @return /
     */
    @Operation(summary = "删除数据", description = "删除数据")
    @Parameter(name = "ids", description = "ID 列表", example = "1,2", in = ParameterIn.PATH)
    @ResponseBody
    @DeleteMapping("/{ids}")
    public R delete(@PathVariable List<Long> ids) {
        this.checkPermission(Api.DELETE);
        baseService.delete(ids);
        return R.ok("删除成功");
    }

    /**
     * 导出
     *
     * @param query
     *            查询条件
     * @param sortQuery
     *            排序查询条件
     * @param response
     *            响应对象
     */
    @Operation(summary = "导出数据", description = "导出数据")
    @NoResponseAdvice
    @GetMapping("/export")
    public void export(Q query, SortQuery sortQuery, HttpServletResponse response) {
        this.checkPermission(Api.EXPORT);
        baseService.export(query, sortQuery, response);
    }

    /**
     * 根据 API 类型进行权限验证
     *
     * @param api
     *            API 类型
     */
    private void checkPermission(Api api) {
        CrudRequestMapping crudRequestMapping = this.getClass().getDeclaredAnnotation(CrudRequestMapping.class);
        String path = crudRequestMapping.value();
        String permissionPrefix = String.join(StringConstants.COLON, StrUtil.splitTrim(path, StringConstants.SLASH));
        StpUtil.checkPermission(String.format("%s:%s", permissionPrefix, api.name().toLowerCase()));
    }
}