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

package top.continew.starter.extension.crud.model.resp;

import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serial;
import java.io.Serializable;
import java.util.List;

/**
 * 分页信息
 *
 * @param <T> 列表数据类型
 * @author Charles7c
 * @since 2.5.2
 */
@Schema(description = "分页信息")
public class BasePageResp<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 列表数据
     */
    @Schema(description = "列表数据")
    private List<T> list;

    /**
     * 总记录数
     */
    @Schema(description = "总记录数", example = "10")
    private long total;

    public BasePageResp() {
    }

    public BasePageResp(final List<T> list, final long total) {
        this.list = list;
        this.total = total;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}
