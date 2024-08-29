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

package top.continew.starter.data.mp.autoconfigure;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import top.continew.starter.core.constant.PropertiesConstants;

import java.lang.annotation.*;

/**
 * 是否启用数据权限注解
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@ConditionalOnProperty(prefix = "mybatis-plus.extension.data-permission", name = PropertiesConstants.ENABLED, havingValue = "true")
public @interface ConditionalOnEnabledDataPermission {
}