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

package top.continew.starter.data.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 查询注解容器
 *
 * <p>
 * 作为 {@link Query} 可重复声明的载体，一般无需直接使用；同一字段声明多个 {@code @Query}
 * 时由编译器自动合成该容器注解，读取时通过 {@code Field#getAnnotationsByType(Query.class)}
 * 展开。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Queries {

    /**
     * 查询注解集合
     *
     * @return 查询注解集合
     */
    Query[] value();
}
