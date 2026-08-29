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

package top.continew.starter.data.service.impl;

import cn.hutool.core.util.ClassUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.MapperProxyMetadata;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.MybatisUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.core.toolkit.reflect.GenericTypeUtils;
import com.baomidou.mybatisplus.extension.repository.AbstractRepository;
import com.baomidou.mybatisplus.extension.repository.IRepository;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.Log;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import top.continew.starter.core.util.ReflectUtils;
import top.continew.starter.data.service.IService;
import top.continew.starter.core.util.validation.CheckUtils;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * 通用业务实现类
 *
 * <p>将 MP 的 {@link IRepository} 迁移至本类中，减少两层继承，解决层级过多出现 Sonar 警告的问题</p>
 *
 * @see IRepository
 *
 * @param <M> Mapper 接口
 * @param <T> 实体类型
 * @author hubin （<a href="https://gitee.com/baomidou/mybatis-plus">MyBatis Plus</a>）
 * @author Charles7c
 * @since 1.5.0
 */
public abstract class ServiceImpl<M extends BaseMapper<T>, T> implements IService<T> {

    @Autowired
    protected M baseMapper;
    private Class<T> entityClass;
    private Class<M> mapperClass;
    private List<Field> entityFields;
    private volatile SqlSessionFactory sqlSessionFactory;
    private final Log innerLog = LogFactory.getLog(getClass());

    /**
     * TableId 注解存在更新记录，否插入一条记录
     *
     * @param entity 实体对象
     * @return boolean
     * @see AbstractRepository#saveOrUpdate(Object)
     */
    @Override
    public boolean saveOrUpdate(T entity) {
        return getBaseMapper().insertOrUpdate(entity);
    }

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param throwEx      有多个 result 是否抛出异常
     * @return 单条数据
     * @see AbstractRepository#getOne(Wrapper, boolean)
     */
    @Override
    public T getOne(Wrapper<T> queryWrapper, boolean throwEx) {
        return getBaseMapper().selectOne(queryWrapper, throwEx);
    }

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param throwEx      有多个 result 是否抛出异常
     * @return {@link Optional} 返回一个Optional对象
     * @see AbstractRepository#getOneOpt(Wrapper, boolean)
     */
    @Override
    public Optional<T> getOneOpt(Wrapper<T> queryWrapper, boolean throwEx) {
        return Optional.ofNullable(getBaseMapper().selectOne(queryWrapper, throwEx));
    }

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @return 单条数据
     * @see AbstractRepository#getMap(Wrapper)
     */
    @Override
    public Map<String, Object> getMap(Wrapper<T> queryWrapper) {
        return SqlHelper.getObject(innerLog, getBaseMapper().selectMaps(queryWrapper));
    }

    /**
     * 根据 Wrapper，查询一条记录
     *
     * @param queryWrapper 实体对象封装操作类 {@link com.baomidou.mybatisplus.core.conditions.query.QueryWrapper}
     * @param mapper       转换函数
     * @return 单条数据
     * @see AbstractRepository#getObj(Wrapper, Function)
     */
    @Override
    public <V> V getObj(Wrapper<T> queryWrapper, Function<? super Object, V> mapper) {
        return SqlHelper.getObject(innerLog, listObjs(queryWrapper, mapper));
    }

    /**
     * 执行批量操作
     *
     * @param list      数据集合
     * @param batchSize 批量大小
     * @param consumer  执行方法
     * @param <E>       泛型
     * @return 操作结果
     */
    protected <E> boolean executeBatch(Collection<E> list, int batchSize,
        BiFunction<SqlSession, E, Integer> consumer) {
        return SqlHelper.executeBatch(getSqlSessionFactory(), this.innerLog, list, batchSize,
            consumer);
    }

    /**
     * 执行批量操作（默认批次提交数量{@link IRepository#DEFAULT_BATCH_SIZE}）
     *
     * @param list     数据集合
     * @param consumer 执行方法
     * @param <E>      泛型
     * @return 操作结果
     */
    protected <E> boolean executeBatch(Collection<E> list,
        BiFunction<SqlSession, E, Integer> consumer) {
        return executeBatch(list, DEFAULT_BATCH_SIZE, consumer);
    }

    /**
     * 根据 ID 删除
     *
     * @param id      主键(类型必须与实体类型字段保持一致)
     * @param useFill 是否启用填充(为true的情况,会将入参转换实体进行delete删除)
     * @return 删除结果
     * @see AbstractRepository#removeById(Serializable, boolean)
     */
    @Override
    public boolean removeById(Serializable id, boolean useFill) {
        return SqlHelper.retBool(getBaseMapper().deleteById(id, useFill));
    }

