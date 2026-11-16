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

package top.continew.starter.smoke.tests.combination;

import static org.assertj.core.api.Assertions.assertThat;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * cache + auth 组合冒烟：redisson 装配与 satoken 共存。
 *
 * <p>RedissonClient 以 mock 替身注册（真实客户端的 redisson() Bean 为
 * ConditionalOnMissingBean，替身注册后即跳过真实连接）；satoken DAO 走默认
 * 内存实现（dao.type 缺省为 default），验证两者在无真实 Redis 环境下可共存启动。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootTest(classes = CacheAuthSmokeApp.class, properties = {
    "sa-token.extension.enabled=true",
    "spring.autoconfigure.exclude=top.continew.starter.data.autoconfigure.MybatisPlusAutoConfiguration,"
        + "com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration,"
        + "top.continew.starter.extension.tenant.autoconfigure.TenantAutoConfiguration,"
        + "top.continew.starter.extension.datapermission.autoconfigure.DataPermissionAutoConfiguration,"
        + "top.continew.starter.encrypt.field.autoconfigure.FieldEncryptAutoConfiguration,"
        + "top.continew.starter.log.autoconfigure.LogAutoConfiguration,"
        + "top.continew.starter.trace.autoconfigure.TraceAutoConfiguration,"
        + "top.continew.starter.storage.autoconfigure.StorageAutoConfiguration,"
        + "top.continew.starter.messaging.mail.autoconfigure.MailAutoConfiguration,"
        + "top.continew.starter.messaging.websocket.autoconfigure.WebSocketAutoConfiguration"},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("cache + auth 组合冒烟")
class CacheAuthCombinationSmokeTest {

    @Autowired
    private SaTokenDao saTokenDao;

    @MockitoBean
    private RedissonClient redissonClient;

    /**
     * 两个 Starter 共存启动后，satoken DAO 应为默认内存实现。
     */
    @Test
    @DisplayName("共存启动且 satoken 走默认内存 DAO")
    void shouldStartWithDefaultSaTokenDao() {
        assertThat(saTokenDao).isInstanceOf(SaTokenDaoDefaultImpl.class);
        saTokenDao.set("smoke:cache-auth", "v", 60);
        assertThat(saTokenDao.get("smoke:cache-auth")).isEqualTo("v");
    }
}
