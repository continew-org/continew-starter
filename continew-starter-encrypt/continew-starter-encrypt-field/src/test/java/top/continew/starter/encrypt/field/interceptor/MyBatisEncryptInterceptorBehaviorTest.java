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
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.binding.BindingException;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import java.util.function.BiConsumer;

/**
 * 字段加密拦截器行为测试
 *
 * <p>用 Mockito 模拟 Executor、以真实 MappedStatement 与 Invocation 驱动拦截器，验证写入参数
 * 的各类形态（实体、et/ew、集合、数组、自定义别名）均被加密、加密只作用于落库参数拷贝而
 * 传入实体保持明文原值、同一实体重复写入幂等、加密失败时参数不被污染。</p>
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

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private final MyBatisEncryptInterceptor interceptor = new MyBatisEncryptInterceptor();

    @AfterEach
    void resetDefaultProperties() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword(VALID_PASSWORD);
        EncryptHelper.init(properties);
    }

    /**
     * et 实体参数应被加密（落库参数为密文），传入实体保持明文原值。
     */
    @Test
    void shouldEncryptEtEntityAndKeepOriginalEntityPlain() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("et", user);
        Executor executor = this.mockWriteExecutor((param, captured) -> this
            .collectPhoneValues(param, captured));
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(user.phone).isEqualTo(PHONE);
    }

    /**
     * 以真实 MapperMethod.ParamMap 为参数时，替换后的落库参数应保持 ParamMap 类型与
     * 缺键抛 BindingException 的拼写检查语义，实体照常加密。
     */
    @Test
    void shouldPreserveParamMapSemantics() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        MapperMethod.ParamMap<Object> parameter = new MapperMethod.ParamMap<>();
        parameter.put("et", user);
        List<Object> capturedParams = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            Map<?, ?> param = invocation.getArgument(1);
            capturedParams.add(param);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(capturedParams).hasSize(1);
        Object newParameter = capturedParams.get(0);
        assertThat(newParameter).isInstanceOf(MapperMethod.ParamMap.class);
        assertThatThrownBy(() -> ((Map<?, ?>) newParameter).get("wrongKey"))
            .isInstanceOf(BindingException.class);
        assertThat(user.phone).isEqualTo(PHONE);
    }

    /**
     * 自定义别名实体、集合与数组中的实体均应被加密，传入实体保持明文。
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
            this.collectPhoneValues(invocation.getArgument(1), captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
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
            this.collectPhoneValues(invocation.getArgument(1), captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        assertThat(capturedPerExecute).hasSize(2);
        assertThat(capturedPerExecute.get(0).get(0)).isEqualTo(capturedPerExecute.get(1).get(0));
    }

    /**
     * 同一实体在多个参数别名（et、param1、自定义别名）下同时出现时应只加密一次，
     * 落库参数中各别名指向同一加密拷贝。
     */
    @Test
    void shouldNotDoubleEncryptSameEntityInOneWrite() throws Throwable {
        User user = new User();
        user.phone = PHONE;
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("et", user);
        parameter.put("param1", user);
        parameter.put("alias", user);
        List<Map<?, ?>> capturedParams = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            capturedParams.add((Map<?, ?>) invocation.getArgument(1));
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.UPDATE), parameter);
        Map<?, ?> newParameter = capturedParams.get(0);
        Object etCopy = newParameter.get("et");
        assertThat(newParameter.get("param1")).isSameAs(etCopy);
        assertThat(newParameter.get("alias")).isSameAs(etCopy);
        assertThat(etCopy).isNotSameAs(user);
        String onceEncrypted = EncryptHelper.encrypt(PHONE, userAnnotation(User.class));
        assertThat(ReflectUtil.getFieldValue(etCopy, "phone")).isEqualTo(onceEncrypted);
    }

    /**
     * UpdateWrapper 的 SET 参数应被加密，空串保持空串不被替换为 null，
     * 写入完成后 paramNameValuePairs 恢复为明文。
     */
    @Test
    void shouldEncryptUpdateWrapperPairsAndKeepEmptyString() throws Throwable {
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.set("phone", PHONE);
        wrapper.set("name", "");
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
        assertThat(captured).hasSize(2);
        assertThat(captured).contains("");
        assertThat(captured).anyMatch(value -> !PHONE.equals(value) && !value.isEmpty());
        assertThat(wrapper.getParamNameValuePairs().containsValue(PHONE)).isTrue();
        assertThat(wrapper.getParamNameValuePairs().containsValue("")).isTrue();
    }

    /**
     * setSql 的原样 SQL 片段（含逗号、无占位符）不应影响 SET 参数的加密解析，
     * 命中加密列的占位符参数照常加密。
     */
    @Test
    void shouldIgnoreSetSqlFragmentWithoutPlaceholder() throws Throwable {
        this.initUserTableInfo();
        UpdateWrapper<User> wrapper = new UpdateWrapper<>();
        wrapper.set("phone", PHONE);
        wrapper.setSql("name = 'a,b'");
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
     * 直传实体（MP insert 等无别名方法）应被加密，传入实体保持明文。
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
     * 直传集合（批量插入）应整集合加密，传入集合中的实体保持明文。
     */
    @Test
    void shouldEncryptInsertCollectionParameter() throws Throwable {
        User first = new User();
        first.phone = PHONE;
        User second = new User();
        second.phone = PHONE;
        List<User> users = new ArrayList<>(Arrays.asList(first, second));
        List<List<String>> capturedPerExecute = new ArrayList<>();
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            this.collectPhoneValues(invocation.getArgument(1), captured);
            capturedPerExecute.add(captured);
            return 1;
        });
        this.interceptUpdate(executor, updateStatement(SqlCommandType.INSERT), users);
        assertThat(capturedPerExecute.get(0)).hasSize(2);
        assertThat(capturedPerExecute.get(0)).allMatch(value -> !PHONE.equals(value));
        assertThat(first.phone).isEqualTo(PHONE);
        assertThat(second.phone).isEqualTo(PHONE);
    }

    /**
     * fail-fast 模式下加密失败应抛出异常，且传入实体不被密文污染。
     */
    @Test
    void shouldNotPolluteEntityWhenEncryptFails() throws Throwable {
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
     * 查询路径的实体参数应被加密（保持既有行为），传入实体保持明文。
     */
    @Test
    void shouldEncryptQueryEntityParameterAndKeepOriginalPlain() throws Throwable {
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
     * QueryWrapper 的条件值应在查询期间被加密，查询完成后恢复为明文。
     */
    @Test
    void shouldEncryptQueryWrapperConditionValueAndRestore() throws Throwable {
        QueryWrapper<User> wrapper = new QueryWrapper<User>().eq("phone", PHONE);
        List<String> captured = new ArrayList<>();
        Executor executor = this.mockQueryExecutor(wrapper, captured);
        this.interceptSelect(executor, wrapper);
        assertThat(captured).allMatch(value -> !PHONE.equals(value));
        assertThat(wrapper.getParamNameValuePairs().containsValue(PHONE)).isTrue();
    }

    /**
     * LambdaQueryWrapper 的条件值应同样被加密并在查询完成后恢复为明文。
     */
    @Test
    void shouldEncryptLambdaQueryWrapperConditionValueAndRestore() throws Throwable {
        this.initUserTableInfo();
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<User>().eq(User::getPhone, PHONE);
        List<String> captured = new ArrayList<>();
        Executor executor = this.mockQueryExecutor(wrapper, captured);
        this.interceptSelect(executor, wrapper);
        assertThat(captured).allMatch(value -> !PHONE.equals(value));
        assertThat(wrapper.getParamNameValuePairs().containsValue(PHONE)).isTrue();
    }

    /**
     * 模拟查询型 Executor：执行期间记录 Wrapper 条件参数的当前值
     *
     * @param wrapper  Wrapper 参数
     * @param captured 捕获结果
     * @return 已打桩的 Executor
     * @throws SQLException Executor.query 声明的受检异常
     */
    private Executor mockQueryExecutor(AbstractWrapper<?, ?, ?> wrapper, List<String> captured)
        throws SQLException {
        Executor executor = mock(Executor.class);
        when(executor.query(any(MappedStatement.class), any(), any(RowBounds.class),
            any(ResultHandler.class))).thenAnswer(invocation -> {
                wrapper.getParamNameValuePairs().forEach((key, value) -> {
                    if (value instanceof String strValue) {
                        captured.add(strValue);
                    }
                });
                return new ArrayList<>();
            });
        return executor;
    }

    /**
     * 通过 Invocation 触发拦截器查询路径
     *
     * @param executor 模拟的执行器
     * @param wrapper  Wrapper 参数
     * @throws Throwable 拦截异常
     */
    private void interceptSelect(Executor executor, AbstractWrapper<?, ?, ?> wrapper)
        throws Throwable {
        Method queryMethod = Executor.class.getMethod("query", MappedStatement.class, Object.class,
            RowBounds.class, ResultHandler.class);
        Map<String, Object> parameter = new HashMap<>();
        parameter.put("ew", wrapper);
        Invocation invocation = new Invocation(executor, queryMethod,
            new Object[] {updateStatement(SqlCommandType.SELECT), parameter, RowBounds.DEFAULT,
                mock(ResultHandler.class)});
        this.interceptor.intercept(invocation);
    }

    /**
     * 注册样例实体的 MyBatis-Plus TableInfo（Lambda 列名解析与全局加密列构建依赖）
     */
    private void initUserTableInfo() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            User.class);
    }

    /**
     * 模拟写入型 Executor：执行期间按给定捕获器记录替换后的落库参数
     *
     * @param capturer 捕获器，入参为替换后的参数对象与捕获结果
     * @return 已打桩的 Executor
     * @throws SQLException Executor.update 声明的受检异常
     */
    private Executor mockWriteExecutor(BiConsumer<Object, List<String>> capturer)
        throws SQLException {
        Executor executor = mock(Executor.class);
        when(executor.update(any(MappedStatement.class), any())).thenAnswer(invocation -> {
            List<String> captured = new ArrayList<>();
            capturer.accept(invocation.getArgument(1), captured);
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
