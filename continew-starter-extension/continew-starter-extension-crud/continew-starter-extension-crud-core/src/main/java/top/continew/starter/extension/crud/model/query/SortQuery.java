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

package top.continew.starter.extension.crud.model.query;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ArrayUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Sort;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.validation.ValidationUtils;
import top.continew.starter.data.util.SqlInjectionUtils;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 排序查询条件
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Schema(description = "排序查询条件")
public class SortQuery implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 排序条件
     */
    @Schema(description = "排序条件", example = "createTime,desc")
    private String[] sort;

    public SortQuery() {
    }

    /**
     * 构造方法
     *
     * <p>
     * 示例：{@code new SortQuery("createTime,desc", "name,asc")}
     * </p>
     *
     * @param sort 排序条件
     * @since 2.12.0
     */
    public SortQuery(String... sort) {
        this.sort = sort;
    }

    /**
     * 构造方法
     *
     * <p>
     * 示例：{@code new SortQuery("createTime", Sort.Direction.DESC)}
     * </p>
     *
     * @param field     字段
     * @param direction 排序方向
     * @since 2.12.0
     */
    public SortQuery(String field, Sort.Direction direction) {
        this(field + StringConstants.COMMA + direction.name().toLowerCase());
    }

    /**
     * 构造方法
     *
     * <p>
     * 示例：{@code new SortQuery(Sort.by(Sort.Order.desc("createTime")))}
     * </p>
     *
     * @param sort 排序条件
     * @since 2.12.0
     */
    public SortQuery(Sort sort) {
        if (sort == null || sort.isUnsorted()) {
            this.sort = null;
            return;
        }
        this.sort = sort.stream()
            .map(order -> order.getProperty() + StringConstants.COMMA
                + order.getDirection().name().toLowerCase())
            .toArray(String[]::new);
    }

    /**
     * 解析排序条件为 Spring 分页排序实体
     *
     * @return Spring 分页排序实体
     */
    public Sort getSort() {
        if (ArrayUtil.isEmpty(sort)) {
            return Sort.unsorted();
        }
        ValidationUtils.throwIf(sort.length < 2, "排序条件无效");
        List<Sort.Order> orders = new ArrayList<>(sort.length);
        if (CharSequenceUtil.contains(sort[0], StringConstants.COMMA)) {
            // e.g "sort=createTime,desc&sort=name,asc"
            for (String s : sort) {
                List<String> sortList = CharSequenceUtil.splitTrim(s, StringConstants.COMMA);
                orders.add(this.getOrder(sortList.get(0), sortList.get(1)));
            }
        } else {
            // e.g "sort=createTime,desc"
            orders.add(this.getOrder(sort[0], sort[1]));
        }
        return Sort.by(orders);
    }

    public void setSort(String[] sort) {
        this.sort = sort;
    }

    /**
     * 获取排序条件
     *
     * @param field     字段
     * @param direction 排序方向
     * @return 排序条件
     */
    private Sort.Order getOrder(String field, String direction) {
        ValidationUtils.throwIf(SqlInjectionUtils.check(field), "排序字段包含无效字符");
        return new Sort.Order(Sort.Direction.valueOf(direction.toUpperCase()), field);
    }
}
