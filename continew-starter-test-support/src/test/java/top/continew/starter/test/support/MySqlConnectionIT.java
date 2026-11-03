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

package top.continew.starter.test.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * MySQL 连接集成测试：验证 Testcontainers 基座可拉起真实 MySQL 并完成 JDBC 交互
 *
 * <p>本机无 Docker 时由 {@link EnabledIfDockerAvailable} 自动跳过，CI 环境真实执行。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
@Testcontainers
class MySqlConnectionIT {

    @Container
    private static final MySQLContainer MYSQL =
        new MySQLContainer(DockerImageName.parse(ContainerImages.MYSQL_IMAGE));

    /**
     * JDBC 连接容器后执行 SELECT 1 应返回 1
     */
    @Test
    void shouldSelectOneFromMysqlContainer() throws SQLException {
        try (
            Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(),
                MYSQL.getUsername(), MYSQL.getPassword());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT 1")) {
            assertTrue(resultSet.next());
            assertEquals(1, resultSet.getInt(1));
        }
    }
}
