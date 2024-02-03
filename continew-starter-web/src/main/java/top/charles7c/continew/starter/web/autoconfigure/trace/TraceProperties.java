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

package top.charles7c.continew.starter.web.autoconfigure.trace;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

/**
 * 链路跟踪配置属性
 *
 * @author Charles7c
 * @since 1.3.0
 */
@ConfigurationProperties(PropertiesConstants.TRACE)
public class TraceProperties {

    /**
     * 是否启用链路跟踪配置
     */
    private boolean enabled = false;

    /**
     * 响应头名称
     */
    private String headerName = "traceId";

    /**
     * TLog 配置
     */
    @NestedConfigurationProperty
    private TLogProperties tlog;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public TLogProperties getTlog() {
        return tlog;
    }

    public void setTlog(TLogProperties tlog) {
        this.tlog = tlog;
    }
}
