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

package top.continew.starter.data.idgenerator;

import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import me.ahoo.cosid.snowflake.SnowflakeId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;

/**
 * MyBatis Plus ID 生成器 - CosId
 *
 * @author Charles7c
 * @since 1.4.0
 */
public class MyBatisPlusCosIdIdentifierGenerator implements IdentifierGenerator {

    // "__share__SnowflakeId" 为 CosId 框架内置的共享雪花算法 Bean 名称，无法修改其命名；
    // 该类由 @Bean 方法创建并经 Spring 后置处理注入，沿用字段注入以保持 ID 生成器的轻量实例化方式
    @SuppressWarnings({"java:S6830", "java:S6813"})
    @Qualifier("__share__SnowflakeId")
    @Lazy
    @Autowired
    private SnowflakeId snowflakeId;

    @Override
    public Number nextId(Object entity) {
        return snowflakeId.generate();
    }
}
