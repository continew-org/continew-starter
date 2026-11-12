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

package top.continew.starter.extension.tenant.mp.it;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import com.baomidou.dynamic.datasource.creator.DefaultDataSourceCreator;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
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
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.extension.tenant.autoconfigure.TenantAutoConfiguration;
import top.continew.starter.extension.tenant.config.TenantDataSource;
import top.continew.starter.extension.tenant.config.TenantProvider;
import top.continew.starter.extension.tenant.context.TenantContext;
import top.continew.starter.extension.tenant.context.TenantContextHolder;
import top.continew.starter.extension.tenant.enums.TenantIsolationLevel;
import top.continew.starter.extension.tenant.mp.it.controller.ItControllerService;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantConfigMapper;
import top.continew.starter.extension.tenant.mp.it.mapper.ItTenantUserMapper;
import top.continew.starter.extension.tenant.mp.it.service.ItIgnoreService;
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
 * 租户 MP 扩展集成测试基座：整个 Failsafe JVM 共享一个真实 MySQL 容器与动态数据源。
 *
 * <p>本模块无 MyBatis Spring Boot 自动配置，测试侧以 MP 的 {@code MybatisConfiguration}
 * 手动装配会话工厂并以 mybatis 原生代理注册 Mapper；数据源级隔离依赖 dynamic-datasource，
 * 基座构造真实 {@code DynamicRoutingDataSource}（primary=master 指向容器），租户上下文与
 * 拦截器链跨测试类共享，容器由 JVM 退出时的 Ryuk 守护进程统一回收。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
abstract class MySqlSupport {

    /**
     * 数据源级隔离租户 ID：提供者对该租户返回 DATASOURCE 级上下文
     */
    static final long DATASOURCE_TENANT_ID = 99L;

    /**
     * 共享测试表：用户表（行级隔离主表，含租户列）、配置表（忽略租户表，无租户列）
     */
    private static final String DDL = """
        DROP TABLE IF EXISTS tenant_user;
        CREATE TABLE tenant_user (
          id BIGINT NOT NULL,
          username VARCHAR(64) NOT NULL,
          tenant_id BIGINT NOT NULL,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        DROP TABLE IF EXISTS tenant_config;
        CREATE TABLE tenant_config (
          id BIGINT NOT NULL,
          config_key VARCHAR(64) NOT NULL,
          config_value VARCHAR(255) DEFAULT NULL,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        """;

    private static final MySQLContainer MYSQL =
        new MySQLContainer(DockerImageName.parse(ContainerImages.MYSQL_IMAGE));
    private static volatile DataSource sharedDataSource;

    /**
     * 启动共享的真实 MySQL 容器并构造动态数据源（primary=master 指向容器）。
     */
    @BeforeAll
    static void startSharedMysql() {
        if (sharedDataSource != null) {
            executeScript(DDL);
            return;
        }
        MYSQL.start();
        UnpooledDataSource master =
            new UnpooledDataSource(Driver.class.getName(), MYSQL.getJdbcUrl(), MYSQL
                .getUsername(), MYSQL.getPassword());
        DynamicRoutingDataSource routing = new DynamicRoutingDataSource(List.of());
        routing.setPrimary("master");
        routing.setStrict(false);
        routing.addDataSource("master", master);
        routing.afterPropertiesSet();
        sharedDataSource = routing;
        executeScript(DDL);
    }

    /**
     * 物理清空测试表并复位租户上下文，保证用例间隔离。
     */
    @BeforeEach
    void resetState() {
        executeUpdate("DELETE FROM tenant_user");
        executeUpdate("DELETE FROM tenant_config");
        TenantContextHolder.clear();
    }

