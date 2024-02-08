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

package top.charles7c.continew.starter.security.crypto.core;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.*;
import top.charles7c.continew.starter.security.crypto.annotation.FieldEncrypt;
import top.charles7c.continew.starter.security.crypto.autoconfigure.CryptoProperties;
import top.charles7c.continew.starter.security.crypto.encryptor.IEncryptor;

import java.lang.reflect.Field;
import java.util.*;

/**
 * 字段加密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
@Intercepts({@Signature(method = "update", type = Executor.class, args = {MappedStatement.class, Object.class}),})
public class MyBatisEncryptInterceptor extends AbstractMyBatisInterceptor {

    private CryptoProperties properties;

    public MyBatisEncryptInterceptor(CryptoProperties properties) {
        this.properties = properties;
    }

    public MyBatisEncryptInterceptor() {
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object[] args = invocation.getArgs();
        MappedStatement mappedStatement = (MappedStatement)args[0];
        SqlCommandType sqlCommandType = mappedStatement.getSqlCommandType();
        if (!(SqlCommandType.UPDATE == sqlCommandType || SqlCommandType.INSERT == sqlCommandType)) {
            return invocation.proceed();
        }
        Object obj = args[1];
        // 兼容 MyBatis Plus 封装的 update 相关方法，updateById、update
        if (obj instanceof Map map) {
            Object entity = map.get(Constants.ENTITY);
            this.doEncrypt(this.getEncryptFields(entity), entity);
        } else {
            this.doEncrypt(this.getEncryptFields(obj), obj);
        }
        return invocation.proceed();
    }

    /**
     * 处理加密
     *
     * @param fieldList 加密字段列表
     * @param entity    实体
     * @throws Exception /
     */
    private void doEncrypt(List<Field> fieldList, Object entity) throws Exception {
        for (Field field : fieldList) {
            IEncryptor encryptor = super.getEncryptor(field);
            Object fieldValue = ReflectUtil.getFieldValue(entity, field);
            // 优先获取自定义对称加密算法密钥，获取不到时再获取全局配置
            String password = ObjectUtil.defaultIfBlank(field.getAnnotation(FieldEncrypt.class).password(), properties
                .getPassword());
            String ciphertext = encryptor.encrypt(fieldValue.toString(), password, properties.getPublicKey());
            ReflectUtil.setFieldValue(entity, field, ciphertext);
        }
    }
}
