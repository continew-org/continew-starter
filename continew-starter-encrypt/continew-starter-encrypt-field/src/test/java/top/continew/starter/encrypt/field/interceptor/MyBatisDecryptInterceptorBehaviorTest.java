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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Invocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.autoconfigure.FieldEncryptProperties;
import top.continew.starter.encrypt.field.util.EncryptHelper;

import java.lang.reflect.Method;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 字段解密拦截器行为测试
 *
 * <p>用 Mockito 模拟 ResultSetHandler 驱动解密拦截器，验证 resultType=map 的结果行按实体
 * 声明的加密列按键解密，且实体结果的解密路径不受影响。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MyBatisDecryptInterceptorBehaviorTest {

    private static final String PHONE = "13800138000";

    private static final String VALID_PASSWORD = "abcdefghijklmnop";

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

    private final MyBatisDecryptInterceptor interceptor = new MyBatisDecryptInterceptor();

    @BeforeEach
    void initDefaultPropertiesAndTableInfo() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword(VALID_PASSWORD);
        EncryptHelper.init(properties);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
            User.class);
    }

    @AfterEach
    void resetDefaultPropertiesAndTableInfo() {
        FieldEncryptProperties properties = new FieldEncryptProperties();
        properties.setPassword(VALID_PASSWORD);
        EncryptHelper.init(properties);
        TableInfoHelper.remove(User.class);
    }

    /**
     * resultType=map 的结果行应按实体声明的加密列按键解密，其余列保持原值。
     */
    @Test
    void shouldDecryptMapRowByDeclaredColumns() throws Throwable {
        String ciphertext = EncryptHelper.encrypt(PHONE, userAnnotation(User.class));
        Map<String, Object> row = new HashMap<>();
        row.put("phone", ciphertext);
        row.put("name", "张三");
        List<Object> rows = new ArrayList<>();
        rows.add(row);
        this.interceptResultSets(rows);
        assertThat(row.get("phone")).isEqualTo(PHONE);
        assertThat(row.get("name")).isEqualTo("张三");
    }

    /**
     * 实体结果行的解密路径应保持原有行为不受 map 行解密改动影响。
     */
    @Test
    void shouldDecryptEntityResultUnaffected() throws Throwable {
        User user = new User();
        user.setPhone(EncryptHelper.encrypt(PHONE, userAnnotation(User.class)));
        user.setName("李四");
        List<Object> rows = new ArrayList<>();
        rows.add(user);
        this.interceptResultSets(rows);
        assertThat(user.getPhone()).isEqualTo(PHONE);
        assertThat(user.getName()).isEqualTo("李四");
    }

    /**
     * 通过 Invocation 触发解密拦截器处理结果集
     *
     * @param rows 结果集
     * @throws Throwable 拦截异常
     */
    private void interceptResultSets(List<Object> rows) throws Throwable {
        ResultSetHandler handler = mock(ResultSetHandler.class);
        when(handler.handleResultSets(any(Statement.class))).thenAnswer(invocation -> rows);
        Method method = ResultSetHandler.class.getMethod("handleResultSets", Statement.class);
        Invocation invocation = new Invocation(handler, method,
            new Object[] {mock(Statement.class)});
        this.interceptor.intercept(invocation);
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