    /**
     * 批量插入
     *
     * @param entityList 数据集合
     * @param batchSize  批量大小
     * @return boolean
     * @see IRepository#saveBatch(Collection, int)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveBatch(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.INSERT_ONE);
        return executeBatch(entityList, batchSize,
            (sqlSession, entity) -> sqlSession.insert(sqlStatement, entity));
    }

    /**
     * 批量插入
     *
     * @param entityList 数据集合
     * @param batchSize  批量大小
     * @return boolean
     * @see IRepository#saveOrUpdateBatch(Collection, int)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Assert.notNull(tableInfo,
            "error: can not execute. because can not find cache of TableInfo for entity!");
        String keyProperty = tableInfo.getKeyProperty();
        Assert.notEmpty(keyProperty,
            "error: can not execute. because can not find column for id from entity!");
        return SqlHelper.saveOrUpdateBatch(getSqlSessionFactory(), this
            .getMapperClass(), this.innerLog, entityList, batchSize, (sqlSession, entity) -> {
                Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
                return StringUtils.checkValNull(idVal) || CollectionUtils.isEmpty(sqlSession
                    .selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
            }, (sqlSession, entity) -> {
                MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                param.put(Constants.ENTITY, entity);
                return sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
            });
    }

    /**
     * 批量更新
     *
     * @param entityList 数据集合
     * @param batchSize  批量大小
     * @return boolean
     * @see IRepository#updateBatchById(Collection, int)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean updateBatchById(Collection<T> entityList, int batchSize) {
        String sqlStatement = getSqlStatement(SqlMethod.UPDATE_BY_ID);
        return executeBatch(entityList, batchSize, (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            return sqlSession.update(sqlStatement, param);
        });
    }

    @Override
    public T getById(Serializable id) {
        return this.getById(id, true);
    }

    /**
     * 根据 ID 查询
     *
     * @param id            ID
     * @param isCheckExists 是否检查存在
     * @return 实体信息
     */
    protected T getById(Serializable id, boolean isCheckExists) {
        T entity = baseMapper.selectById(id);
        if (isCheckExists) {
            CheckUtils.throwIfNotExists(entity, ClassUtil.getClassName(this.getEntityClass(), true),
                "ID", id);
        }
        return entity;
    }

    @Override
    public M getBaseMapper() {
        Assert.notNull(this.baseMapper, "baseMapper can not be null");
        return this.baseMapper;
    }

    @Override
    public Class<T> getEntityClass() {
        if (this.entityClass == null) {
            this.entityClass = (Class<T>) GenericTypeUtils.resolveTypeArguments(this
                .getMapperClass(), BaseMapper.class)[0];
        }
        return this.entityClass;
    }

    /**
     * 获取当前 Mapper 类型
     *
     * @return 当前 Mapper 类型
     */
    public Class<M> getMapperClass() {
        if (this.mapperClass == null) {
            MapperProxyMetadata mapperProxyMetadata =
                MybatisUtils.getMapperProxy(this.getBaseMapper());
            this.mapperClass = (Class<M>) mapperProxyMetadata.getMapperInterface();
        }
        return this.mapperClass;
    }

    /**
     * 获取当前实体类型字段
     *
     * @return 当前实体类型字段列表
     */
    public List<Field> getEntityFields() {
        if (this.entityFields == null) {
            this.entityFields = ReflectUtils.getNonStaticFields(this.getEntityClass());
        }
        return this.entityFields;
    }

    /**
     * 获取 SqlSessionFactory
     *
     * @return SqlSessionFactory
     */
    protected SqlSessionFactory getSqlSessionFactory() {
        if (this.sqlSessionFactory == null) {
            MapperProxyMetadata mapperProxyMetadata =
                MybatisUtils.getMapperProxy(this.getBaseMapper());
            this.sqlSessionFactory =
                MybatisUtils.getSqlSessionFactory(mapperProxyMetadata.getSqlSession());
        }
        return this.sqlSessionFactory;
    }

    /**
     * 获取mapperStatementId
     *
     * @param sqlMethod 方法名
     * @return 命名id
     */
    protected String getSqlStatement(SqlMethod sqlMethod) {
        return SqlHelper.getSqlStatement(this.getMapperClass(), sqlMethod);
    }
}
