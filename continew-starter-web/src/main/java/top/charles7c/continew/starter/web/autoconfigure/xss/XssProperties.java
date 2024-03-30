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
 * xss配置属性
 *
 * @author whhya
 * @since 1.0.0
 */
@ConfigurationProperties(PropertiesConstants.XSS)
public class XssProperties {
    /**
     * 是否启用Xss
     */
    private boolean enabled = true;

    /**
     * 拦截的路由，默认为空
     */
    private List<String> pathPatterns = new ArrayList<>();

    /**
     * 放行的路由，默认为空
     */
    private List<String> pathExcludePatterns = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    public void setPathPatterns(List<String> pathPatterns) {
        this.pathPatterns = pathPatterns;
    }

    public List<String> getPathExcludePatterns() {
        return pathExcludePatterns;
    }

    public void setPathExcludePatterns(List<String> pathExcludePatterns) {
        this.pathExcludePatterns = pathExcludePatterns;
    }

}
