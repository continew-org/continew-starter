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

package top.continew.starter.idempotent.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 幂等注解
 *
 * @author loach
 * @author Charles7c
 * @since 2.10.0
 */
@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {

    /**
     * 名称
     */
    String name() default "";

    /**
     * 键（支持 Spring EL 表达式）
     */
    String key() default "";

    /**
     * 超时时间
     */
    int timeout() default 1000;

    /**
     * 时间单位（默认：毫秒）
     */
    TimeUnit unit() default TimeUnit.MILLISECONDS;

    /**
     * 提示信息
     */
    String message() default "请勿重复操作";
}