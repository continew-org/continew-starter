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

/**
 * 数字工具类
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class NumberUtils {

    private NumberUtils() {
    }

    /**
     * 如果给定值为0，返回默认值，否则返回原值
     *
     * @param value        值
     * @param defaultValue 默认值
     * @return 默认值或非0值
     */
    public static int zero2Default(int value, int defaultValue) {
        return 0 == value ? defaultValue : value;
    }
}
