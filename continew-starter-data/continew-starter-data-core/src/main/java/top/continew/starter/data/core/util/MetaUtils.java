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

package top.continew.starter.data.core.util;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.db.DbRuntimeException;
import cn.hutool.db.DbUtil;
import cn.hutool.db.meta.Column;
import cn.hutool.db.meta.MetaUtil;
import cn.hutool.db.meta.Table;
import cn.hutool.db.meta.TableType;
import top.continew.starter.core.exception.BusinessException;
import top.continew.starter.data.core.enums.DatabaseType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 数据库元数据信息工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class MetaUtils {

    private MetaUtils() {
    }

    /**
     * 获取数据库类型（如果获取不到数据库类型，则返回默认数据库类型）
     *
     * @param dataSource   数据源
     * @param defaultValue 默认数据库类型
     * @return 数据库类型
     * @since 1.4.1
     */
    public static DatabaseType getDatabaseTypeOrDefault(DataSource dataSource, DatabaseType defaultValue) {
        DatabaseType databaseType = getDatabaseType(dataSource);
        return null == databaseType ? defaultValue : databaseType;
    }

    /**
     * 获取数据库类型
     *
     * @param dataSource 数据源
     * @return 数据库类型
     * @since 1.4.1
     */
    public static DatabaseType getDatabaseType(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String databaseProductName = metaData.getDatabaseProductName();
            return DatabaseType.get(databaseProductName);
        } catch (SQLException e) {
            throw new BusinessException(e);
        }
    }

    /**
     * 获取所有表信息
     *
     * @param dataSource 数据源
     * @return 表信息列表
     */
    public static List<Table> getTables(DataSource dataSource) {
        return getTables(dataSource, null);
    }

    /**
     * 获取所有表信息
     *
     * @param dataSource 数据源
     * @param tableName  表名称
     * @return 表信息列表
     * @author looly
     * @since 2.7.2
     */
    public static List<Table> getTables(DataSource dataSource, String tableName) {
        List<Table> tables = new ArrayList<>();
        Connection conn = null;
        try {
            conn = dataSource.getConnection();
            String catalog = MetaUtil.getCatalog(conn);
            String schema = MetaUtil.getSchema(conn);
            final DatabaseMetaData metaData = conn.getMetaData();
            try (final ResultSet rs = metaData.getTables(catalog, schema, tableName, Convert
                .toStrArray(TableType.TABLE))) {
                if (null != rs) {
                    String name;
                    while (rs.next()) {
                        name = rs.getString("TABLE_NAME");
                        if (CharSequenceUtil.isNotBlank(name)) {
                            final Table table = Table.create(name);
                            table.setCatalog(catalog);
                            table.setSchema(schema);
                            table.setComment(MetaUtil.getRemarks(metaData, catalog, schema, name));
                            tables.add(table);
                        }
                    }
                }
            }
            return tables;
        } catch (Exception e) {
            throw new DbRuntimeException("Get tables error!", e);
        } finally {
            DbUtil.close(conn);
        }
    }

    /**
     * 获取所有列信息
     *
     * @param dataSource 数据源
     * @param tableName  表名称
     * @return 列信息列表
     */
    public static Collection<Column> getColumns(DataSource dataSource, String tableName) {
        Table table = MetaUtil.getTableMeta(dataSource, tableName);
        return table.getColumns();
    }
}
