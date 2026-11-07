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

package top.continew.starter.data.util;

import java.util.function.Supplier;

/**
 * 查询分组上下文
 *
 * <p>
 * 以 {@link ThreadLocal} 承载当前查询所属的分组标识，供查询构建方（如 CRUD Service）在
 * 不改变既有方法签名的前提下把分组传递到 {@code QueryWrapperHelper}。上下文仅在设置
 * 动作（{@link #callWith(String, Supplier)}、{@link #runWith(String, Runnable)}）的执行
 * 期间生效，结束后恢复原值，支持嵌套且不残留。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class QueryGroupContext {

    private static final ThreadLocal<String> GROUP = new ThreadLocal<>();

    private QueryGroupContext() {
    }

    /**
     * 获取当前线程绑定的分组标识
     *
     * @return 分组标识，未绑定时返回 {@code null}
     * @since 3.1.0
     */
    public static String get() {
        return GROUP.get();
    }

    /**
     * 在指定分组下执行动作
     *
     * @param <T>    动作返回值类型
     * @param group  分组标识
     * @param action 动作
     * @return 动作返回值
     * @since 3.1.0
     */
    public static <T> T callWith(String group, Supplier<T> action) {
        String previous = GROUP.get();
        GROUP.set(group);
        try {
            return action.get();
        } finally {
            restore(previous);
        }
    }

    /**
     * 在指定分组下执行无返回值动作
     *
     * @param group  分组标识
     * @param action 动作
     * @since 3.1.0
     */
    public static void runWith(String group, Runnable action) {
        String previous = GROUP.get();
        GROUP.set(group);
        try {
            action.run();
        } finally {
            restore(previous);
        }
    }

    /**
     * 恢复进入前的分组绑定，进入前未绑定时移除 Entry，避免线程池场景在
     * ThreadLocalMap 中残留 null 值 Entry
     *
     * @param previous 进入前的分组标识
     */
    private static void restore(String previous) {
        if (previous == null) {
            GROUP.remove();
        } else {
            GROUP.set(previous);
        }
    }
}
