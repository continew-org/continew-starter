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

package top.charles7c.continew.starter.web.autoconfigure.xss;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * XSS 过滤配置属性
 *
 * @author whhya
 * @since 2.0.0
 */
@ConfigurationProperties(PropertiesConstants.XSS)
public class XssProperties {

    /**
     * 是否启用 XSS 过滤
     */
    private boolean enabled = true;

    /**
     * 拦截路由（默认为空）
     *
     * <p>
     * 当拦截的路由配置不为空，则根据该配置执行过滤
     * </p>
     */
    private List<String> includePatterns = new ArrayList<>();

    /**
     * 放行路由（默认为空）
     */
    private List<String> excludePatterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getIncludePatterns() {
        return includePatterns;
    }

    public void setIncludePatterns(List<String> includePatterns) {
        this.includePatterns = includePatterns;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }
}
