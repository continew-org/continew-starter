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

package top.continew.starter.security.crypto;

import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import top.continew.starter.security.crypto.autoconfigure.CryptoProperties;
import top.continew.starter.security.crypto.core.MyBatisDecryptInterceptor;
import top.continew.starter.security.crypto.core.MyBatisEncryptInterceptor;
import top.continew.starter.security.crypto.domain.TestUser;
import top.continew.starter.security.crypto.mapper.TestUserMapper;

import javax.sql.DataSource;
import java.io.*;
import java.sql.Connection;

/*
 * *
 * 
 * @author huangxiong
 * 
 * @date 2024/06/06 22:42
 */
public class MybatisEncryptTest {

    private static SqlSessionFactory sqlSessionFactory;
    private static SqlSession sqlSession;

    @Before
    public void setUp() throws Exception {
        Reader reader = Resources.getResourceAsReader("mybatis-config.xml");
        sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(reader);
        Configuration configuration = sqlSessionFactory.getConfiguration();
        DataSource dataSource = configuration.getEnvironment().getDataSource();
        Connection connection = dataSource.getConnection();
        ScriptRunner scriptRunner = new ScriptRunner(connection);
        scriptRunner.runScript(Resources.getResourceAsReader("db/schema.sql"));
        //        scriptRunner.runScript(Resources.getResourceAsReader("db/data.sql"));
        System.out.println("===================== init db =======================");

        CryptoProperties cryptoProperties = buildCryptoProperties();
        configuration.addInterceptor(new MyBatisEncryptInterceptor(cryptoProperties));
        configuration.addInterceptor(new MyBatisDecryptInterceptor(cryptoProperties));

        sqlSession = sqlSessionFactory.openSession(true);
    }

    @After
    public void closeDown() {
        sqlSession.close();
    }

    @Test
    public void getById() {
        TestUser testUser = insertTestUser(18, "cary", "john.doe@example.com");
        System.out.println(testUser);
        TestUserMapper mapper = sqlSession.getMapper(TestUserMapper.class);
        TestUser testUser1 = mapper.selectById(testUser.getId());
        System.out.println(testUser1);
    }

    @Test
    public void mpUpdate() {
        TestUser testUser = insertTestUser(18, "cary", "john.doe@example.com");
        TestUserMapper mapper = sqlSession.getMapper(TestUserMapper.class);
        LambdaUpdateWrapper<TestUser> updateWrapper = new UpdateWrapper<TestUser>().lambda()
            .set(TestUser::getEmail, "666@22.com")
            .set(TestUser::getAge, 19);
        mapper.update(updateWrapper);

        String rawEmail = mapper.getRawEmail(testUser.getId());
        System.out.println(rawEmail);
    }

    /**
     * insert testUser
     * 
     * @return data
     */
    private TestUser insertTestUser(Integer age, String name, String email) {
        TestUserMapper mapper = sqlSession.getMapper(TestUserMapper.class);
        TestUser testUser = new TestUser();
        testUser.setAge(age);
        testUser.setName(name);
        testUser.setEmail(email);
        mapper.insert(testUser);
        Assert.notNull(testUser.getId());
        return testUser;
    }

    /**
     * 构建加密配置
     */
    private CryptoProperties buildCryptoProperties() throws Exception {
        CryptoProperties cryptoProperties = new CryptoProperties();
        cryptoProperties.setEnabled(true);
        cryptoProperties.setPassword("abcdefghijklmnop");
        cryptoProperties
            .setPrivateKey("MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAM51dgYtMyF+tTQt80sfFOpSV27a7t9uaUVeFrdGiVxscuizE7H8SMntYqfn9lp8a5GH5P1/GGehVjUD2gF/4kcCAwEAAQ==");
        cryptoProperties
            .setPublicKey("MIIBVQIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAznV2Bi0zIX61NC3zSx8U6lJXbtru325pRV4Wt0aJXGxy6LMTsfxIye1ip+f2WnxrkYfk/X8YZ6FWNQPaAX/iRwIDAQABAkEAk/VcAusrpIqA5Ac2P5Tj0VX3cOuXmyouaVcXonr7f+6y2YTjLQuAnkcfKKocQI/juIRQBFQIqqW/m1nmz1wGeQIhAO8XaA/KxzOIgU0l/4lm0A2Wne6RokJ9HLs1YpOzIUmVAiEA3Q9DQrpAlIuiT1yWAGSxA9RxcjUM/1kdVLTkv0avXWsCIE0X8woEjK7lOSwzMG6RpEx9YHdopjViOj1zPVH61KTxAiBmv/dlhqkJ4rV46fIXELZur0pj6WC3N7a4brR8a+CLLQIhAMQyerWl2cPNVtE/8tkziHKbwW3ZUiBXU24wFxedT9iV");
        return cryptoProperties;
    }
}
