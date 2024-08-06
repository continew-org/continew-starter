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

import java.io.Serializable;

/**
 * ID 响应信息
 *
 * @author Charles7c
 * @since 2.5.0
 */
public class BaseIdResp<T extends Serializable> implements Serializable {

    /**
     * ID
     */
    @Schema(description = "ID", example = "1")
    private T id;

    public T getId() {
        return id;
    }

    public void setId(T id) {
        this.id = id;
    }

    BaseIdResp(final T id) {
        this.id = id;
    }

    public static <T extends Serializable> BaseIdRespBuilder<T> builder() {
        return new BaseIdRespBuilder();
    }

    public static class BaseIdRespBuilder<T extends Serializable> {
        private T id;

        BaseIdRespBuilder() {
        }

        public BaseIdRespBuilder<T> id(final T id) {
            this.id = id;
            return this;
        }

        public BaseIdResp<T> build() {
            return new BaseIdResp(this.id);
        }
    }
}
