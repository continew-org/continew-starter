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

package top.continew.starter.log.annotation;

import top.continew.starter.log.enums.Include;

import java.lang.annotation.*;

/**
 * 日志注解
 * <p>用于接口方法或类上，辅助 Spring Doc 使用效果最佳</p>
 *
 * @author Charles7c
 * @since 1.1.0
 */
@Documented
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {

    /**
     * 日志描述（仅用于接口方法上）
     * <p>
     * 优先级：@Log("描述") > @Operation(summary="描述")
     * </p>
     */
    String value() default "";

    /**
     * 所属模块（用于接口方法或类上）
     * <p>
     * 优先级： 接口方法上的 @Log(module = "模块") > 接口类上的 @Log(module = "模块") > @Tag(name = "模块") 内容
     * </p>
     */
    String module() default "";

    /**
     * 包含信息（在全局配置基础上扩展包含信息）
     */
    Include[] includes() default {};

    /**
     * 排除信息（在全局配置基础上减少包含信息）
     */
    Include[] excludes() default {};

    /**
     * 是否忽略日志记录（用于接口方法或类上）
     */
    boolean ignore() default false;
}
