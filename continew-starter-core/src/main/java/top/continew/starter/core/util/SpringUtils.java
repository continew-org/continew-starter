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

/**
 * Spring 工具类
 *
 * @author Charles7c
 * @since 2.8.2
 */
public class SpringUtils {

    private SpringUtils() {
    }

    /**
     * 获取代理对象
     *
     * @param target 目标对象
     * @param <T>    目标对象类型
     * @return 代理对象
     * @since 2.8.2
     */
    public static <T> T getProxy(T target) {
        return (T)SpringUtil.getBean(target.getClass());
    }
}
