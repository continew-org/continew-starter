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

package top.continew.starter.encrypt.field.interceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMap;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.scripting.defaults.RawSqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.autoconfigure.FieldEncryptProperties;
import top.continew.starter.encrypt.field.exception.FieldEncryptException;
import top.continew.starter.encrypt.field.util.EncryptHelper;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * 字段加密拦截器行为测试
 *
 * <p>用 Mockito 模拟 Executor、以真实 MappedStatement 与 Invocation 驱动拦截器，验证写入参数
 * 的各类形态（实体、et/ew、集合、数组、自定义别名）均被加密、写入后参数恢复原值、同一实体
 * 重复写入幂等、加密失败时参数不被污染。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MyBatisEncryptInterceptorBehaviorTest {

    private static final String PHONE = "13800138000";

    private static final String VALID_PASSWORD = "abcdefghijklmnop";

    /**
     * 携带注解级非法密钥的样例实体
     */
    static class InvalidKeyEntity {

        @FieldEncrypt(password = "12345")
        private String phone;
    }

    /**
     * 使用全局默认配置的样例实体
     */
    static class User {

        @FieldEncrypt
        private String phone;

        private String name;
    }

    private final MyBatisEncryptInterceptor interceptor = new MyBatisEncryptInterceptor();

    @AfterEach
    void resetDefaultProperties() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword(VALID_PASSWORD);
        EncryptHelper.init(properties);
    }

    /**
     * et 实体参数应在写入期间被加密，写入完成后恢复为明文原值。
     */
    @Test
    void shouldEncryptEtEntityAndRestoreAfterWrite() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("et", user);
        Executor executor = this.mockWriteExecutor(captured -> this.collectPhoneValues(parameter,
            captured));
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(user.phone).isEqualTo(PHONE);
    }

    /**
     * 自定义别名实体、集合与数组中的实体均应被加密（写入期间均为密文），写入完成后恢复为明文。
     */
    @Test
    void shouldEncryptCustomAliasCollectionAndArrayEntities() throws Throwable {
        User aliased = new User();
        aliased.phone = PHONE;
        User first = new User();
        first.phone = PHONE;
        User second = new User();
        second.phone = PHONE;
        User third = new User();
        third.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("user", aliased);
        parameter.put("list", Arrays.asList(first, second));
        parameter.put("array", new User[] {third});
        List<List<String>> capturedPerExecute = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            this.collectPhoneValues(parameter, captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(capturedPerExecute).hasSize(1);
        assertThat(capturedPerExecute.get(0)).hasSize(4);
        assertThat(capturedPerExecute.get(0)).allMatch(value -> !PHONE.equals(value));
        assertThat(aliased.phone).isEqualTo(PHONE);
        assertThat(first.phone).isEqualTo(PHONE);
        assertThat(second.phone).isEqualTo(PHONE);
        assertThat(third.phone).isEqualTo(PHONE);
    }

    /**
     * 同一实体连续写入两次，两次落库值应一致（幂等，不二次加密）。
     */
    @Test
    void shouldBeIdempotentForRepeatedWrite() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("et", user);
        List<List<String>> capturedPerExecute = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            this.collectPhoneValues(parameter, captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(capturedPerExecute).hasSize(2);
        assertThat(capturedPerExecute.get(0).get(0)).isEqualTo(capturedPerExecute.get(1).get(0));
    }

    /**
     * 同一实体在多个参数别名（et、param1、自定义别名）下同时出现时应只加密一次。
     */
    @Test
    void shouldNotDoubleEncryptSameEntityInOneWrite() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("et", user);
        parameter.put("param1", user);
        parameter.put("alias", user);
        List<List<String>> capturedPerExecute = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            this.collectPhoneValues(parameter, captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(capturedPerExecute.get(0)).hasSize(3);
        String onceEncrypted = EncryptHelper.encrypt(PHONE, userAnnotation(User.class));
        assertThat(capturedPerExecute.get(0)).allMatch(onceEncrypted::equals);
    }

    /**
     * UpdateWrapper 的 SET 参数应在写入期间被加密，写入完成后恢复为明文。
     */
    @Test
    void shouldEncryptUpdateWrapperPairsAndRestore() throws Throwable {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.set("phone", PHONE);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("ew", wrapper);
        List<String> captured = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            Map<?, ?> param = invocation.getArgument(1);
            UpdateWrapper<?> wrapperParam = (UpdateWrapper<?>) param.get("ew");
            wrapperParam.getParamNameValuePairs().forEach((key, value) -> {
                if (value instanceof String strValue) {
                    captured.add(strValue);
                }
            });
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isNotEqualTo(PHONE);
        assertThat(wrapper.getParamNameValuePairs().containsValue(PHONE)).isTrue();
    }

    /**
     * 直传实体（MP insert 等无别名方法）应被加密，写入完成后恢复为明文。
     */
    @Test
    void shouldEncryptInsertEntityParameter() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        List<List<String>> capturedPerExecute = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            this.collectPhoneValues(invocation.getArgument(1), captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.INSERT), user);
        assertThat(capturedPerExecute.get(0)).hasSize(1);
        assertThat(capturedPerExecute.get(0).get(0)).isNotEqualTo(PHONE);
        assertThat(user.phone).isEqualTo(PHONE);
    }

    /**
     * fail-fast 模式下加密失败应抛出异常，且实体字段不被密文污染。
     */
    @Test
    void shouldRestoreEntityWhenEncryptFails() throws Throwable {
        InvalidKeyEntity entity = new InvalidKeyEntity();
        entity.phone = PHONE;
        Executor executor = mock(Executor.class);
        assertThatThrownBy(
            () -> this.interceptUpdate(executor, updateStatement(SqlCommandType.INSERT),
                entity))
            .isInstanceOf(FieldEncryptException.class);
        assertThat(entity.phone).isEqualTo(PHONE);
    }

    /**
     * 查询路径的实体参数应被加密（保持既有行为），查询完成后恢复为明文。
     */
    @Test
    void shouldEncryptQueryEntityParameterAndRestore() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("user", user);
        List<String> captured = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.query(any(MappedStatement.class), any(), any(RowBounds.class),
            any(ResultHandler.class))).thenAnswer(invocation -> {
                this.collectPhoneValues(invocation.getArgument(1), captured);
                return new ArrayList<>();
            });
        Method queryMethod = Executor.class.getMethod("query", MappedStatement.class, Object.class,
            RowBounds.class, ResultHandler.class);
        Invocation invocation = new Invocation(executor, queryMethod,
            new Object[] {updateStatement(SqlCommandType.SELECT), parameter, RowBounds.DEFAULT,
                mock(ResultHandler.class)});
        this.interceptor.intercept(invocation);
        assertThat(captured).hasSize(1);
        assertThat(captured.get(0)).isNotEqualTo(PHONE);
        assertThat(user.phone).isEqualTo(PHONE);
    }

    /**
     * 模拟写入型 Executor：执行期间按给定捕获器记录参数快照
     *
     * @param capturer 捕获器，执行期间被调用以记录当时的参数值
     * @return 已打桩的 Executor
     * @throws SQLException Executor.update 声明的受检异常
     */
    private Executor mockWriteExecutor(Consumer<List<String>> capturer) throws SQLException {
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            capturer.accept(captured);
            return 1;
        });
        return executor;
    }

    /**
     * 收集参数中所有样例实体的加密字段值：Map 值、集合与数组元素均展开
     *
     * @param param    参数（Map、集合、数组或实体）
     * @param captured 捕获结果
     */
    private void collectPhoneValues(Object param, List<String> captured) {
        if (param instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                this.collectPhoneValues(value, captured);
            }
            return;
        }
        if (param instanceof List<?> list) {
            for (Object element : list) {
                this.collectPhoneValues(element, captured);
            }
            return;
        }
        if (param instanceof User[] array) {
            for (User user : array) {
                captured.add(user.phone);
            }
            return;
        }
        if (param instanceof User user) {
            captured.add(user.phone);
        }
    }

    /**
     * 通过 Invocation 触发拦截器写入路径
     *
     * @param executor  模拟的执行器
     * @param statement 映射语句
     * @param parameter 写入参数
     * @throws Throwable 拦截异常
     */
    private void interceptUpdate(Executor executor, MappedStatement statement,
        Object parameter) throws Throwable {
        Method updateMethod =
            Executor.class.getMethod("update", MappedStatement.class, Object.class);
        Invocation invocation = new Invocation(executor, updateMethod, new Object[] {statement,
            parameter});
        this.interceptor.intercept(invocation);
    }

    /**
     * 构造指定类型的映射语句，参数映射类型指向 User 以支持 Wrapper 场景取实体类
     *
     * @param commandType 命令类型
     * @return 映射语句
     */
    private MappedStatement updateStatement(SqlCommandType commandType) {
        Configuration configuration = new Configuration();
        RawSqlSource sqlSource = new RawSqlSource(configuration, "UPDATE user SET phone = ?", null);
        // 语句 ID 采用「存在的类名.方法后缀」形式，避免查询路径解析参数级注解时类加载失败
        String statementId = MyBatisEncryptInterceptorBehaviorTest.class.getName() + ".updateUser";
        return new MappedStatement.Builder(configuration, statementId, sqlSource, commandType)
            .parameterMap(new ParameterMap.Builder(configuration, "default", User.class,
                new ArrayList<>()).build())
            .build();
    }

    /**
     * 获取样例实体加密字段上的注解
     *
     * @param entityClass 实体类
     * @return 字段加密注解
     */
    private FieldEncrypt userAnnotation(Class<?> entityClass) {
        return ReflectUtil.getField(entityClass, "phone").getAnnotation(FieldEncrypt.class);
    }
}
