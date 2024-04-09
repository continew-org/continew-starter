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

package top.continew.starter.auth.satoken.autoconfigure.dao;

import top.continew.starter.auth.satoken.enums.SaTokenDaoType;

/**
 * SaToken 持久层配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class SaTokenDaoProperties {

    /**
     * 持久层类型
     */
    private SaTokenDaoType type = SaTokenDaoType.DEFAULT;

    public SaTokenDaoType getType() {
        return type;
    }

    public void setType(SaTokenDaoType type) {
        this.type = type;
    }
}
