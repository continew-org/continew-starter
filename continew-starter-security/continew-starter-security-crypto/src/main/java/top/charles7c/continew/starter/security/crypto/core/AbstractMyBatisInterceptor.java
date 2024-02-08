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

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.extra.spring.SpringUtil;
import org.apache.ibatis.plugin.*;
import top.charles7c.continew.starter.security.crypto.annotation.FieldEncrypt;
import top.charles7c.continew.starter.security.crypto.encryptor.IEncryptor;
import top.charles7c.continew.starter.security.crypto.enums.Algorithm;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 字段解密拦截器
 *
 * @author Charles7c
 * @since 1.4.0
 */
public abstract class AbstractMyBatisInterceptor implements Interceptor {

    /**
     * 获取所有字符串类型、需要加/解密的、有值字段
     *
     * @param obj 对象
     * @return 字段列表
     */
    public List<Field> getEncryptFields(Object obj) {
        if (null == obj) {
            return Collections.emptyList();
        }
        return Arrays.stream(ReflectUtil.getFields(obj.getClass()))
            .filter(field -> String.class.equals(field.getType()))
            .filter(field -> null != field.getAnnotation(FieldEncrypt.class))
            .filter(field -> null != ReflectUtil.getFieldValue(obj, field))
            .toList();
    }

    /**
     * 获取字段加/解密处理器
     *
     * @param field 字段
     * @return 加/解密处理器
     */
    public IEncryptor getEncryptor(Field field) {
        FieldEncrypt fieldEncrypt = field.getAnnotation(FieldEncrypt.class);
        Class<? extends IEncryptor> encryptorClass = fieldEncrypt.encryptor();
        // 使用预定义加/解密处理器
        if (encryptorClass == IEncryptor.class) {
            Algorithm algorithm = fieldEncrypt.value();
            return ReflectUtil.newInstance(algorithm.getEncryptor());
        }
        // 使用自定义加/解密处理器
        return SpringUtil.getBean(encryptorClass);
    }
}