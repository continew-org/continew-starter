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

import cn.hutool.core.util.ReflectUtil;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.exception.BusinessException;

import java.lang.invoke.MethodHandleProxies;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 反射工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class ReflectUtils {

    private ReflectUtils() {
    }

    /**
     * 获得一个类中所有非静态字段名列表，包括其父类中的字段<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param beanClass 类
     * @return 非静态字段名列表
     * @throws SecurityException 安全检查异常
     */
    public static List<String> getNonStaticFieldsName(Class<?> beanClass) throws SecurityException {
        List<Field> nonStaticFields = getNonStaticFields(beanClass);
        return CollUtils.mapToList(nonStaticFields, Field::getName);
    }

    /**
     * 获得一个类中所有非静态字段列表，包括其父类中的字段<br>
     * 如果子类与父类中存在同名字段，则这两个字段同时存在，子类字段在前，父类字段在后。
     *
     * @param beanClass 类
     * @return 非静态字段列表
     * @throws SecurityException 安全检查异常
     */
    public static List<Field> getNonStaticFields(Class<?> beanClass) throws SecurityException {
        Field[] fields = ReflectUtil.getFields(beanClass);
        return Arrays.stream(fields).filter(f -> !Modifier.isStatic(f.getModifiers()))
            .collect(Collectors.toList());
    }

    /**
     * 通过反射创建方法引用，支持在父类中查找方法
     *
     * @param clazz      实体类类型
     * @param methodName 方法名
     * @param <T>        实体类类型
     * @param <K>        返回值类型
     * @return {@code Function<T, K>} 方法引用
     * @throws IllegalArgumentException 如果参数不合法
     * @author lishuyan
     * @since 2.13.2
     */
    @SuppressWarnings("unchecked")
    public static <T, K> Function<T, K> createMethodReference(Class<T> clazz, String methodName) {
        try {
            Method method = ReflectUtil.getMethodByName(clazz, methodName);
            method.setAccessible(true);
            return MethodHandleProxies.asInterfaceInstance(Function.class,
                MethodHandles.lookup().unreflect(method));
        } catch (Exception e) {
            throw new BusinessException(
                "创建方法引用失败：" + clazz.getName() + StringConstants.DOT + methodName, e);
        }
    }
}
