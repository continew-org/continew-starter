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

import static org.assertj.core.api.Assertions.assertThat;

import com.mybatisflex.core.paginate.Page;
import org.junit.jupiter.api.Test;
import top.continew.starter.extension.crud.model.entity.BaseIdDO;

import java.util.List;

/**
 * {@link PageResp} 分页信息单元测试。
 *
 * <p>覆盖 MyBatis Flex Page 构建、类型转换构建、内存列表分页与空数据边界。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class PageRespTest {

    /**
     * 分页数据源实体
     */
    static class UserDO extends BaseIdDO {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    /**
     * 分页目标类型
     */
    static class UserVO {

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private UserDO createUser(Long id, String name) {
        UserDO user = new UserDO();
        user.setId(id);
        user.setName(name);
        return user;
    }

    /**
     * 基于类型转换的 Flex Page 构建应拷贝记录并保留总行数。
     */
    @Test
    void shouldBuildFromFlexPageWithConvert() {
        Page<UserDO> page = Page.of(1, 10, 25);
        page.setRecords(List.of(createUser(1L, "alice"), createUser(2L, "bob")));

        PageResp<UserVO> pageResp = PageResp.build(page, UserVO.class);

        assertThat(pageResp.getTotal()).isEqualTo(25L);
        assertThat(pageResp.getList()).hasSize(2);
        assertThat(pageResp.getList().get(0).getName()).isEqualTo("alice");
        assertThat(pageResp.getList().get(1).getName()).isEqualTo("bob");
    }

    /**
     * 基于同类型 Flex Page 的构建应保留原记录与总行数。
     */
    @Test
    void shouldBuildFromFlexPageWithoutConvert() {
        List<UserDO> records = List.of(createUser(1L, "alice"));
        Page<UserDO> page = Page.of(1, 10, 1);
        page.setRecords(records);

        PageResp<UserDO> pageResp = PageResp.build(page);

        assertThat(pageResp.getTotal()).isEqualTo(1L);
        assertThat(pageResp.getList()).isSameAs(records);
    }

    /**
     * Flex Page 为 null 时应返回空分页信息。
     */
    @Test
    void shouldReturnEmptyWhenFlexPageIsNull() {
        assertThat(PageResp.build((Page<UserDO>) null, UserVO.class)).satisfies(pageResp -> {
            assertThat(pageResp.getTotal()).isZero();
            assertThat(pageResp.getList()).isEmpty();
        });
    }

    /**
     * 内存列表分页应按页码与条数正确切片。
     */
    @Test
    void shouldSliceInMemoryList() {
        List<Integer> list = List.of(1, 2, 3, 4, 5);

        assertThat(PageResp.build(2, 2, list)).satisfies(pageResp -> {
            assertThat(pageResp.getTotal()).isEqualTo(5L);
            assertThat(pageResp.getList()).containsExactly(3, 4);
        });
    }

    /**
     * 页码超出数据范围时应返回空列表，总数不受影响。
     */
    @Test
    void shouldReturnEmptyListWhenPageOutOfRange() {
        List<Integer> list = List.of(1, 2, 3);

        assertThat(PageResp.build(2, 3, list)).satisfies(pageResp -> {
            assertThat(pageResp.getTotal()).isEqualTo(3L);
            assertThat(pageResp.getList()).isEmpty();
        });
    }

    /**
     * 列表为 null 或空时应返回空分页信息。
     */
    @Test
    void shouldReturnEmptyWhenListIsNullOrEmpty() {
        assertThat(PageResp.build(1, 10, null)).satisfies(pageResp -> {
            assertThat(pageResp.getTotal()).isZero();
            assertThat(pageResp.getList()).isEmpty();
        });
        assertThat(PageResp.build(1, 10, List.<Integer>of())).satisfies(pageResp -> {
            assertThat(pageResp.getTotal()).isZero();
            assertThat(pageResp.getList()).isEmpty();
        });
    }

    /**
     * 首页切片应返回原列表的子列表视图，元素引用保持不变。
     */
    @Test
    void shouldReturnSubListViewOfOriginalList() {
        UserDO alice = createUser(1L, "alice");
        UserDO bob = createUser(2L, "bob");
        List<UserDO> records = List.of(alice, bob);

        PageResp<UserDO> pageResp = PageResp.build(1, 10, records);

        assertThat(pageResp.getList()).containsExactly(alice, bob);
        assertThat(pageResp.getList().get(0)).isSameAs(alice);
        assertThat(pageResp.getList().get(1)).isSameAs(bob);
    }
}
