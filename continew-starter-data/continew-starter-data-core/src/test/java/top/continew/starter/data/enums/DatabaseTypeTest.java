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

package top.continew.starter.data.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 数据库类型枚举测试
 *
 * <p>覆盖产品名匹配（大小写不敏感、未知与空值回落）、产品名暴露，以及两种数据库
 * 的 find_in_set 兼容 SQL 生成格式。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DatabaseTypeTest {

    /**
     * 产品名匹配不区分大小写。
     */
    @Test
    void shouldMatchDatabaseProductNameIgnoreCase() {
        assertThat(DatabaseType.get("MySQL")).isEqualTo(DatabaseType.MYSQL);
        assertThat(DatabaseType.get("mysql")).isEqualTo(DatabaseType.MYSQL);
        assertThat(DatabaseType.get("PostgreSQL")).isEqualTo(DatabaseType.POSTGRE_SQL);
        assertThat(DatabaseType.get("POSTGRESQL")).isEqualTo(DatabaseType.POSTGRE_SQL);
    }

    /**
     * 未知或空白产品名应返回 null 而非抛出。
     */
    @Test
    void shouldReturnNullForUnknownOrBlankProduct() {
        assertThat(DatabaseType.get("Oracle")).isNull();
        assertThat(DatabaseType.get("")).isNull();
        assertThat(DatabaseType.get(null)).isNull();
    }

    /**
     * 枚举应暴露登记时的原始产品名。
     */
    @Test
    void shouldExposeDatabaseProductName() {
        assertThat(DatabaseType.MYSQL.getDatabase()).isEqualTo("MySQL");
        assertThat(DatabaseType.POSTGRE_SQL.getDatabase()).isEqualTo("PostgreSQL");
    }

    /**
     * MySQL 的 find_in_set 应生成原生函数调用格式。
     */
    @Test
    void shouldBuildFindInSetForMysql() {
        assertThat(DatabaseType.MYSQL.findInSet(1, "t.role_ids"))
            .isEqualTo("find_in_set('1', t.role_ids) <> 0");
    }

    /**
     * PostgreSQL 无 find_in_set 函数，应生成 position 模拟格式。
     */
    @Test
    void shouldBuildFindInSetForPostgreSql() {
        assertThat(DatabaseType.POSTGRE_SQL.findInSet(2, "t.role_ids"))
            .isEqualTo("(select position(',2,' in ','||t.role_ids||',')) <> 0");
    }
}
