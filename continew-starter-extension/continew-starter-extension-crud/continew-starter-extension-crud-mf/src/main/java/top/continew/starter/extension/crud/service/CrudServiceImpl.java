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

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.core.util.ReflectUtils;
import top.continew.starter.core.util.TreeUtils;
import top.continew.starter.core.util.validation.CheckUtils;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.data.base.BaseMapper;
import top.continew.starter.data.service.impl.ServiceImpl;
import top.continew.starter.data.util.QueryWrapperHelper;
import top.continew.starter.excel.util.ExcelUtils;
import top.continew.starter.extension.crud.annotation.DictModel;
import top.continew.starter.extension.crud.annotation.TreeField;
import top.continew.starter.extension.crud.autoconfigure.CrudProperties;
import top.continew.starter.extension.crud.autoconfigure.CrudTreeDictModelProperties;
import top.continew.starter.extension.crud.model.entity.BaseIdDO;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.resp.LabelValueResp;
import top.continew.starter.extension.crud.model.resp.PageResp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * CRUD 业务实现基类
 *
 * @param <M> Mapper 接口
 * @param <T> 实体类型
 * @param <L> 列表类型
 * @param <D> 详情类型
 * @param <Q> 查询条件类型
 * @param <C> 创建或修改请求参数类型
 * @author Charles7c
 * @since 1.0.0
 */
