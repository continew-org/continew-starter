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

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.net.NetUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.http.HtmlUtil;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import net.dreamlu.mica.ip2region.core.IpInfo;
import top.continew.starter.core.constant.StringConstants;

import java.util.Objects;
import java.util.Set;

/**
 * IP 工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class IpUtils {

    private IpUtils() {
    }

    /**
     * 查询 IP 归属地（本地库解析）
     *
     * @param ip IP 地址
     * @return IP 归属地
     */
    public static String getIpv4Address(String ip) {
        if (isInnerIpv4(ip)) {
            return "内网IP";
        }
        Ip2regionSearcher ip2regionSearcher = SpringUtil.getBean(Ip2regionSearcher.class);
        IpInfo ipInfo = ip2regionSearcher.memorySearch(ip);
        if (null == ipInfo) {
            return null;
        }
        Set<String> regionSet = CollUtil.newLinkedHashSet(ipInfo.getCountry(), ipInfo.getRegion(), ipInfo
            .getProvince(), ipInfo.getCity(), ipInfo.getIsp());
        regionSet.removeIf(Objects::isNull);
        return String.join(StringConstants.PIPE, regionSet);
    }

    /**
     * 是否为内网 IPv4
     *
     * @param ip IP 地址
     * @return 是否为内网 IP
     */
    public static boolean isInnerIpv4(String ip) {
        return NetUtil.isInnerIP("0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : HtmlUtil.cleanHtmlTag(ip));
    }
}
