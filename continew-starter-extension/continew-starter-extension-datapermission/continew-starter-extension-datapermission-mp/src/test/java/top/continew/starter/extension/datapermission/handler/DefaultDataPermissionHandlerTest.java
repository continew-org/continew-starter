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

package top.continew.starter.extension.datapermission.handler;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.schema.Column;
import org.junit.jupiter.api.Test;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import top.continew.starter.extension.datapermission.exception.DataPermissionException;
import top.continew.starter.extension.datapermission.model.UserData;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;

/**
 * 默认数据权限处理器测试。
 *
 * <p>数据权限属于安全边界。用户上下文、Mapper 反射或表达式构造失败时必须中断查询，
 * 不能返回原始 {@code WHERE} 条件继续执行，否则会静默扩大数据可见范围。</p>
 *
 * @author Charles7c
 * @since 2.17.0
 */
class DefaultDataPermissionHandlerTest {

    private static final String ANNOTATED_STATEMENT = TestMapper.class.getName() + ".selectList";

    /**
     * 用户数据无效时应失败关闭。
     */
    @Test
    void shouldRejectInvalidUserData() {
        DefaultDataPermissionHandler handler = new DefaultDataPermissionHandler(
            new TestUserDataProvider(true, null));

        assertThrows(DataPermissionException.class,
            () -> handler.getSqlSegment(null, ANNOTATED_STATEMENT));
    }

    /**
     * Mapper 无法反射时应失败关闭。
     */
    @Test
    void shouldRejectUnknownMapper() {
        DefaultDataPermissionHandler handler = new DefaultDataPermissionHandler(
            new TestUserDataProvider(true, null));

        assertThrows(DataPermissionException.class,
            () -> handler.getSqlSegment(null, "example.missing.Mapper.selectList"));
    }

    /**
     * 没有数据权限注解的方法仍应保留原查询条件。
     */
    @Test
    void shouldKeepWhereForUnannotatedMethod() {
        DefaultDataPermissionHandler handler = new DefaultDataPermissionHandler(
            new TestUserDataProvider(true, null));
        Expression where = new EqualsTo(new Column("id"), new LongValue(1));

        Expression result = handler.getSqlSegment(where,
            TestMapper.class.getName() + ".selectWithoutDataPermission");

        assertSame(where, result);
    }

    /**
     * Provider 明确关闭过滤时应保留原查询条件。
     */
    @Test
    void shouldKeepWhereWhenFilteringIsDisabled() {
        DefaultDataPermissionHandler handler = new DefaultDataPermissionHandler(
            new TestUserDataProvider(false, null));
        Expression where = new EqualsTo(new Column("id"), new LongValue(1));

        Expression result = handler.getSqlSegment(where, ANNOTATED_STATEMENT);

        assertSame(where, result);
    }

    /**
     * 测试 Mapper。
     */
    interface TestMapper {

        /**
         * 带数据权限的方法。
         */
        @DataPermission
        void selectList();

        /**
         * 不带数据权限的方法。
         */
        void selectWithoutDataPermission();
    }

    /**
     * 测试用户数据提供者。
     *
     * @param filter   是否过滤
     * @param userData 用户数据
     */
    private record TestUserDataProvider(boolean filter,
        UserData userData) implements DataPermissionUserDataProvider {

        @Override
        public boolean isFilter() {
            return filter;
        }

        @Override
        public UserData getUserData() {
            return userData;
        }
    }
}
