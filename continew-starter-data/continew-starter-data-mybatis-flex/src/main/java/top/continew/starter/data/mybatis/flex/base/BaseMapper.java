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

package top.continew.starter.data.mybatis.flex.base;

import cn.hutool.core.util.ClassUtil;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.row.Db;

import java.util.Collection;

/**
 * Mapper 基类
 *
 * @param <T> 实体类
 * @author hellokaton
 * @since 1.0.0
 */
public interface BaseMapper<T> extends com.mybatisflex.core.BaseMapper<T> {

    /**
     * 批量更新记录
     *
     * @param entityList 实体列表
     * @return 是否成功
     */
    default boolean updateBatchById(Collection<T> entityList) {
        return Db.updateEntitiesBatch(entityList) > 0;
    }

    /**
     * 链式查询
     *
     * @return QueryWrapper 的包装类
     */
    default QueryWrapper query() {
        return QueryWrapper.create();
    }

    /**
     * 链式查询
     *
     * @return QueryWrapper 的包装类
     */
    default QueryWrapper query(T entity) {
        return QueryWrapper.create(entity);
    }

    /**
     * 获取实体类 Class 对象
     *
     * @return 实体类 Class 对象
     */
    default Class<T> currentEntityClass() {
        return (Class<T>)ClassUtil.getTypeArgument(this.getClass(), 0);
    }

}
