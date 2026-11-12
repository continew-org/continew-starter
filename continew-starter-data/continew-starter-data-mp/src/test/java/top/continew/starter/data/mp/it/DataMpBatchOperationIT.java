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

package top.continew.starter.data.mp.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import top.continew.starter.data.mp.it.entity.ItUser;
import top.continew.starter.data.mp.it.entity.ItUserStatus;
import top.continew.starter.data.mp.it.mapper.ItUserMapper;
import top.continew.starter.data.mp.it.service.ItUserService;

import java.util.List;

/**
 * MyBatis Plus 批量操作集成测试：真实 MySQL 上的批量插入、批量保存或更新与链式查询。
 *
 * <p>批量路径经 Spring 批量会话真实提交，断言直接核对真实表行数与列值。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
class DataMpBatchOperationIT extends MySqlSupport {

    /**
     * 批量插入：批量大小小于集合长度时按批次真实提交，全部落库。
     */
    @Test
    void shouldSaveBatchWithBatchSize() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            List<ItUser> users = List.of(user(1L, "单元一号", 10L, ItUserStatus.ENABLED),
                user(2L, "单元二号", 10L, ItUserStatus.DISABLED),
                user(3L, "单元三号", 20L, ItUserStatus.LOCKED));
            assertThat(service.saveBatch(users, 2)).isTrue();
            assertThat(rawCount()).isEqualTo(3L);
            assertThat(rawRow(3L).get("username")).isEqualTo("junit_u3");
            assertThat(((Number) rawRow(2L).get("status")).intValue()).isEqualTo(0);
        });
    }

    /**
     * 批量保存或更新：已有主键走真实更新、新主键走真实插入，混合集合一次提交。
     */
    @Test
    void shouldSaveOrUpdateBatchMixed() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            service.save(user(1L, "单元一号", 10L, ItUserStatus.ENABLED));

            ItUser existing = user(1L, "单元一号改", 30L, ItUserStatus.LOCKED);
            ItUser fresh = user(2L, "单元二号", 10L, ItUserStatus.DISABLED);
            assertThat(service.saveOrUpdateBatch(List.of(existing, fresh), 1)).isTrue();
            assertThat(rawCount()).isEqualTo(2L);
            assertThat(rawRow(1L).get("nickname")).isEqualTo("单元一号改");
            assertThat(((Number) rawRow(1L).get("dept_id")).longValue()).isEqualTo(30L);
            assertThat(rawRow(2L).get("username")).isEqualTo("junit_u2");
        });
    }

    /**
     * 批量更新：批量大小为 1 时逐条真实更新，各行列值全部生效。
     */
    @Test
    void shouldUpdateBatchById() {
        this.runner().run(context -> {
            ItUserService service = context.getBean(ItUserService.class);
            service.save(user(1L, "单元一号", 10L, ItUserStatus.ENABLED));
            service.save(user(2L, "单元二号", 20L, ItUserStatus.DISABLED));

            ItUser first = user(1L, "单元一号改", 40L, ItUserStatus.LOCKED);
            ItUser second = user(2L, "单元二号改", 50L, ItUserStatus.ENABLED);
            assertThat(service.updateBatchById(List.of(first, second), 1)).isTrue();
            assertThat(((Number) rawRow(1L).get("dept_id")).longValue()).isEqualTo(40L);
            assertThat(((Number) rawRow(1L).get("status")).intValue()).isEqualTo(2);
            assertThat(rawRow(2L).get("nickname")).isEqualTo("单元二号改");
            assertThat(((Number) rawRow(2L).get("status")).intValue()).isEqualTo(1);
        });
    }

    /**
     * Mapper 扩展默认方法：insertBatch/updateBatchById 经 Db 工具按实体类型从上下文解析
     * 真实 Mapper 批量执行，lambdaQuery 链式查询按真实数据过滤。
     */
    @Test
    void shouldBatchAndChainByMapperDefaults() {
        this.runner().run(context -> {
            ItUserMapper mapper = context.getBean(ItUserMapper.class);
            ItUser first = user(1L, "单元一号", 10L, ItUserStatus.ENABLED);
            ItUser second = user(2L, "单元二号", 20L, ItUserStatus.DISABLED);
            assertThat(mapper.insertBatch(List.of(first, second))).isTrue();
            assertThat(rawCount()).isEqualTo(2L);

            ItUser renamed = user(1L, "单元一号改", 10L, ItUserStatus.ENABLED);
            assertThat(mapper.updateBatchById(List.of(renamed))).isTrue();
            assertThat(rawRow(1L).get("nickname")).isEqualTo("单元一号改");

            List<ItUser> matched = mapper.lambdaQuery().eq(ItUser::getDeptId, 20L).list();
            assertThat(matched).hasSize(1);
            assertThat(matched.get(0).getId()).isEqualTo(2L);
        });
    }

    private static ItUser user(Long id, String nickname, Long deptId, ItUserStatus status) {
        ItUser user = new ItUser();
        user.setId(id);
        user.setUsername("junit_u" + id);
        user.setNickname(nickname);
        user.setDeptId(deptId);
        user.setStatus(status);
        user.setDeleted(0);
        return user;
    }
}
