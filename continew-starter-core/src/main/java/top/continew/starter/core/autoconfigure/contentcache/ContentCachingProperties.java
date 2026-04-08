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

package top.continew.starter.core.autoconfigure.contentcache;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.SpringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 应用配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = PropertiesConstants.WEB_CONTENT_CACHE)
public class ContentCachingProperties {

    /**
     * 缓存大小限制
     */
    private int cacheLimit = 0;

    /**
     * 放行路由
     */
    private List<String> excludePatterns = new ArrayList<>();

    public int getCacheLimit() {
        return cacheLimit;
    }

    public void setCacheLimit(int cacheLimit) {
        this.cacheLimit = cacheLimit;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    /**
     * 是否匹配放行路由
     *
     * @param uri 请求 URI
     * @return true: 匹配; false: 不匹配
     */
    public boolean isMatchExcludeUri(String uri) {
        return this.getExcludePatterns().stream().anyMatch(pattern -> SpringUtils.isMatch(uri, pattern));
    }
}
