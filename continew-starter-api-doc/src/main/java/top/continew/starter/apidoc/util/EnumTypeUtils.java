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

package top.continew.starter.apidoc.util;

import top.continew.starter.core.enums.BaseEnum;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * 枚举类型工具
 *
 * @author echo
 * @since 2.4.0
 */
public class EnumTypeUtils {

    private EnumTypeUtils() {
    }

    /**
     * 获取枚举值类型
     *
     * @param enumClass 枚举类型
     * @return 枚举值类型
     */
    public static String getEnumValueTypeAsString(Class<?> enumClass) {
        try {
            // 获取枚举类实现的所有接口
            Type[] interfaces = enumClass.getGenericInterfaces();
            // 遍历所有接口
            for (Type type : interfaces) {
                // 检查接口是否为参数化类型
                if (type instanceof ParameterizedType parameterizedType) {
                    // 检查接口的原始类型是否为 BaseEnum
                    if (parameterizedType.getRawType() != BaseEnum.class) {
                        continue;
                    }
                    Type actualType = parameterizedType.getActualTypeArguments()[0];
                    // 检查实际类型参数是否为类类型
                    if (actualType instanceof Class<?> actualClass) {
                        if (actualClass == Integer.class) {
                            return "integer";
                        } else if (actualClass == Long.class) {
                            return "long";
                        } else if (actualClass == Double.class) {
                            return "number";
                        } else if (actualClass == String.class) {
                            return "string";
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // ignored
        }
        return "string";
    }
}
