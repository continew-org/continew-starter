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

package top.continew.starter.data.mybatis.plus.service.impl;

import cn.hutool.core.util.ClassUtil;
import top.continew.starter.core.util.ReflectUtils;
import top.continew.starter.core.util.validate.CheckUtils;
import top.continew.starter.data.mybatis.plus.base.BaseMapper;
import top.continew.starter.data.mybatis.plus.service.IService;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 通用业务实现类
 *
 * @param <M> Mapper 接口
 * @param <T> 实体类型
 * @author Charles7c
 * @since 1.5.0
 */
public class ServiceImpl<M extends BaseMapper<T>, T> extends com.baomidou.mybatisplus.extension.service.impl.ServiceImpl<M, T> implements IService<T> {

    private List<Field> entityFields;

    @Override
    public T getById(Serializable id) {
        return this.getById(id, true);
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
     * 根据 ID 查询
     *
     * @param id            ID
     * @param isCheckExists 是否检查存在
     * @return 实体信息
     */
    protected T getById(Serializable id, boolean isCheckExists) {
        T entity = baseMapper.selectById(id);
        if (isCheckExists) {
            CheckUtils.throwIfNotExists(entity, ClassUtil.getClassName(this.getEntityClass(), true), "ID", id);
        }
        return entity;
    }
}