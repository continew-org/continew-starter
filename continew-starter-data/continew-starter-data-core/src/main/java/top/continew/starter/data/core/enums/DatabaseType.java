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

package top.continew.starter.data.core.enums;

import top.continew.starter.data.core.function.ISqlFunction;

/**
 * 数据库类型枚举
 *
 * @author Charles7c
 * @since 1.4.1
 */
public enum DatabaseType implements ISqlFunction {

    /**
     * MySQL
     */
    MYSQL("MySQL") {
        @Override
        public String findInSet() {
            return "find_in_set({0}, {1}) <> 0";
        }
    },

    /**
     * PostgreSQL
     */
    POSTGRE_SQL("PostgreSQL") {
        @Override
        public String findInSet() {
            return "(select position(',{0},' in ','||{1}||',')) <> 0";
        }
    },;

    private final String database;

    DatabaseType(String database) {
        this.database = database;
    }

    /**
     * 获取数据库类型
     *
     * @param database 数据库
     */
    public static DatabaseType get(String database) {
        for (DatabaseType databaseType : DatabaseType.values()) {
            if (databaseType.database.equalsIgnoreCase(database)) {
                return databaseType;
            }
        }
        return null;
    }

    public String getDatabase() {
        return database;
    }
}
