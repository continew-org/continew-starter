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

package top.continew.starter.ratelimiter.generator;

import cn.hutool.core.util.ClassUtil;
import top.continew.starter.core.constant.StringConstants;

import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认限流器名称生成器
 *
 * @author Charles7c
 * @since 2.2.0
 */
public class DefaultRateLimiterNameGenerator implements RateLimiterNameGenerator {

    protected final ConcurrentHashMap<Method, String> nameMap = new ConcurrentHashMap<>();

    @Override
    public String generate(Object target, Method method, Object... args) {
        return nameMap.computeIfAbsent(method, key -> {
            final StringBuilder nameSb = new StringBuilder();
            String className = method.getDeclaringClass().getName();
            nameSb.append(ClassUtil.getShortClassName(className));
            nameSb.append(StringConstants.DOT);
            nameSb.append(method.getName());
            nameSb.append(StringConstants.ROUND_BRACKET_START);
            for (Class<?> clazz : method.getParameterTypes()) {
                this.getDescriptor(nameSb, clazz);
            }
            nameSb.append(StringConstants.ROUND_BRACKET_END);
            return nameSb.toString();
        });
    }

    /**
     * 获取指定数据类型的描述
     *
     * @param sb        名称字符串缓存
     * @param typeClass 数据类型
     */
    private void getDescriptor(final StringBuilder sb, final Class<?> typeClass) {
        Class<?> clazz = typeClass;
        while (true) {
            if (clazz.isPrimitive()) {
                sb.append(this.getPrimitiveChar(clazz));
                return;
            } else if (clazz.isArray()) {
                sb.append(StringConstants.BRACKET_START);
                clazz = clazz.getComponentType();
            } else {
                sb.append('L');
                String name = clazz.getName();
                name = ClassUtil.getShortClassName(name);
                sb.append(name);
                sb.append(StringConstants.SEMICOLON);
                return;
            }
        }
    }

    /**
     * 根据基本数据获取类型字符
     *
     * @param clazz 数据类型
     * @return 类型字符
     */
    private char getPrimitiveChar(Class<?> clazz) {
        char c;
        if (clazz == Integer.TYPE) {
            c = 'I';
        } else if (clazz == Void.TYPE) {
            c = 'V';
        } else if (clazz == Boolean.TYPE) {
            c = 'Z';
        } else if (clazz == Byte.TYPE) {
            c = 'B';
        } else if (clazz == Character.TYPE) {
            c = 'C';
        } else if (clazz == Short.TYPE) {
            c = 'S';
        } else if (clazz == Double.TYPE) {
            c = 'D';
        } else if (clazz == Float.TYPE) {
            c = 'F';
        } else {
            c = 'J';
        }
        return c;
    }
}
