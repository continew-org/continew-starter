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

package top.continew.starter.data.util;

import cn.hutool.core.text.CharSequenceUtil;
import top.continew.starter.data.annotation.Query;

import java.lang.reflect.Field;

/**
 * 查询注解工具
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class QueryUtils {

    private QueryUtils() {
    }

    /**
     * 选择字段在指定分组下生效的 {@code @Query} 声明
     *
     * <p>
     * 选择顺序：命中当前分组的声明优先；未命中分组声明时回退到未声明分组的声明（与既有
     * 单声明行为一致）；两者均不存在时返回 {@code null}，表示该字段在当前分组下不参与
     * 查询条件构建。
     * </p>
     *
     * @param field 查询条件字段
     * @param group 当前分组标识，为空时视为未指定分组，等价于既有单声明行为
     * @return 生效的 {@code @Query} 声明，无生效声明时返回 {@code null}
     * @since 3.1.0
     */
    public static Query selectQuery(Field field, String group) {
        // getAnnotationsByType 自动展开可重复注解的容器，单声明与多声明统一返回数组
        Query[] queries = field.getAnnotationsByType(Query.class);
        if (queries.length == 0) {
            return null;
        }
        if (CharSequenceUtil.isNotBlank(group)) {
            for (Query query : queries) {
                for (String declaredGroup : query.groups()) {
                    if (group.equals(declaredGroup)) {
                        return query;
                    }
                }
            }
        }
        for (Query query : queries) {
            if (query.groups().length == 0) {
                return query;
            }
        }
        return null;
    }
}
