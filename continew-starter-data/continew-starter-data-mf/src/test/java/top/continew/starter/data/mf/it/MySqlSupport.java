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

package top.continew.starter.data.mf.it;

import static org.mockito.Mockito.mock;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.logicdelete.impl.IntegerLogicDeleteProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.junit.jupiter.EnabledIfDockerAvailable;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;
import top.continew.starter.data.autoconfigure.MybatisFlexAutoConfiguration;
import top.continew.starter.data.datapermission.DataPermissionAspect;
import top.continew.starter.data.datapermission.DataPermissionCurrentUser;
import top.continew.starter.data.datapermission.DataPermissionFilter;
import top.continew.starter.data.mf.it.service.ItUserService;
import top.continew.starter.test.support.ContainerImages;

import jakarta.validation.Validator;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.Map;

/**
 * MyBatis Flex 集成测试基座：整个 Failsafe JVM 共享一个真实 MySQL 容器与数据源。
 *
 * <p>Flex 的全局配置、方言注册表与逻辑删除处理器均为 JVM 级静态状态，容器与数据源必须
 * 跨测试类共享，容器由 JVM 退出时的 Ryuk 守护进程统一回收。模块自动配置要求容器提供
 * {@link DataPermissionFilter}（必选扩展点）与 {@link DataPermissionAspect}（AOP 触发
 * {@code @DataPermission} 注解），基座以共享实例提供，测试用例经静态方法切换权限态。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@EnabledIfDockerAvailable
abstract class MySqlSupport {

    /**
     * 共享测试表：用户表（逻辑删除）、部门表（本部门及以下子查询）、角色部门关联表（自定义子查询）
     */
    private static final String DDL = """
        DROP TABLE IF EXISTS mf_it_user;
        CREATE TABLE mf_it_user (
          id BIGINT NOT NULL,
          username VARCHAR(64) NOT NULL,
          nickname VARCHAR(64) DEFAULT NULL,
          dept_id BIGINT DEFAULT NULL,
          create_user BIGINT DEFAULT NULL,
          deleted INT NOT NULL DEFAULT 0,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        DROP TABLE IF EXISTS mf_it_dept;
        CREATE TABLE mf_it_dept (
          id BIGINT NOT NULL,
          ancestors VARCHAR(255) DEFAULT NULL,
          PRIMARY KEY (id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        DROP TABLE IF EXISTS mf_it_role_dept;
        CREATE TABLE mf_it_role_dept (
          role_id VARCHAR(64) NOT NULL,
          dept_id BIGINT NOT NULL,
          PRIMARY KEY (role_id, dept_id)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
        """;

    private static final MySQLContainer MYSQL =
        new MySQLContainer(DockerImageName.parse(ContainerImages.MYSQL_IMAGE));
    private static volatile DriverManagerDataSource sharedDataSource;
    private static volatile boolean filterEnabled;
    private static volatile DataPermissionCurrentUser currentUser;

    /**
     * 启动共享的真实 MySQL 容器、重建测试表，并固化 JVM 级 Flex 静态配置。
     *
     * <p>逻辑删除处理器取整数语义（0 未删 / 1 已删）；数据库类型显式指定为 MYSQL，使
     * 方言注册表命中模块自动配置注册的 {@code DataPermissionDialect}。</p>
     */
    @BeforeAll
    static void startSharedMysql() {
        LogicDeleteManager.setProcessor(new IntegerLogicDeleteProcessor());
        if (sharedDataSource != null) {
            executeScript(DDL);
            return;
        }
        MYSQL.start();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(MYSQL.getJdbcUrl(), MYSQL
            .getUsername(), MYSQL.getPassword());
        sharedDataSource = dataSource;
        executeScript(DDL);
    }

    /**
     * 物理清空测试表并复位数据权限过滤状态，保证用例间隔离。
     */
    @BeforeEach
    void resetState() {
        jdbc().update("DELETE FROM mf_it_user");
        jdbc().update("DELETE FROM mf_it_dept");
        jdbc().update("DELETE FROM mf_it_role_dept");
        applyDataPermission(false, null);
    }

    /**
     * 构建装配真实数据源与自动配置的上下文运行器。
     *
     * <p>模块自动配置与官方 MyBatis Flex 自动配置同容器装配，复现下游应用的真实装配链路。
     * 官方 Boot4 自动配置类与模块自动配置类同名，模块类经 import 引入，官方类保留内联限定名
     * （该规则仅约束项目内类名）。</p>
     *
     * @return 上下文运行器
     */
    ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
            .withPropertyValues("mybatis-flex.extension.enabled=true")
            .withConfiguration(AutoConfigurations.of(
                com.mybatisflex.spring.boot.v4.MybatisFlexAutoConfiguration.class,
                MybatisFlexAutoConfiguration.class))
            .withUserConfiguration(ItConfig.class)
            .withBean(cn.hutool.extra.spring.SpringUtil.class);
    }

    /**
     * 设置当前用例的数据权限过滤状态。
     *
     * @param enabled 是否启用数据权限过滤
     * @param user    当前用户（含角色与数据权限范围）
     */
    static void applyDataPermission(boolean enabled, DataPermissionCurrentUser user) {
        filterEnabled = enabled;
        currentUser = user;
    }

    /**
     * 显式指定 Flex 数据库类型为 MYSQL，使方言解析命中模块自动配置注册的
     * {@code DataPermissionDialect}。
     *
     * <p>须在上下文启动后的查询前调用：会话工厂构建时会重建全局配置，启动前设置会被覆盖。</p>
     */
    static void useMysqlDialect() {
        FlexGlobalConfig.getDefaultConfig().setDbType(DbType.MYSQL);
    }

    /**
     * 获取共享的真实数据源。
     *
     * @return 与共享容器连接的数据源
     */
    static DriverManagerDataSource dataSource() {
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
     * 按主键读取用户表一行真实数据。
     *
     * @param id 主键值
     * @return 数据库原始行（列名为键）
     */
    static Map<String, Object> rawUserRow(Long id) {
        return jdbc().queryForMap("SELECT * FROM mf_it_user WHERE id = ?", id);
    }

    /**
     * 统计用户表真实行数。
     *
     * @return 行数
     */
    static Long rawUserCount() {
        return jdbc().queryForObject("SELECT COUNT(*) FROM mf_it_user", Long.class);
    }

    /**
     * 测试装配配置：真实数据源、数据权限扩展点、AOP 切面与被测业务实现。
     *
     * <p>DataSource 为 JVM 级共享实例，禁止推断销毁方法；DataPermissionFilter 为同一共享
     * 实例，权限态由用例经基座静态方法切换。</p>
     *
     * @author Charles7c
     * @since 3.1.0
     */
    @Configuration(proxyBeanMethods = false)
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @MapperScan(basePackages = "top.continew.starter.data.mf.it.mapper",
        sqlSessionFactoryRef = "sqlSessionFactory")
    static class ItConfig {

        @Bean(destroyMethod = "")
        DriverManagerDataSource dataSource() {
            return MySqlSupport.dataSource();
        }

        @Bean
        DataPermissionFilter dataPermissionFilter() {
            return new ItDataPermissionFilter();
        }

        @Bean
        DataPermissionAspect dataPermissionAspect() {
            return new DataPermissionAspect();
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

    /**
     * 数据权限过滤扩展点实现：启停与当前用户由基座静态状态控制
     *
     * @author Charles7c
     * @since 3.1.0
     */
    static final class ItDataPermissionFilter implements DataPermissionFilter {

        @Override
        public boolean isFilter() {
            return filterEnabled;
        }

        @Override
        public DataPermissionCurrentUser getCurrentUser() {
            return currentUser;
        }
    }
}
