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

package top.continew.starter.extension.crud.service.impl;

import cn.crane4j.core.support.OperateTemplate;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.ClassUtils;
import top.continew.starter.core.util.ReflectUtils;
import top.continew.starter.core.util.validate.ValidationUtils;
import top.continew.starter.data.mybatis.plus.base.BaseMapper;
import top.continew.starter.data.mybatis.plus.query.QueryWrapperHelper;
import top.continew.starter.data.mybatis.plus.service.impl.ServiceImpl;
import top.continew.starter.extension.crud.annotation.TreeField;
import top.continew.starter.extension.crud.model.entity.BaseDO;
import top.continew.starter.extension.crud.model.req.BaseReq;
import top.continew.starter.extension.crud.model.query.PageQuery;
import top.continew.starter.extension.crud.model.query.SortQuery;
import top.continew.starter.extension.crud.model.resp.PageResp;
import top.continew.starter.extension.crud.service.BaseService;
import top.continew.starter.extension.crud.util.TreeUtils;
import top.continew.starter.file.excel.util.ExcelUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
public abstract class BaseServiceImpl<M extends BaseMapper<T>, T extends BaseDO, L, D, Q, C extends BaseReq> extends ServiceImpl<M, T> implements BaseService<L, D, Q, C> {

    private final Class<?>[] typeArgumentCache = ClassUtils.getTypeArguments(this.getClass());
    protected final Class<L> listClass = this.currentListClass();
    protected final Class<D> detailClass = this.currentDetailClass();
    protected final Class<Q> queryClass = this.currentQueryClass();
    private final List<Field> queryFields = ReflectUtils.getNonStaticFields(this.queryClass);

    @Override
    public PageResp<L> page(Q query, PageQuery pageQuery) {
        QueryWrapper<T> queryWrapper = this.handleQueryWrapper(query);
        IPage<T> page = baseMapper.selectPage(pageQuery.toPage(), queryWrapper);
        PageResp<L> pageResp = PageResp.build(page, listClass);
        pageResp.getList().forEach(this::fill);
        return pageResp;
    }

    @Override
    public List<Tree<Long>> tree(Q query, SortQuery sortQuery, boolean isSimple) {
        List<L> list = this.list(query, sortQuery);
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        // 如果构建简单树结构，则不包含基本树结构之外的扩展字段
        TreeNodeConfig treeNodeConfig = TreeUtils.DEFAULT_CONFIG;
        TreeField treeField = listClass.getDeclaredAnnotation(TreeField.class);
        if (!isSimple) {
            // 根据 @TreeField 配置生成树结构配置
            treeNodeConfig = TreeUtils.genTreeNodeConfig(treeField);
        }
        // 构建树
        return TreeUtils.build(list, treeNodeConfig, (node, tree) -> {
            // 转换器
            tree.setId(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.value())));
            tree.setParentId(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.parentIdKey())));
            tree.setName(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.nameKey())));
            tree.setWeight(ReflectUtil.invoke(node, CharSequenceUtil.genGetter(treeField.weightKey())));
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
    public List<L> list(Q query, SortQuery sortQuery) {
        List<L> list = this.list(query, sortQuery, listClass);
        list.forEach(this::fill);
        return list;
    }

    @Override
    public D get(Long id) {
        T entity = super.getById(id, false);
        D detail = BeanUtil.toBean(entity, detailClass);
        this.fill(detail);
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long add(C req) {
        this.beforeAdd(req);
        T entity = BeanUtil.copyProperties(req, entityClass);
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
        baseMapper.deleteBatchIds(ids);
        this.afterDelete(ids);
    }

    @Override
    public void export(Q query, SortQuery sortQuery, HttpServletResponse response) {
        List<D> list = this.list(query, sortQuery, detailClass);
        list.forEach(this::fill);
        ExcelUtils.export(list, "导出数据", detailClass, response);
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
        QueryWrapper<T> queryWrapper = this.handleQueryWrapper(query);
        // 设置排序
        this.sort(queryWrapper, sortQuery);
        List<T> entityList = baseMapper.selectList(queryWrapper);
        if (entityClass == targetClass) {
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
        Sort sort = Opt.ofNullable(sortQuery).orElseGet(SortQuery::new).getSort();
        for (Sort.Order order : sort) {
            if (null != order) {
                String property = order.getProperty();
                String checkProperty;
                // 携带表别名则获取 . 后面的字段名
                if (property.contains(StringConstants.DOT)) {
                    checkProperty = CollUtil.getLast(CharSequenceUtil.split(property, StringConstants.DOT));
                } else {
                    checkProperty = property;
                }
                Optional<Field> optional = entityFields.stream()
                    .filter(field -> checkProperty.equals(field.getName()))
                    .findFirst();
                ValidationUtils.throwIf(optional.isEmpty(), "无效的排序字段 [{}]", property);
                queryWrapper.orderBy(true, order.isAscending(), CharSequenceUtil.toUnderlineCase(property));
            }
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
     * 处理查询条件
     *
     * @return QueryWrapper
     */
    protected QueryWrapper<T> handleQueryWrapper(Q query) {
        QueryWrapper<T> queryWrapper = new QueryWrapper<>();
        // 解析并拼接查询条件
        return QueryWrapperHelper.build(query, queryFields, queryWrapper);
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

    /**
     * 获取当前列表信息类型
     *
     * @return 当前列表信息类型
     */
    protected Class<L> currentListClass() {
        return (Class<L>)this.typeArgumentCache[2];
    }

    /**
     * 获取当前详情信息类型
     *
     * @return 当前详情信息类型
     */
    protected Class<D> currentDetailClass() {
        return (Class<D>)this.typeArgumentCache[3];
    }

    /**
     * 获取当前查询条件类型
     *
     * @return 当前查询条件类型
     */
    protected Class<Q> currentQueryClass() {
        return (Class<Q>)this.typeArgumentCache[4];
    }
}
