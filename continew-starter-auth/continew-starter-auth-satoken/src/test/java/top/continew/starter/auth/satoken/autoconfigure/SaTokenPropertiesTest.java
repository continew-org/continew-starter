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

package top.continew.starter.auth.satoken.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.auth.satoken.autoconfigure.dao.SaTokenDaoProperties;

/**
 * {@link SaTokenDaoProperties} 与扩展属性单元测试：默认值与读写。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class SaTokenPropertiesTest {

    /**
     * Dao 类型应默认内存实现。
     */
    @Test
    @DisplayName("Dao 类型默认 DEFAULT")
    void shouldDefaultDaoTypeToInMemory() {
        assertThat(new SaTokenDaoProperties().getType())
            .isEqualTo(SaTokenDaoProperties.DaoType.DEFAULT);
    }

    /**
     * Dao 类型应可切换为 Redis。
     */
    @Test
    @DisplayName("Dao 类型可切换 REDIS")
    void shouldSwitchDaoTypeToRedis() {
        SaTokenDaoProperties properties = new SaTokenDaoProperties();
        properties.setType(SaTokenDaoProperties.DaoType.REDIS);

        assertThat(properties.getType()).isEqualTo(SaTokenDaoProperties.DaoType.REDIS);
    }
}
