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

package top.continew.starter.data.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.db.DbRuntimeException;
import cn.hutool.db.meta.Table;
import org.junit.jupiter.api.Test;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.enums.DatabaseType;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import javax.sql.DataSource;

/**
 * 数据库元数据工具测试
 *
 * <p>以 JDBC 元数据链路的 mock 替身验证类型识别、默认值回落、表信息读取与异常包装，
 * 不依赖真实数据库。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MetaUtilsTest {

    /**
     * 已登记的产品名应精确匹配到对应数据库类型。
     */
    @Test
    void shouldReturnMatchedDatabaseType() throws SQLException {
        DataSource dataSource = this.dataSourceOf("MySQL");
        assertThat(MetaUtils.getDatabaseType(dataSource)).isEqualTo(DatabaseType.MYSQL);
    }

    /**
     * 产品名比对不区分大小写。
     */
    @Test
    void shouldMatchDatabaseProductNameIgnoreCase() throws SQLException {
        DataSource dataSource = this.dataSourceOf("postgresql");
        assertThat(MetaUtils.getDatabaseType(dataSource)).isEqualTo(DatabaseType.POSTGRE_SQL);
    }

    /**
     * 未登记的产品名应回落到默认数据库类型。
     */
    @Test
    void shouldReturnDefaultWhenDatabaseTypeUnknown() throws SQLException {
        DataSource dataSource = this.dataSourceOf("Oracle");
        assertThat(MetaUtils.getDatabaseTypeOrDefault(dataSource, DatabaseType.MYSQL))
            .isEqualTo(DatabaseType.MYSQL);
    }

    /**
     * 能识别产品名时应返回实际类型而非默认值。
     */
    @Test
    void shouldReturnActualTypeWhenRecognized() throws SQLException {
        DataSource dataSource = this.dataSourceOf("MySQL");
        assertThat(MetaUtils.getDatabaseTypeOrDefault(dataSource, DatabaseType.POSTGRE_SQL))
            .isEqualTo(DatabaseType.MYSQL);
    }

    /**
     * 连接异常应包装为业务异常向上抛出。
     */
    @Test
    void shouldWrapSqlExceptionAsBusinessException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("connection refused"));
        assertThrows(BusinessException.class, () -> MetaUtils.getDatabaseType(dataSource));
    }

    /**
     * 表名空白的结果行应被跳过，仅返回有效表。
     */
    @Test
    void shouldSkipBlankTableNameRows() throws SQLException {
        ResultSet tablesResult = mock(ResultSet.class);
        when(tablesResult.next()).thenReturn(true, true, false);
        when(tablesResult.getString("TABLE_NAME")).thenReturn("sys_user", "sys_role", "   ");
        DataSource dataSource = this.tablesDataSourceOf(tablesResult);
        List<Table> tables = MetaUtils.getTables(dataSource);
        assertThat(tables).extracting(Table::getTableName).containsExactly("sys_user", "sys_role");
    }

    /**
     * 结果集为 null 或无数据行时应返回空表列表而非抛出。
     */
    @Test
    void shouldReturnEmptyTablesWhenNoRowsOrNoResultSet() throws SQLException {
        DataSource nullResultSetSource = this.tablesDataSourceOf(null);
        assertThat(MetaUtils.getTables(nullResultSetSource)).isEmpty();

        ResultSet tablesResult = mock(ResultSet.class);
        when(tablesResult.next()).thenReturn(false);
        DataSource emptySource = this.tablesDataSourceOf(tablesResult);
        assertThat(MetaUtils.getTables(emptySource)).isEmpty();
    }

    /**
     * 读取表信息时的连接异常应包装为 Hutool 运行时异常。
     */
    @Test
    void shouldWrapGetTablesFailureAsDbRuntimeException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("boom"));
        assertThrows(DbRuntimeException.class, () -> MetaUtils.getTables(dataSource));
    }

    /**
     * 列信息读取应返回表内的列集合（mock 空结果链路）。
     */
    @Test
    void shouldReturnColumnsFromTableMeta() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet emptyResult = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("MySQL");
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(emptyResult);
        assertThat(MetaUtils.getColumns(dataSource, "sys_user")).isEmpty();
    }

    /**
     * 构造返回指定产品名的数据源替身。
     *
     * @param productName 数据库产品名
     * @return 数据源替身
     */
    private DataSource dataSourceOf(String productName) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn(productName);
        return dataSource;
    }

    /**
     * 构造可返回指定表结果集的数据源替身，注释与列信息查询均返回空结果。
     *
     * <p>Hutool 的备注查询同样走 {@code DatabaseMetaData.getTables}，故表结果集仅在
     * 首次查询返回，后续备注查询统一返回无行结果集，避免备注查询消费表行的遍历游标。</p>
     *
     * @param tablesResult 表信息结果集（可为 null，模拟查询无结果）
     * @return 数据源替身
     */
    private DataSource tablesDataSourceOf(ResultSet tablesResult) throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        DatabaseMetaData metaData = mock(DatabaseMetaData.class);
        ResultSet emptyResult = mock(ResultSet.class);
        ResultSet remarksResult = mock(ResultSet.class);
        when(remarksResult.next()).thenReturn(false);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables(any(), any(), any(), any()))
            .thenReturn(tablesResult, remarksResult);
        when(metaData.getColumns(any(), any(), any(), any())).thenReturn(emptyResult);
        when(metaData.getPrimaryKeys(any(), any(), any())).thenReturn(emptyResult);
        when(metaData.getIndexInfo(any(), any(), any(), any(Boolean.class), any(Boolean.class)))
            .thenReturn(emptyResult);
        return dataSource;
    }
}
