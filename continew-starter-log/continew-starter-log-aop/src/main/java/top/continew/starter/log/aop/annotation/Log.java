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

package top.continew.starter.log.aop.annotation;

import top.continew.starter.log.core.enums.Include;

import java.lang.annotation.*;

/**
 * 日志注解
 * <p>用于接口方法或类上</p>
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 所属模块（用于接口方法或类上）
     */
    String module() default "";

    /**
     * 日志描述 - 接口操作内容（仅用于接口方法上）
     */
    String value() default "";

    /**
     * 包含信息（在全局配置基础上扩展包含信息）
     */
    Include[] includes() default {};

    /**
     * 排除信息（在全局配置基础上减少包含信息）
     */
    Include[] excludes() default {};
}
