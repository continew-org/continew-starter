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

package top.continew.starter.web.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 异常别名
 * <p>
 * 声明一个异常类型映射到的响应码、响应信息和 HTTP 状态码，需通过
 * {@code ExceptionAliasRegister#doRegisterExceptionAlias(Class)} 完成注册后生效。
 * </p>
 *
 * @author Charles7c
 * @since 3.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ExceptionAliasFor {

    /**
     * 该别名配置生效的异常类型（可列多个，注册后这些类型及其子类生效）
     *
     * @return 异常类型数组
     */
    Class<? extends Throwable>[] aliasFor();

    /**
     * 响应码
     *
     * @return 响应码
     */
    String code() default "";

    /**
     * 响应信息
     *
     * @return 响应信息
     */
    String msg() default "";

    /**
     * HTTP 状态码，小于等于 0 时使用全局默认值
     *
     * @return HTTP 状态码
     */
    int httpStatusCode() default 0;
}
