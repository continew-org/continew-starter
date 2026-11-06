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

package top.continew.starter.openapi.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.openapi.protocol.OpenApiSignProtocol;

import java.util.ArrayList;
import java.util.List;

/**
 * 开放接口签名配置属性
 *
 * @author Charles7c
 * @since 3.1.0
 */
@ConfigurationProperties(PropertiesConstants.OPENAPI)
public class OpenApiSignProperties {

    /**
     * 是否启用（自动配置条件，缺省视为关闭，使用方必须显式开启）
     */
    private Boolean enabled = Boolean.FALSE;

    /**
     * 验签过滤器拦截的路径模式（仅支持 Servlet url-pattern 语法，Ant 风格 {@code **} 不会生效）
     */
    private List<String> includePaths = new ArrayList<>(List.of("/open/*"));

    /**
     * 协议标识，当前唯一取值
     */
    private String algorithm = OpenApiSignProtocol.SCHEME;

    /**
     * 验签参数配置
     */
    private Sign sign = new Sign();

    /**
     * 重放防护配置
     */
    private Replay replay = new Replay();

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIncludePaths() {
        return includePaths;
    }

    public void setIncludePaths(List<String> includePaths) {
        this.includePaths = includePaths;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public Sign getSign() {
        return sign;
    }

    public void setSign(Sign sign) {
        this.sign = sign;
    }

    public Replay getReplay() {
        return replay;
    }

    public void setReplay(Replay replay) {
        this.replay = replay;
    }

    /**
     * 验签参数配置
     *
     * @author Charles7c
     * @since 3.1.0
     */
    public static class Sign {

        /**
         * 时间戳允许窗口（秒），请求时间与服务器时间之差的绝对值上限
         */
        private long timestampWindow = 300;

        /**
         * 额外时钟偏差容忍（秒），叠加在时间戳窗口之上
         */
        private long clockSkew = 0;

        /**
         * 请求体大小上限（字节），超过直接拒绝
         */
        private long maxBodySize = 1048576L;

        /**
         * 签名头长度上限（字节），超过直接拒绝
         */
        private int maxHeaderSize = 4096;

        /**
         * query 长度上限（字节），超过直接拒绝
         */
        private int maxQuerySize = 8192;

        public long getTimestampWindow() {
            return timestampWindow;
        }

        public void setTimestampWindow(long timestampWindow) {
            this.timestampWindow = timestampWindow;
        }

        public long getClockSkew() {
            return clockSkew;
        }

        public void setClockSkew(long clockSkew) {
            this.clockSkew = clockSkew;
        }

        public long getMaxBodySize() {
            return maxBodySize;
        }

        public void setMaxBodySize(long maxBodySize) {
            this.maxBodySize = maxBodySize;
        }

        public int getMaxHeaderSize() {
            return maxHeaderSize;
        }

        public void setMaxHeaderSize(int maxHeaderSize) {
            this.maxHeaderSize = maxHeaderSize;
        }

        public int getMaxQuerySize() {
            return maxQuerySize;
        }

        public void setMaxQuerySize(int maxQuerySize) {
            this.maxQuerySize = maxQuerySize;
        }
    }

    /**
     * 重放防护配置
     *
     * @author Charles7c
     * @since 3.1.0
     */
    public static class Replay {

        /**
         * nonce 占位有效期（秒），应不小于时间戳窗口（含时钟偏差）的两倍
         */
        private long nonceTtl = 600;

        public long getNonceTtl() {
            return nonceTtl;
        }

        public void setNonceTtl(long nonceTtl) {
            this.nonceTtl = nonceTtl;
        }
    }
}
