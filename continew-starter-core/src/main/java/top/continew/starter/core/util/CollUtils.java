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

package top.continew.starter.core.util;

import cn.hutool.core.collection.CollUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 集合相关工具类
 *
 * @author Charles7c
 * @since 2.13.1
 */
public class CollUtils {

    private CollUtils() {
    }

    /**
     * 通过 func 自定义一个规则，此规则将原集合中的元素转换成新的元素，生成新的列表返回<br>
     * 例如：提供一个 Bean 列表，通过 Function 接口实现获取某个字段值，返回这个字段值组成的新列表
     *
     * @param <T>        集合元素类型
     * @param <R>        返回集合元素类型
     * @param collection 原集合
     * @param func       编辑函数
     * @return 抽取后的新列表（默认去除 null 值）
     * @see CollUtil#map(Iterable, Function, boolean)
     */
    public static <T, R> List<R> mapToList(Collection<T> collection,
        Function<? super T, ? extends R> func) {
        return mapToList(collection, func, true);
    }

    /**
     * 通过 func 自定义一个规则，此规则将原集合中的元素转换成新的元素，生成新的列表返回<br>
     * 例如：提供一个 Bean 列表，通过 Function 接口实现获取某个字段值，返回这个字段值组成的新列表
     *
     * @param <T>        集合元素类型
     * @param <R>        返回集合元素类型
     * @param collection 原集合
     * @param func       编辑函数
     * @param ignoreNull 是否忽略空值，这里的空值包括函数处理前和处理后的 null 值
     * @return 抽取后的新列表
     * @see CollUtil#map(Iterable, Function, boolean)
     */
    public static <T, R> List<R> mapToList(Collection<T> collection,
        Function<? super T, ? extends R> func,
        boolean ignoreNull) {
        if (CollUtil.isEmpty(collection)) {
            return new ArrayList<>(0);
        }
        Stream<T> stream = collection.stream();
        if (ignoreNull) {
            return stream.filter(Objects::nonNull).map(func).filter(Objects::nonNull)
                .collect(Collectors.toList());
        }
        return stream.map(func).collect(Collectors.toList());
    }

    /**
     * 通过 func 自定义一个规则，此规则将原集合中的元素转换成新的元素，生成新的集合返回<br>
     * 例如：提供一个 Bean 集合，通过 Function 接口实现获取某个字段值，返回这个字段值组成的新集合
     *
     * @param <T>        集合元素类型
     * @param <R>        返回集合元素类型
     * @param collection 原集合
     * @param func       编辑函数
     * @return 抽取后的新集合（默认去除 null 值）
     * @see CollUtil#map(Iterable, Function, boolean)
     */
    public static <T, R> Set<R> mapToSet(Collection<T> collection,
        Function<? super T, ? extends R> func) {
        return mapToSet(collection, func, true);
    }

    /**
     * 通过 func 自定义一个规则，此规则将原集合中的元素转换成新的元素，生成新的集合返回<br>
     * 例如：提供一个 Bean 集合，通过 Function 接口实现获取某个字段值，返回这个字段值组成的新集合
     *
     * @param <T>        集合元素类型
     * @param <R>        返回集合元素类型
     * @param collection 原集合
     * @param func       编辑函数
     * @param ignoreNull 是否忽略空值，这里的空值包括函数处理前和处理后的 null 值
     * @return 抽取后的新集合
     * @see CollUtil#map(Iterable, Function, boolean)
     */
    public static <T, R> Set<R> mapToSet(Collection<T> collection,
        Function<? super T, ? extends R> func,
        boolean ignoreNull) {
        if (CollUtil.isEmpty(collection)) {
            return new HashSet<>(0);
        }
        Stream<T> stream = collection.stream();
        if (ignoreNull) {
            return stream.filter(Objects::nonNull).map(func).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        }
        return stream.map(func).collect(Collectors.toSet());
    }
}
