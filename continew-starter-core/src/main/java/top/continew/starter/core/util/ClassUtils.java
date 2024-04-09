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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.TypeUtil;

import java.lang.reflect.Type;

/**
 * 类工具类
 *
 * @author Charles7c
 * @since 1.1.1
 */
public class ClassUtils {

    private ClassUtils() {
    }

    /**
     * 获得给定类的所有泛型参数
     *
     * @param clazz 被检查的类，必须是已经确定泛型类型的类
     * @return {@link Class}[]
     */
    public static Class<?>[] getTypeArguments(Class<?> clazz) {
        final Type[] typeArguments = TypeUtil.getTypeArguments(clazz);
        if (ArrayUtil.isEmpty(typeArguments)) {
            return new Class[0];
        }
        final Class<?>[] classes = new Class<?>[typeArguments.length];
        for (int i = 0; i < typeArguments.length; i++) {
            classes[i] = TypeUtil.getClass(typeArguments[i]);
        }
        return classes;
    }
}
