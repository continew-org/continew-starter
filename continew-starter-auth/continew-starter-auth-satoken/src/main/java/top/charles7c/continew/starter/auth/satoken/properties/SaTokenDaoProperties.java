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

package top.charles7c.continew.starter.auth.satoken.properties;

import cn.dev33.satoken.dao.SaTokenDao;
import top.charles7c.continew.starter.auth.satoken.enums.SaTokenDaoType;

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
    private SaTokenDaoType type;

    /**
     * 自定义持久层实现类（当 type 为 CUSTOM 时必填）
     */
    private Class<? extends SaTokenDao> impl;

    public SaTokenDaoType getType() {
        return type;
    }

    public void setType(SaTokenDaoType type) {
        this.type = type;
    }

    public Class<? extends SaTokenDao> getImpl() {
        return impl;
    }

    public void setImpl(Class<? extends SaTokenDao> impl) {
        this.impl = impl;
    }

    @Override
    public String toString() {
        return "SaTokenDaoProperties{" + "type=" + type + ", impl=" + impl + '}';
    }
}
