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

import cn.hutool.core.text.CharSequenceUtil;

import java.util.function.Function;

/**
 * 字符串工具类
 *
 * @author Charles7c
 * @since 2.0.1
 */
public class StrUtils {

    private StrUtils() {
    }

    /**
     * 如果字符串是{@code null}或者&quot;&quot;或者空白，则返回指定默认字符串，否则针对字符串处理后返回
     *
     * @param str          要转换的字符串
     * @param defaultValue 默认值
     * @param mapper       针对字符串的转换方法
     * @return 转换后的字符串或指定的默认字符串
     * @since 2.0.1
     */
    public static <T> T blankToDefault(CharSequence str, T defaultValue, Function<String, T> mapper) {
        return CharSequenceUtil.isBlank(str) ? defaultValue : mapper.apply(str.toString());
    }
}
