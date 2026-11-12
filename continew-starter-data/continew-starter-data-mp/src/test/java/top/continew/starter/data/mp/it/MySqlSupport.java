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

package top.continew.starter.data.mp.it;

import static org.mockito.Mockito.mock;

import cn.hutool.extra.spring.SpringUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.data.autoconfigure.MybatisPlusAutoConfiguration;
import top.continew.starter.data.mp.it.service.ItUserService;
import top.continew.starter.test.support.ContainerImages;

import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;
import javax.sql.DataSource;

/**
 * MyBatis Plus 集成测试基座：整个 Failsafe JVM 共享一个真实 MySQL 容器与连接池。
 *
 * <p>MP 批量工具（Db/SqlHelper）经 Spring 上下文按类型解析 Mapper、实体 TableInfo 随 Mapper
 * 注册后 JVM 存续期内不可重建，因此容器与连接池必须跨测试类共享，容器由 JVM 退出时的 Ryuk
 * 守护进程统一回收。测试类只依赖 {@code @EnabledIfDockerAvailable} 门禁，无 Docker 时整体跳过。</p>
 *
 * <p>DataSource 以 {@code destroyMethod = ""} 注册：同一实例被各用例上下文共享，销毁方法若被
 * 推断触发会在上下文关闭时关掉连接池，殃及后续用例。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
abstract class MySqlSupport {

    /**
     * 共享测试表：覆盖逻辑删除标记位与枚举值列
     */
    private static final String DDL = """
        DROP TABLE IF EXISTS mp_it_user;
        CREATE TABLE mp_it_user (
          id BIGINT NOT NULL,
          username VARCHAR(64) NOT NULL,
          nickname VARCHAR(64) DEFAULT NULL,
          dept_id BIGINT DEFAULT NULL,
          status INT DEFAULT NULL,
          deleted INT NOT NULL DEFAULT 0,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        """;

    private static final MySQLContainer MYSQL =
        new MySQLContainer(DockerImageName.parse(ContainerImages.MYSQL_IMAGE));
    private static volatile DataSource sharedDataSource;

    /**
     * 启动共享的真实 MySQL 容器、建立连接池并重建测试表。
     */
    @BeforeAll
    static void startSharedMysql() {
        if (sharedDataSource != null) {
            executeScript(DDL);
            return;
        }
        MYSQL.start();
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(MYSQL.getJdbcUrl());
        config.setUsername(MYSQL.getUsername());
        config.setPassword(MYSQL.getPassword());
        config.setMaximumPoolSize(4);
        HikariDataSource hikari = new HikariDataSource(config);
        sharedDataSource = hikari;
        executeScript(DDL);
    }

    /**
     * 物理清空测试表，保证用例间隔离。
     */
    @BeforeEach
    void resetTable() {
        jdbc().update("DELETE FROM mp_it_user");
    }

    /**
     * 构建装配真实数据源与自动配置的上下文运行器。
     *
     * <p>模块自动配置开启扩展、分页与防全表更新插件，与官方 MyBatis Plus 自动配置同容器装配，
     * 复现下游应用的真实装配链路。官方自动配置类与模块自动配置类同名，模块类经 import 引入，
     * 官方类保留内联限定名（该规则仅约束项目内类名）。</p>
     *
     * @return 上下文运行器
     */
    ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withPropertyValues("mybatis-plus.extension.enabled=true",
                "mybatis-plus.extension.pagination.enabled=true",
                "mybatis-plus.extension.pagination.db-type=mysql")
            .withConfiguration(AutoConfigurations
                .of(com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration.class,
                    MybatisPlusAutoConfiguration.class))
            .withUserConfiguration(ItConfig.class)
            .withBean(SpringUtil.class);
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
     * 获取共享数据源上的 JDBC 模板，用于绕过被测模块直接核对真实表数据。
     *
     * @return JDBC 模板
     */
    static JdbcTemplate jdbc() {
        return new JdbcTemplate(sharedDataSource);
    }

    /**
     * 在共享容器上执行多语句建表脚本。
     *
     * @param script 建表 SQL 脚本（分号分隔）
     */
    static void executeScript(String script) {
        try (Connection connection = sharedDataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                new EncodedResource(new ByteArrayResource(script
                    .getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("执行建表脚本失败", e);
        }
    }

    /**
     * 按主键读取一行真实表数据。
     *
     * @param id 主键值
     * @return 数据库原始行（列名为键）
     */
    static Map<String, Object> rawRow(Long id) {
        return jdbc().queryForMap("SELECT * FROM mp_it_user WHERE id = ?", id);
    }

    /**
     * 统计测试表真实行数。
     *
     * @return 行数
     */
    static Long rawCount() {
        return jdbc().queryForObject("SELECT COUNT(*) FROM mp_it_user", Long.class);
    }

    /**
     * 测试装配配置：真实数据源、事务管理与被测业务实现。
     *
     * <p>DataSource 为 JVM 级共享实例，禁止推断销毁方法。</p>
     *
     * @author Charles7c
     * @since 3.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @MapperScan("top.continew.starter.data.mp.it.mapper")
    static class ItConfig {

        @Bean(destroyMethod = "")
        DataSource dataSource() {
            return MySqlSupport.dataSource();
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        ItUserService itUserService() {
            return new ItUserService();
        }

        @Bean
        Validator validator() {
            // core 校验器在静态初始化期经 SpringUtil 取 JSR 303 校验器，测试上下文无 hibernate-validator，
            // 以 mock Bean 满足该依赖（与 core 模块 CheckUtilsTest 同一约定），用例路径不触发真实校验
            return mock(Validator.class);
        }
    }
}
