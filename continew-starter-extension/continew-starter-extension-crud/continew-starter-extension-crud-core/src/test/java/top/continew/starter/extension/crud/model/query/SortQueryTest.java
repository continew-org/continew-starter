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

import cn.hutool.extra.spring.SpringUtil;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import top.continew.starter.core.exception.BadRequestException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 排序参数解析测试。
 *
 * <p>对应 GHSA-g8qc-r85v-gqpp（ORDER BY 注入）、GHSA-jfcv-24mv-r9c3（tree 端点
 * {@code sort} 注入）与 GHSA-3r3w-5g4r-3xph（分页端点 {@code toPage} 注入）。三者的
 * 共同入口是 {@link SortQuery#getSort()}：此处若放过非法字段，后续
 * {@code QueryWrapper.orderBy} 与 {@code OrderItem.setColumn} 会把整串原样拼入 SQL。
 * 测试固化「入口即拒绝」这一不变量。</p>
 *
 * <p>{@code ValidationUtils} 通过 {@code Validator} 的静态字段经 Hutool
 * {@code SpringUtil} 获取 {@code jakarta.validation.Validator}，无法脱离 Spring 上下文
 * 使用；此处以最小上下文注册校验器与 {@code SpringUtil}。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
@SpringJUnitConfig(SortQueryTest.ValidatorConfig.class)
class SortQueryTest {

    @Configuration(proxyBeanMethods = false)
    @Import(SpringUtil.class)
    static class ValidatorConfig {

        /**
         * 排序字段校验走的是纯正则路径，不会调用校验器实例；此处仅提供占位 Bean，
         * 满足 {@code Validator} 静态初始化的依赖要求。
         *
         * @return 校验器占位实现
         */
        @Bean
        jakarta.validation.Validator jakartaValidator() {
            return mock(jakarta.validation.Validator.class);
        }
    }

    /**
     * 常规排序参数应被正确解析。
     *
     * <p>注意：{@code getSort()} 要求排序参数至少两个元素——单元素形式会被
     * 「排序条件无效」拒绝，因此这里按受支持的写法验证。</p>
     */
    @Test
    void shouldParseRegularSortParameter() {
        Sort sort = new SortQuery("createTime,desc", "id,asc").getSort();
        assertEquals(2, sort.toList().size());
        Sort.Order order = sort.toList().get(0);
        assertEquals("createTime", order.getProperty());
        assertTrue(order.isDescending());
    }

    /**
     * 「字段名数组 + 方向数组」形式（{@code sort=createTime&sort=desc}）应被正确解析。
     */
    @Test
    void shouldParseFieldAndDirectionArrayForm() {
        Sort sort = new SortQuery("createTime", "desc").getSort();
        assertEquals(1, sort.toList().size());
        assertEquals("createTime", sort.toList().get(0).getProperty());
    }

    /**
     * 带表别名的字段名应通过校验。
     */
    @Test
    void shouldAcceptTableAliasPrefixedField() {
        assertDoesNotThrow(() -> new SortQuery("u.createTime,asc", "id,asc").getSort());
    }

    /**
     * GHSA-g8qc-r85v-gqpp 的核心 payload：表达式与 {@code #} 注释组合后可同时骗过关键字
     * 黑名单与「只校验最后一段字段名」的旧实现，整串被原样拼入 ORDER BY。
     */
    @Test
    void shouldRejectExpressionWithHashCommentSuffix() {
        assertThrows(BadRequestException.class,
            () -> new SortQuery("(select 1 from dual)#x.id,desc", "id,asc").getSort());
        assertThrows(BadRequestException.class, () -> new SortQuery("id)#,desc", "id,asc")
            .getSort());
    }

    /**
     * GHSA-jfcv-24mv-r9c3 报告的 tree 端点注入形态必须被拒绝。
     */
    @Test
    void shouldRejectTreeEndpointInjection() {
        assertThrows(BadRequestException.class, () -> new SortQuery("id;drop table sys_user,asc",
            "id,asc").getSort());
        assertThrows(BadRequestException.class, () -> new SortQuery("id --,asc", "id,asc")
            .getSort());
        assertThrows(BadRequestException.class, () -> new SortQuery("id/*x*/,asc", "id,asc")
            .getSort());
    }

    /**
     * GHSA-3r3w-5g4r-3xph 报告的分页端点 payload（{@code toPage} 会把字段转成
     * {@code OrderItem.setColumn}）必须在入口就被拒绝。
     */
    @Test
    void shouldRejectPaginationEndpointInjection() {
        assertThrows(BadRequestException.class, () -> new SortQuery("(select 1)#t1.id,desc",
            "id,asc").getSort());
        assertThrows(BadRequestException.class, () -> new SortQuery("if(1=1,id,name),asc",
            "id,asc").getSort());
    }

    /**
     * 含引号与空白字符的字段名必须被拒绝。
     */
    @Test
    void shouldRejectQuotesAndWhitespace() {
        assertThrows(BadRequestException.class, () -> new SortQuery("id' ,asc", "id,asc")
            .getSort());
        assertThrows(BadRequestException.class, () -> new SortQuery("id asc,desc", "id,asc")
            .getSort());
    }
}
