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

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 测试支撑：向 Hutool {@link SpringUtil} 注入受控的 {@link ApplicationContext}
 *
 * <p>
 * {@code Validator} 与 {@code MessageSourceUtils} 的静态字段在类初始化时即调用
 * {@code SpringUtil.getBean}，纯单测环境无 Spring 容器，须在目标类首次加载前注入 mock。
 * Hutool 5.8.48 的 {@code getBeanFactory()} 优先读取 {@code beanFactory} 字段，
 * 同 JVM 内其他测试启动过的容器可能已写入该字段，故两个静态字段须一并覆盖。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class SpringContextTestHolder {

    private SpringContextTestHolder() {
    }

    /**
     * 将 mock BeanFactory 写入 SpringUtil 的两个静态字段，供静态初始化的容器依赖读取
     *
     * @param beanFactory 受控的 mock BeanFactory
     */
    public static void inject(ConfigurableListableBeanFactory beanFactory) {
        ReflectionTestUtils.setField(SpringUtil.class, "beanFactory", beanFactory);
        ReflectionTestUtils.setField(SpringUtil.class, "applicationContext", null);
    }
}
