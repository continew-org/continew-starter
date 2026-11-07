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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import cn.hutool.extra.spring.SpringUtil;
import net.dreamlu.mica.ip2region.core.Ip2regionSearcher;
import net.dreamlu.mica.ip2region.core.IpInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

/**
 * IP 工具类测试
 *
 * <p>
 * 通过静态模拟 {@link SpringUtil} 注入受控的 {@link Ip2regionSearcher}，固化内网判定、本地库查询与
 * 归属地字段拼接行为，防止依赖升级引起输出格式回归。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class IpUtilsTest {

    /**
     * 构造归属地信息（IP 归属地字段均为独立 setter 赋值）
     *
     * @param province 省份，可为 {@code null}
     * @return 归属地信息
     */
    private static IpInfo buildInfo(String province) {
        IpInfo info = new IpInfo();
        info.setCountry("中国");
        info.setProvince(province);
        info.setCity("深圳市");
        info.setIsp("电信");
        return info;
    }

    @Test
    @DisplayName("回环地址识别为内网 IP，不触发本地库查询")
    void shouldReturnIntranetMarkForLoopbackIp() {
        assertThat(IpUtils.getIpv4Address("127.0.0.1")).isEqualTo("内网IP");
        assertThat(IpUtils.getIpv4Address("0:0:0:0:0:0:0:1")).isEqualTo("内网IP");
    }

    @Test
    @DisplayName("私网地址识别为内网 IP，不触发本地库查询")
    void shouldReturnIntranetMarkForPrivateIp() {
        assertThat(IpUtils.getIpv4Address("192.168.1.100")).isEqualTo("内网IP");
    }

    @Test
    @DisplayName("Ip2regionSearcher 未装配时抛出 Bean 缺失异常")
    void shouldThrowWhenSearcherBeanMissing() {
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Ip2regionSearcher.class))
                .thenThrow(new NoSuchBeanDefinitionException(Ip2regionSearcher.class));
            assertThatThrownBy(() -> IpUtils.getIpv4Address("1.2.4.113"))
                .isInstanceOf(NoSuchBeanDefinitionException.class);
        }
    }

    @Test
    @DisplayName("本地库未命中时返回 null")
    void shouldReturnNullWhenSearcherMiss() {
        Ip2regionSearcher searcher = mock(Ip2regionSearcher.class);
        when(searcher.memorySearch("1.2.4.113")).thenReturn(null);
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Ip2regionSearcher.class)).thenReturn(searcher);
            assertThat(IpUtils.getIpv4Address("1.2.4.113")).isNull();
        }
    }

    @Test
    @DisplayName("归属地按国家、省份、城市、运营商四段拼接")
    void shouldJoinFieldsInOrder() {
        Ip2regionSearcher searcher = mock(Ip2regionSearcher.class);
        when(searcher.memorySearch("1.2.4.113")).thenReturn(buildInfo("广东省"));
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Ip2regionSearcher.class)).thenReturn(searcher);
            assertThat(IpUtils.getIpv4Address("1.2.4.113")).isEqualTo("中国|广东省|深圳市|电信");
        }
    }

    @Test
    @DisplayName("缺失字段不产生空段，输出按实际字段数拼接")
    void shouldSkipNullFields() {
        Ip2regionSearcher searcher = mock(Ip2regionSearcher.class);
        when(searcher.memorySearch("1.2.4.113")).thenReturn(buildInfo(null));
        try (MockedStatic<SpringUtil> springUtil = mockStatic(SpringUtil.class)) {
            springUtil.when(() -> SpringUtil.getBean(Ip2regionSearcher.class)).thenReturn(searcher);
            assertThat(IpUtils.getIpv4Address("1.2.4.113")).isEqualTo("中国|深圳市|电信");
        }
    }
}
