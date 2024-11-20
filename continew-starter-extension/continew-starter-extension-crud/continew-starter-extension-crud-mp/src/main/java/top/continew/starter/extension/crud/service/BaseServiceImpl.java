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

import cn.crane4j.core.support.OperateTemplate;
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
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.ClassUtils;
import top.continew.starter.core.util.ReflectUtils;
import top.continew.starter.core.validation.CheckUtils;
import top.continew.starter.core.validation.ValidationUtils;
import top.continew.starter.data.mp.base.BaseMapper;
import top.continew.starter.data.mp.service.impl.ServiceImpl;
import top.continew.starter.data.mp.util.QueryWrapperHelper;
import top.continew.starter.extension.crud.annotation.DictField;
import top.continew.starter.extension.crud.annotation.TreeField;
import top.continew.starter.extension.crud.autoconfigure.CrudProperties;
import top.continew.starter.extension.crud.autoconfigure.CrudTreeProperties;
import top.continew.starter.extension.crud.model.entity.BaseIdDO;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.resp.LabelValueResp;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.file.excel.util.ExcelUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 业务实现基类
 *
 * @param <M> Mapper 接口
 * @param <T> 实体类型
 * @param <L> 列表类型
 * @param <D> 详情类型
 * @param <Q> 查询条件
 * @param <C> 创建或修改类型
 * @author Charles7c
 * @since 1.0.0
 */
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseIdDO, L, D, Q, C> extends ServiceImpl<M, T> implements BaseService<L, D, Q, C> {

    private Class<L> listClass;
    private Class<D> detailClass;
    private Class<Q> queryClass;
    private List<Field> queryFields;

    @Override
    public PageResp<L> page(Q query, PageQuery pageQuery) {
        QueryWrapper<T> queryWrapper = this.buildQueryWrapper(query);
        this.sort(queryWrapper, pageQuery);
        IPage<T> page = baseMapper.selectPage(new Page<>(pageQuery.getPage(), pageQuery.getSize()), queryWrapper);
        PageResp<L> pageResp = PageResp.build(page, this.getListClass());
        pageResp.getList().forEach(this::fill);
        return pageResp;
    }

    @Override
    public List<L> list(Q query, SortQuery sortQuery) {
        List<L> list = this.list(query, sortQuery, this.getListClass());
        list.forEach(this::fill);
        return list;
    }

    @Override
    public List<Tree<Long>> tree(Q query, SortQuery sortQuery, boolean isSimple) {
        List<L> list = this.list(query, sortQuery);
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        CrudProperties crudProperties = SpringUtil.getBean(CrudProperties.class);
        CrudTreeProperties treeProperties = crudProperties.getTree();
        TreeField treeField = listClass.getDeclaredAnnotation(TreeField.class);
        TreeNodeConfig treeNodeConfig;
        Long rootId;
        // 简单树（下拉列表）使用全局配置结构，复杂树（表格）使用局部配置
        if (isSimple) {
            treeNodeConfig = treeProperties.genTreeNodeConfig();
            rootId = treeProperties.getRootId();
        } else {
            treeNodeConfig = treeProperties.genTreeNodeConfig(treeField);
            rootId = treeField.rootId();
        }
        // 构建树
        return TreeUtil.build(list, rootId, treeNodeConfig, (node, tree) -> {
            tree.setId(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.value())));
            tree.setParentId(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.parentIdKey())));
            tree.setName(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.nameKey())));
            tree.setWeight(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.weightKey())));
            // 如果构建简单树结构，则不包含扩展字段
            if (!isSimple) {
                List<Field> fieldList = ReflectUtils.getNonStaticFields(listClass);
                fieldList.removeIf(f -> CharSequenceUtil.equalsAnyIgnoreCase(f.getName(), treeField.value(), treeField
                    .parentIdKey(), treeField.nameKey(), treeField.weightKey(), treeField.childrenKey()));
                fieldList.forEach(f -> tree.putExtra(f.getName(), ReflectUtil.invoke(node, CharSequenceUtil.genGetter(f
                    .getName()))));
            }
        });
    }

    @Override
    public D get(Long id) {
        T entity = super.getById(id, false);
        D detail = BeanUtil.toBean(entity, this.getDetailClass());
        this.fill(detail);
        return detail;
    }

    @Override
    public List<LabelValueResp> listDict(Q query, SortQuery sortQuery) {
        DictField dictField = super.getEntityClass().getDeclaredAnnotation(DictField.class);
        CheckUtils.throwIfNull(dictField, "请添加并配置 @DictField 字典结构信息");
        List<L> list = this.list(query, sortQuery);
        // 解析映射
        List<LabelValueResp> respList = new ArrayList<>(list.size());
        String labelKey = dictField.labelKey().contains(StringConstants.DOT)
            ? CharSequenceUtil.subAfter(dictField.labelKey(), StringConstants.DOT, true)
            : dictField.labelKey();
        String valueKey = dictField.valueKey().contains(StringConstants.DOT)
            ? CharSequenceUtil.subAfter(dictField.valueKey(), StringConstants.DOT, true)
            : dictField.valueKey();
        List<String> extraFieldNames = Arrays.stream(dictField.extraKeys())
            .map(extraKey -> extraKey.contains(StringConstants.DOT)
                ? CharSequenceUtil.subAfter(extraKey, StringConstants.DOT, true)
                : extraKey)
            .map(CharSequenceUtil::toCamelCase)
            .toList();
        for (L entity : list) {
            LabelValueResp<Object> labelValueResp = new LabelValueResp<>();
            labelValueResp.setLabel(Convert.toStr(ReflectUtil.getFieldValue(entity, CharSequenceUtil
                .toCamelCase(labelKey))));
            labelValueResp.setValue(ReflectUtil.getFieldValue(entity, CharSequenceUtil.toCamelCase(valueKey)));
            respList.add(labelValueResp);
            if (CollUtil.isEmpty(extraFieldNames)) {
                continue;
            }
            // 额外数据
            Map<String, Object> extraMap = MapUtil.newHashMap(dictField.extraKeys().length);
            for (String extraFieldName : extraFieldNames) {
                extraMap.put(extraFieldName, ReflectUtil.getFieldValue(entity, extraFieldName));
            }
            labelValueResp.setExtra(extraMap);
        }
        return respList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(C req) {
        this.beforeAdd(req);
        T entity = BeanUtil.copyProperties(req, super.getEntityClass());
        baseMapper.insert(entity);
        this.afterAdd(req, entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(C req, Long id) {
        this.beforeUpdate(req, id);
        T entity = this.getById(id);
        BeanUtil.copyProperties(req, entity, CopyOptions.create().ignoreNullValue());
        baseMapper.updateById(entity);
        this.afterUpdate(req, entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(List<Long> ids) {
        this.beforeDelete(ids);
        baseMapper.deleteByIds(ids);
        this.afterDelete(ids);
    }

    @Override
    public void export(Q query, SortQuery sortQuery, HttpServletResponse response) {
        List<D> list = this.list(query, sortQuery, this.getDetailClass());
        list.forEach(this::fill);
        ExcelUtils.export(list, "导出数据", this.getDetailClass(), response);
    }

    /**
     * 获取当前列表信息类型
     *
     * @return 当前列表信息类型
     */
    public Class<L> getListClass() {
        if (this.listClass == null) {
            this.listClass = (Class<L>)ClassUtils.getTypeArguments(this.getClass())[2];
        }
        return this.listClass;
    }

    /**
     * 获取当前详情信息类型
     *
     * @return 当前详情信息类型
     */
    public Class<D> getDetailClass() {
        if (this.detailClass == null) {
            this.detailClass = (Class<D>)ClassUtils.getTypeArguments(this.getClass())[3];
        }
        return this.detailClass;
    }

    /**
     * 获取当前查询条件类型
     *
     * @return 当前查询条件类型
     */
    public Class<Q> getQueryClass() {
        if (this.queryClass == null) {
            this.queryClass = (Class<Q>)ClassUtils.getTypeArguments(this.getClass())[4];
        }
        return this.queryClass;
    }

    /**
     * 获取当前查询条件类型字段
     *
     * @return 当前查询条件类型字段列表
     */
    public List<Field> getQueryFields() {
        if (this.queryFields == null) {
            this.queryFields = ReflectUtils.getNonStaticFields(this.getQueryClass());
        }
        return queryFields;
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
        QueryWrapper<T> queryWrapper = this.buildQueryWrapper(query);
        // 设置排序
        this.sort(queryWrapper, sortQuery);
        List<T> entityList = baseMapper.selectList(queryWrapper);
        if (super.getEntityClass() == targetClass) {
            return (List<E>)entityList;
        }
        return BeanUtil.copyToList(entityList, targetClass);
    }

    /**
     * 设置排序
     *
     * @param queryWrapper 查询条件封装对象
     * @param sortQuery    排序查询条件
     */
    protected void sort(QueryWrapper<T> queryWrapper, SortQuery sortQuery) {
        if (sortQuery == null || sortQuery.getSort().isUnsorted()) {
            return;
        }
        Sort sort = sortQuery.getSort();
        for (Sort.Order order : sort) {
            String property = order.getProperty();
            String checkProperty;
            // 携带表别名则获取 . 后面的字段名
            if (property.contains(StringConstants.DOT)) {
                checkProperty = CollUtil.getLast(CharSequenceUtil.split(property, StringConstants.DOT));
            } else {
                checkProperty = property;
            }
            Optional<Field> optional = super.getEntityFields().stream()
                .filter(field -> checkProperty.equals(field.getName()))
                .findFirst();
            ValidationUtils.throwIf(optional.isEmpty(), "无效的排序字段 [{}]", property);
            queryWrapper.orderBy(true, order.isAscending(), CharSequenceUtil.toUnderlineCase(property));
        }
    }

    /**
     * 填充数据
     *
     * @param obj 待填充信息
     */
    protected void fill(Object obj) {
        if (null == obj) {
            return;
        }
        OperateTemplate operateTemplate = SpringUtil.getBean(OperateTemplate.class);
        operateTemplate.execute(obj);
    }

    /**
     * 构建 QueryWrapper
     *
     * @param query 查询条件
     * @return QueryWrapper
     */
    protected QueryWrapper<T> buildQueryWrapper(Q query) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        // 解析并拼接查询条件
        return QueryWrapperHelper.build(query, this.getQueryFields(), queryWrapper);
    }

    /**
     * 新增前置处理
     *
     * @param req 创建信息
     */
    protected void beforeAdd(C req) {
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
    protected void afterAdd(C req, T entity) {
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
}