    /**
     * 构建装配真实数据源与租户自动配置的上下文运行器。
     *
     * <p>租户自动配置默认启用，要求容器提供租户提供者扩展点；忽略表经配置属性注入。</p>
     *
     * @return 上下文运行器
     */
    ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withPropertyValues("continew-starter.tenant.ignore-tables=tenant_config")
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
            .withUserConfiguration(ItConfig.class)
            .withBean(SpringUtil.class);
    }

    /**
     * 构建仅含数据源基建的最小运行器：不提供租户提供者，用于验证缺省提供者的装配失败行为。
     *
     * @return 上下文运行器
     */
    ApplicationContextRunner bareRunner() {
        return new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TenantAutoConfiguration.class))
            .withUserConfiguration(BareConfig.class)
            .withBean(SpringUtil.class);
    }

    /**
     * 设置当前用例的行级租户上下文。
     *
     * @param tenantId 当前租户 ID
     */
    static void applyTenant(Long tenantId) {
        TenantContextHolder.setContext(tenantContextOf(tenantId.toString()));
    }

    /**
     * 设置当前用例的指定隔离级租户上下文。
     *
     * @param tenantId       当前租户 ID
     * @param isolationLevel 租户隔离级别
     */
    static void applyTenant(Long tenantId, TenantIsolationLevel isolationLevel) {
        TenantContext context = tenantContextOf(tenantId.toString());
        context.setIsolationLevel(isolationLevel);
        if (TenantIsolationLevel.DATASOURCE.equals(isolationLevel)) {
            TenantDataSource dataSource = new TenantDataSource();
            dataSource.setPoolName("master");
            context.setDataSource(dataSource);
        }
        TenantContextHolder.setContext(context);
    }

    /**
     * 构建租户上下文：数据源级隔离租户（99）携带 master 池配置，其余默认行级隔离。
     *
     * @param tenantIdAsString 租户 ID 文本
     * @return 租户上下文
     */
    static TenantContext tenantContextOf(String tenantIdAsString) {
        TenantContext context = new TenantContext();
        context.setTenantId(Long.valueOf(tenantIdAsString));
        if (DATASOURCE_TENANT_ID == context.getTenantId()) {
            context.setIsolationLevel(TenantIsolationLevel.DATASOURCE);
            TenantDataSource dataSource = new TenantDataSource();
            dataSource.setPoolName("master");
            context.setDataSource(dataSource);
        }
        return context;
    }

    /**
     * 获取共享的真实动态数据源。
     *
     * @return 与共享容器连接的动态数据源
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
        List<Map<String, Object>> rows = queryForList("SELECT * FROM tenant_user WHERE id = " + id);
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
     * 测试装配配置：真实动态数据源、租户提供者、拦截器链、手动会话工厂与业务实现。
     *
     * <p>租户忽略切面由租户自动配置注册，经 AOP 代理对 {@code @TenantIgnore} 方法生效。</p>
     *
     * @author Charles7c
     * @since 3.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    static class ItConfig {

        @Bean(destroyMethod = "")
        DataSource dataSource() {
            return MySqlSupport.dataSource();
        }

        @Bean
        DefaultDataSourceCreator defaultDataSourceCreator() {
            return new DefaultDataSourceCreator();
        }

        @Bean
        TenantProvider tenantProvider() {
            return (tenantId, isVerify) -> MySqlSupport.tenantContextOf(tenantId);
        }

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor(
            com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor tenantLineInnerInterceptor) {
            MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
            interceptor.addInnerInterceptor(tenantLineInnerInterceptor);
            return interceptor;
        }

        @Bean
        SqlSessionFactory sqlSessionFactory(DataSource dataSource,
            MybatisPlusInterceptor interceptor) {
            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setEnvironment(
                new Environment("tenant-it", new JdbcTransactionFactory(), dataSource));
            configuration.addInterceptor(interceptor);
            configuration.setLogImpl(NoLoggingImpl.class);
            return new MybatisSqlSessionFactoryBuilder().build(configuration);
        }

        @Bean
        ItTenantUserMapper tenantUserMapper(SqlSessionFactory sqlSessionFactory) {
            sqlSessionFactory.getConfiguration().addMapper(ItTenantUserMapper.class);
            return sqlSessionFactory.openSession(true).getMapper(ItTenantUserMapper.class);
        }

        @Bean
        ItTenantConfigMapper tenantConfigMapper(SqlSessionFactory sqlSessionFactory) {
            sqlSessionFactory.getConfiguration().addMapper(ItTenantConfigMapper.class);
            return sqlSessionFactory.openSession(true).getMapper(ItTenantConfigMapper.class);
        }

        @Bean
        ItIgnoreService ignoreService(ItTenantUserMapper tenantUserMapper) {
            return new ItIgnoreService(tenantUserMapper);
        }

        @Bean
        ItControllerService controllerService(ItTenantUserMapper tenantUserMapper) {
            return new ItControllerService(tenantUserMapper);
        }
    }

    /**
     * 最小装配配置：仅数据源基建，不提供租户提供者（验证缺省提供者装配失败行为）
     *
     * @author Charles7c
     * @since 3.1.0
     */
    @Configuration(proxyBeanMethods = false)
    static class BareConfig {

        @Bean(destroyMethod = "")
        DataSource dataSource() {
            return MySqlSupport.dataSource();
        }

        @Bean
        DefaultDataSourceCreator defaultDataSourceCreator() {
            return new DefaultDataSourceCreator();
        }
    }
}
