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

package top.continew.starter.security.crypto.core;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.type.SimpleTypeRegistry;
import top.continew.starter.security.crypto.annotation.FieldEncrypt;
import top.continew.starter.security.crypto.autoconfigure.CryptoProperties;
import top.continew.starter.security.crypto.encryptor.IEncryptor;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;

/**
 * 字段解密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets", args = {Statement.class})})
public class MyBatisDecryptInterceptor extends AbstractMyBatisInterceptor implements Interceptor {

    private CryptoProperties properties;

    public MyBatisDecryptInterceptor(CryptoProperties properties) {
        this.properties = properties;
    }

    public MyBatisDecryptInterceptor() {
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object obj = invocation.proceed();
        if (null == obj || !(invocation.getTarget() instanceof ResultSetHandler)) {
            return obj;
        }
        List<?> resultList = (List<?>)obj;
        for (Object result : resultList) {
            // String、Integer、Long 等简单类型对象无需处理
            if (SimpleTypeRegistry.isSimpleType(result.getClass())) {
                continue;
            }
            // 获取所有字符串类型、需要解密的、有值字段
            List<Field> fieldList = super.getEncryptFields(result);
            // 解密处理
            for (Field field : fieldList) {
                IEncryptor encryptor = super.getEncryptor(field.getAnnotation(FieldEncrypt.class));
                Object fieldValue = ReflectUtil.getFieldValue(result, field);
                if (null == fieldValue) {
                    continue;
                }
                // 优先获取自定义对称加密算法密钥，获取不到时再获取全局配置
                String password = ObjectUtil.defaultIfBlank(field.getAnnotation(FieldEncrypt.class)
                    .password(), properties.getPassword());
                String ciphertext = encryptor.decrypt(fieldValue.toString(), password, properties.getPrivateKey());
                ReflectUtil.setFieldValue(result, field, ciphertext);
            }
        }
        return resultList;
    }
}