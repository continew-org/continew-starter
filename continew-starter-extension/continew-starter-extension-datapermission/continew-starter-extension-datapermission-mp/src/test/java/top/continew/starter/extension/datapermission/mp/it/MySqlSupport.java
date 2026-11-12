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

package top.continew.starter.extension.datapermission.mp.it;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.mysql.cj.jdbc.Driver;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.extension.datapermission.autoconfigure.DataPermissionAutoConfiguration;
import top.continew.starter.extension.datapermission.model.UserData;
import top.continew.starter.extension.datapermission.mp.it.mapper.ItDpUserMapper;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;
import top.continew.starter.test.support.ContainerImages;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

/**
 * 数据权限 MP 扩展集成测试基座：整个 Failsafe JVM 共享一个真实 MySQL 容器与数据源。
 *
 * <p>本模块无 MyBatis Spring Boot 自动配置与 mybatis-spring，测试侧以 MP 自带
 * {@code MybatisSqlSessionFactoryBean} 手动装配会话工厂并以 mybatis 原生代理注册 Mapper；
 * {@code MetaUtils.getDatabaseType} 经 {@code SpringUtil} 从容器取真实数据源检测数据库类型，
 * 因此容器与数据源跨测试类共享，由 JVM 退出时的 Ryuk 守护进程统一回收。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
abstract class MySqlSupport {

    /**
     * 共享测试表：用户表（数据权限主表）、部门表（本部门及以下子查询）、角色部门关联表（自定义子查询）
     */
    private static final String DDL = """
        DROP TABLE IF EXISTS dp_it_user;
        CREATE TABLE dp_it_user (
          id BIGINT NOT NULL,
          username VARCHAR(64) NOT NULL,
          dept_id BIGINT DEFAULT NULL,
          create_user BIGINT DEFAULT NULL,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        DROP TABLE IF EXISTS dp_it_dept;
        CREATE TABLE dp_it_dept (
          id BIGINT NOT NULL,
          ancestors VARCHAR(255) DEFAULT NULL,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        DROP TABLE IF EXISTS dp_it_role_dept;
        CREATE TABLE dp_it_role_dept (
          role_id BIGINT NOT NULL,
          dept_id BIGINT NOT NULL,
          PRIMARY KEY (role_id, dept_id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        """;

    private static final MySQLContainer MYSQL =
        new MySQLContainer(DockerImageName.parse(ContainerImages.MYSQL_IMAGE));
    private static volatile DataSource sharedDataSource;
    private static volatile boolean filterEnabled;
    private static volatile UserData userData;

    /**
     * 启动共享的真实 MySQL 容器并重建测试表。
     */
    @BeforeAll
    static void startSharedMysql() {
        if (sharedDataSource != null) {
            executeScript(DDL);
            return;
        }
        MYSQL.start();
        sharedDataSource = new UnpooledDataSource(Driver.class.getName(), MYSQL.getJdbcUrl(),
            MYSQL.getUsername(), MYSQL
                .getPassword());
        executeScript(DDL);
    }

    /**
     * 物理清空测试表并复位数据权限过滤状态，保证用例间隔离。
     */
    @BeforeEach
    void resetState() {
        executeUpdate("DELETE FROM dp_it_user");
        executeUpdate("DELETE FROM dp_it_dept");
        executeUpdate("DELETE FROM dp_it_role_dept");
        applyUserData(false, null);
    }

    /**
     * 构建装配真实数据源与数据权限自动配置的上下文运行器。
     *
     * <p>数据权限自动配置默认启用（matchIfMissing），要求容器提供用户数据提供者扩展点。</p>
     *
     * @return 上下文运行器
     */
    ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(DataPermissionAutoConfiguration.class))
            .withUserConfiguration(ItConfig.class)
            .withBean(SpringUtil.class);
    }

    /**
     * 设置当前用例的数据权限过滤状态。
     *
     * @param enabled  是否启用数据权限过滤
     * @param userData 当前用户数据（含角色与数据权限范围）
     */
    static void applyUserData(boolean enabled, UserData userData) {
        filterEnabled = enabled;
        MySqlSupport.userData = userData;
    }

    /**
     * 获取共享的真实数据源。
     *
     * @return 与共享容器连接的数据源
     */
    static DataSource dataSource() {
        return sharedDataSource;
    }

    /**
     * 执行更新类 SQL（建表与预置数据），用于绕过被测模块直接核对真实表数据。
     *
     * @param sql 更新 SQL
     * @return 影响行数
     */
    static int executeUpdate(String sql) {
        try (
            Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(),
                MYSQL.getUsername(), MYSQL
                    .getPassword());
            PreparedStatement statement = connection.prepareStatement(sql)) {
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("执行 SQL 失败: " + sql, e);
        }
    }

    /**
     * 在共享容器上执行多语句建表脚本。
     *
     * @param script 建表 SQL 脚本（分号分隔）
     */
    static void executeScript(String script) {
        for (String statement : script.split(";")) {
            if (!statement.isBlank()) {
                executeUpdate(statement.trim());
            }
        }
    }

    /**
     * 按主键读取用户表一行真实数据。
     *
     * @param id 主键值
     * @return 数据库原始行（列名为键），无行时返回空 Map
     */
    static Map<String, Object> rawUserRow(Long id) {
        List<Map<String, Object>> rows = queryForList("SELECT * FROM dp_it_user WHERE id = " + id);
        return rows.isEmpty() ? Map.of() : rows.getFirst();
    }

    /**
     * 查询真实表数据行。
     *
     * @param sql 查询 SQL
     * @return 行列表（列名为键）
     */
    static List<Map<String, Object>> queryForList(String sql) {
        try (
            Connection connection = DriverManager.getConnection(MYSQL.getJdbcUrl(),
                MYSQL.getUsername(), MYSQL
                    .getPassword());
            PreparedStatement statement = connection.prepareStatement(sql);
            ResultSet resultSet = statement
                .executeQuery()) {
            int columnCount = resultSet.getMetaData().getColumnCount();
            List<Map<String, Object>> rows = new ArrayList<>();
            while (resultSet.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(resultSet.getMetaData().getColumnLabel(i).toLowerCase(),
                        resultSet.getObject(i));
                }
                rows.add(row);
            }
            return rows;
        } catch (SQLException e) {
            throw new IllegalStateException("查询 SQL 失败: " + sql, e);
        }
    }

    /**
     * 测试装配配置：真实数据源、数据权限用户数据扩展点、拦截器链与手动装配的会话工厂。
     *
     * <p>拦截器经 {@code MybatisSqlSessionFactoryBean} 插件链生效，Mapper 以 mybatis 原生代理注册
     * 于自动提交会话。</p>
     *
     * @author Charles7c
     * @since 3.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class ItConfig {

        @Bean(destroyMethod = "")
        DataSource dataSource() {
            return MySqlSupport.dataSource();
        }

        @Bean
        DataPermissionUserDataProvider dataPermissionUserDataProvider() {
            return new ItUserDataProvider();
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor(
            DataPermissionInterceptor dataPermissionInterceptor) {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(dataPermissionInterceptor);
            return interceptor;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource,
            MybatisPlusInterceptor interceptor) {
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration
                .setEnvironment(new Environment("dp-it", new JdbcTransactionFactory(), dataSource));
            configuration.addInterceptor(interceptor);
            configuration.setLogImpl(NoLoggingImpl.class);
            return new MybatisSqlSessionFactoryBuilder().build(configuration);
        }

        @Bean
        ItDpUserMapper itDpUserMapper(SqlSessionFactory sqlSessionFactory) {
            sqlSessionFactory.getConfiguration().addMapper(ItDpUserMapper.class);
            return sqlSessionFactory.openSession(true).getMapper(ItDpUserMapper.class);
        }
    }

    /**
     * 数据权限用户数据扩展点实现：启停与当前用户数据由基座静态状态控制
     *
     * @author Charles7c
     * @since 3.1.0
     */
    static final class ItUserDataProvider implements DataPermissionUserDataProvider {

        @Override
        public boolean isFilter() {
            return filterEnabled;
        }

        @Override
        public UserData getUserData() {
            return userData;
        }
    }
}
