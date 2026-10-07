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

package top.continew.starter.json.jackson.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.json.jackson.enums.BigNumberSerializeMode;

/**
 * Jackson 扩展配置属性
 *
 * @author Jasmine
 * @author Charles7c
 * @since 2.12.1
 */
@ConfigurationProperties("spring.jackson")
public class JacksonExtensionProperties {

    /**
     * 大数值序列化模式
     */
    private BigNumberSerializeMode bigNumberSerializeMode = BigNumberSerializeMode.FLEXIBLE;

    public BigNumberSerializeMode getBigNumberSerializeMode() {
        return bigNumberSerializeMode;
    }

    public void setBigNumberSerializeMode(BigNumberSerializeMode bigNumberSerializeMode) {
        this.bigNumberSerializeMode = bigNumberSerializeMode;
    }
}
