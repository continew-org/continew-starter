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

package top.continew.starter.log.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.log.enums.Include;
import top.continew.starter.web.util.SpringWebUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 日志配置属性
 *
 * @author Charles7c
 * @since 1.1.0
 */
@ConfigurationProperties(PropertiesConstants.LOG)
public class LogProperties {

    /**
     * 是否启用日志
     */
    private boolean enabled = true;

    /**
     * 访问日志配置
     */
    @NestedConfigurationProperty
    private AccessLogProperties accessLog = new AccessLogProperties();

    /**
     * 包含信息
     */
    private Set<Include> includes = Include.defaultIncludes();

    /**
     * 放行路由
     */
    private List<String> excludePatterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Set<Include> getIncludes() {
        return includes;
    }

    public void setIncludes(Set<Include> includes) {
        this.includes = includes;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public AccessLogProperties getAccessLog() {
        return accessLog;
    }

    public void setAccessLog(AccessLogProperties accessLog) {
        this.accessLog = accessLog;
    }

    /**
     * 是否匹配放行路由
     *
     * @param uri 请求 URI
     * @return 是否匹配
     */
    public boolean isMatch(String uri) {
        return this.getExcludePatterns().stream().anyMatch(pattern -> SpringWebUtils.isMatch(uri, pattern));
    }
}
