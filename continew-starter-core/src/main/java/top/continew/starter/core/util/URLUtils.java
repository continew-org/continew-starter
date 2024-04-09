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

import cn.hutool.http.HttpUtil;

/**
 * URL（Uniform Resource Locator）统一资源定位符相关工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class URLUtils {

    private URLUtils() {
    }

    /**
     * 提供的 URL 是否为 HTTP URL（协议包括："http"，"https"）
     *
     * @param url URL
     * @return 是否为 HTTP URL
     */
    public static boolean isHttpUrl(String url) {
        return HttpUtil.isHttp(url) || HttpUtil.isHttps(url);
    }
}