public class CrudServiceImpl<M extends BaseMapper<T>, T extends BaseIdDO, L, D, Q, C>
    extends ServiceImpl<M, T>
    implements CrudService<L, D, Q, C> {

    protected final Class<L> listClass = this.currentListClass();
    protected final Class<D> detailClass = this.currentDetailClass();
    protected final Class<Q> queryClass = this.currentQueryClass();
    private final List<Field> queryFields = ReflectUtils.getNonStaticFields(this.queryClass);

    @Override
    public PageResp<L> page(Q query, PageQuery pageQuery) {
        QueryWrapper queryWrapper = this.buildQueryWrapper(query);
        this.sort(queryWrapper, pageQuery);
        Page<T> page = mapper.paginate(pageQuery.getPage(), pageQuery.getSize(), queryWrapper);
        PageResp<L> pageResp = PageResp.build(page, listClass);
        pageResp.getList().forEach(this::fill);
        return pageResp;
    }

    @Override
    public List<L> list(Q query, SortQuery sortQuery) {
        List<L> list = this.list(query, sortQuery, listClass);
        list.forEach(this::fill);
        return list;
    }

    /**
     * 查询列表
     *
     * @param query       查询条件
     * @param sortQuery   排序查询条件
     * @param targetClass 指定类型
     * @return 列表信息
     */
    protected <E> List<E> list(Q query, SortQuery sortQuery, Class<E> targetClass) {
        QueryWrapper queryWrapper = this.buildQueryWrapper(query);
        // 设置排序
        this.sort(queryWrapper, sortQuery);
        List<T> entityList = mapper.selectListByQuery(queryWrapper);
        if (this.entityClass == targetClass) {
            return (List<E>) entityList;
        }
        return BeanUtil.copyToList(entityList, targetClass);
    }

    @Override
    public List<Tree<Long>> tree(Q query, SortQuery sortQuery, boolean isSimple) {
        return this.tree(query, sortQuery, isSimple, false);
    }

    @Override
    public List<Tree<Long>> tree(Q query, SortQuery sortQuery, boolean isSimple,
        boolean isSingleRoot) {
        List<L> list = this.list(query, sortQuery);
        if (CollUtil.isEmpty(list)) {
            return CollUtil.newArrayList();
        }
        CrudProperties crudProperties = SpringUtil.getBean(CrudProperties.class);
        CrudTreeDictModelProperties treeDictModel = crudProperties.getTreeDictModel();
        TreeField treeField = listClass.getDeclaredAnnotation(TreeField.class);
        TreeNodeConfig treeNodeConfig;
        Long rootId;
        // 简单树（例如：下拉列表）使用 CrudTreeDictModelProperties 全局树型字典映射配置，复杂树（例如：表格）使用 @TreeField 局部结构配置
        if (isSimple) {
            treeNodeConfig = treeDictModel.genTreeNodeConfig();
            rootId = treeDictModel.getRootId();
        } else {
            treeNodeConfig = treeDictModel.genTreeNodeConfig(treeField);
            rootId = treeField.rootId();
        }
        if (isSingleRoot) {
            // 构建单根节点树
            return TreeUtil.build(list, rootId, treeNodeConfig, (node,
                tree) -> buildTreeField(isSimple, node, tree,
                    treeField));
        } else {
            Function<L, Long> getId =
                ReflectUtils.createMethodReference(listClass, CharSequenceUtil.genGetter(treeField
                    .value()));
            Function<L, Long> getParentId =
                ReflectUtils.createMethodReference(listClass, CharSequenceUtil
                    .genGetter(treeField.parentIdKey()));
            // 构建多根节点树
            return TreeUtils.buildMultiRoot(list, getId, getParentId, treeNodeConfig, (node,
                tree) -> buildTreeField(isSimple,
                    node, tree, treeField));
        }
    }

    @Override
    public D get(Long id) {
        T entity = super.getById(id);
        D detail = BeanUtil.toBean(entity, detailClass);
        this.fill(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(C req) {
        this.beforeCreate(req);
        T entity = BeanUtil.copyProperties(req, this.entityClass);
        mapper.insert(entity);
        this.afterCreate(req, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(C req, Long id) {
        this.beforeUpdate(req, id);
        T entity = this.getById(id);
        if (entity == null) {
            throw new BusinessException("更新数据不存在，请检查后重试");
        }
        BeanUtil.copyProperties(req, entity, CopyOptions.create().ignoreNullValue());
        mapper.update(entity);
        this.afterUpdate(req, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        this.beforeDelete(ids);
        mapper.deleteBatchByIds(ids);
        this.afterDelete(ids);
    }

    @Override
    public void export(Q query, SortQuery sortQuery, HttpServletResponse response) {
        List<D> list = this.list(query, sortQuery, detailClass);
        list.forEach(this::fill);
        ExcelUtils.export(list, "导出数据", detailClass, response);
    }

    @Override
    public List<LabelValueResp> dict(Q query, SortQuery sortQuery) {
        DictModel dictModel = entityClass.getDeclaredAnnotation(DictModel.class);
        CheckUtils.throwIfNull(dictModel, "请添加并配置 @DictModel 字典结构信息");
        List<L> list = this.list(query, sortQuery);
        // 解析映射
        List<LabelValueResp> respList = new ArrayList<>(list.size());
        String labelKey = dictModel.labelKey().contains(StringConstants.DOT)
            ? CharSequenceUtil.subAfter(dictModel.labelKey(), StringConstants.DOT, true)
            : dictModel.labelKey();
        String valueKey = dictModel.valueKey().contains(StringConstants.DOT)
            ? CharSequenceUtil.subAfter(dictModel.valueKey(), StringConstants.DOT, true)
            : dictModel.valueKey();
        List<String> extraFieldNames = Arrays.stream(dictModel.extraKeys())
            .map(extraKey -> extraKey.contains(StringConstants.DOT)
                ? CharSequenceUtil.subAfter(extraKey, StringConstants.DOT, true)
                : extraKey)
            .map(CharSequenceUtil::toCamelCase)
            .toList();
        for (L entity : list) {
            LabelValueResp<Object> labelValueResp = new LabelValueResp<>();
            labelValueResp.setLabel(Convert.toStr(ReflectUtil.getFieldValue(entity, CharSequenceUtil
                .toCamelCase(labelKey))));
            labelValueResp.setValue(
                ReflectUtil.getFieldValue(entity, CharSequenceUtil.toCamelCase(valueKey)));
            respList.add(labelValueResp);
            if (CollUtil.isEmpty(extraFieldNames)) {
                continue;
            }
            // 额外数据
            Map<String, Object> extraMap = MapUtil.newHashMap(dictModel.extraKeys().length);
            for (String extraFieldName : extraFieldNames) {
                extraMap.put(extraFieldName, ReflectUtil.getFieldValue(entity, extraFieldName));
            }
            labelValueResp.setExtra(extraMap);
        }
        return respList;
    }

    /**
     * 设置排序
     *
     * @param queryWrapper 查询条件封装对象
     * @param sortQuery    排序查询条件
     */
    protected void sort(QueryWrapper queryWrapper, SortQuery sortQuery) {
        if (sortQuery == null || sortQuery.getSort().isUnsorted()) {
            return;
        }
        Sort sort = sortQuery.getSort();
        List<Field> entityFields = ReflectUtils.getNonStaticFields(this.entityClass);
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            String checkProperty;
            // 携带表别名则获取 . 后面的字段名
            if (property.contains(StringConstants.DOT)) {
                checkProperty =
                    CollUtil.getLast(CharSequenceUtil.split(property, StringConstants.DOT));
            } else {
                checkProperty = property;
            }
            Optional<Field> optional = entityFields.stream()
                .filter(field -> checkProperty.equals(field.getName()))
                .findFirst();
            ValidationUtils.throwIf(optional.isEmpty(), "无效的排序字段 [{}]", property);
            queryWrapper.orderBy(CharSequenceUtil.toUnderlineCase(property), order.isAscending());
        }
    }

    /**
     * 构建 QueryWrapper
     *
     * @param query 查询条件
     * @return QueryWrapper
     */
    protected QueryWrapper buildQueryWrapper(Q query) {
        QueryWrapper queryWrapper = QueryWrapper.create();
        // 解析并拼接查询条件
        return QueryWrapperHelper.build(query, queryFields, queryWrapper);
    }

    /**
     * 填充数据
     *
     * @param obj 待填充信息
     */
    protected void fill(Object obj) {
        /* 数据填充后置处理，默认无操作，由子类按需重写 */
    }

    /**
     * 新增前置处理
     *
     * @param req 创建信息
     */
    protected void beforeCreate(C req) {
        /* 新增前置处理 */
    }

    /**
     * 修改前置处理
     *
     * @param req 修改信息
     * @param id  ID
     */
    protected void beforeUpdate(C req, Long id) {
        /* 修改前置处理 */
    }

    /**
     * 删除前置处理
     *
     * @param ids ID 列表
     */
    protected void beforeDelete(List<Long> ids) {
        /* 删除前置处理 */
    }

    /**
     * 新增后置处理
     *
     * @param req    创建信息
     * @param entity 实体信息
     */
    protected void afterCreate(C req, T entity) {
        /* 新增后置处理 */
    }

    /**
     * 修改后置处理
     *
     * @param req    修改信息
     * @param entity 实体信息
     */
    protected void afterUpdate(C req, T entity) {
        /* 修改后置处理 */
    }

    /**
     * 删除后置处理
     *
     * @param ids ID 列表
     */
    protected void afterDelete(List<Long> ids) {
        /* 删除后置处理 */
    }

    /**
     * 获取当前列表信息类型
     *
     * @return 当前列表信息类型
     */
    protected Class<L> currentListClass() {
        return (Class<L>) this.typeArguments[2];
    }

    /**
     * 获取当前详情信息类型
     *
     * @return 当前详情信息类型
     */
    protected Class<D> currentDetailClass() {
        return (Class<D>) this.typeArguments[3];
    }

    /**
     * 获取当前查询条件类型
     *
     * @return 当前查询条件类型
     */
    protected Class<Q> currentQueryClass() {
        return (Class<Q>) this.typeArguments[4];
    }

    /**
     * 构建树字段
     *
     * @param isSimple  是否简单树结构
     * @param node      节点
     * @param tree      树
     * @param treeField 树字段
     */
    private void buildTreeField(boolean isSimple, L node, Tree<Long> tree, TreeField treeField) {
        tree.setId(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.value())));
        tree.setParentId(
            ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.parentIdKey())));
        tree.setName(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.nameKey())));
        tree.setWeight(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.weightKey())));
        // 如果构建简单树结构，则不包含扩展字段
        if (!isSimple) {
            List<Field> fieldList = ReflectUtils.getNonStaticFields(listClass);
            fieldList.removeIf(f -> CharSequenceUtil.equalsAnyIgnoreCase(f.getName(),
                treeField.value(), treeField
                    .parentIdKey(),
                treeField.nameKey(), treeField.weightKey(), treeField.childrenKey()));
            fieldList.forEach(f -> tree.putExtra(f.getName(),
                ReflectUtil.invoke(node, CharSequenceUtil.genGetter(f
                    .getName()))));
        }
    }
}
