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

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import org.hibernate.validator.constraints.Range;
import org.springdoc.core.annotations.ParameterObject;

import java.io.Serial;

/**
 * 分页查询条件
 *
 * @author Charles7c
 * @since 1.0.0
 */
@ParameterObject
@Schema(description = "分页查询条件")
public class PageQuery extends SortQuery {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 默认页码：1
     */
    private static final int DEFAULT_PAGE = 1;
    /**
     * 默认每页条数：10
     */
    private static final int DEFAULT_SIZE = 10;

    /**
     * 页码
     */
    @Schema(description = "页码", example = "1")
    @Min(value = 1, message = "页码最小值为 {value}")
    private Integer page = DEFAULT_PAGE;

    /**
     * 每页条数
     */
    @Schema(description = "每页条数", example = "10")
    @Range(min = 1, max = 1000, message = "每页条数（取值范围 {min}-{max}）")
    private Integer size = DEFAULT_SIZE;

    public Integer getPage() {
        return page;
    }

    public void setPage(Integer page) {
        this.page = page;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }
}
