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
 * 定义幂等注解
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 19:26
 * @Package top.continew.starter.idempotent.annotation.Idempotent
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Idempotent {

    // 幂等key前缀
    String prefix() default "idempotent:";

    // key的过期时间
    long timeout() default 0;

    // 时间单位
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    // 失败时的提示信息
    String message() default "请勿重复操作";
}