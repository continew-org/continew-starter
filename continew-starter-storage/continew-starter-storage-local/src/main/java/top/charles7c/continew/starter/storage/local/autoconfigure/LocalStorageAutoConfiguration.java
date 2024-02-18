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

package top.charles7c.continew.starter.storage.local.autoconfigure;

import cn.hutool.core.text.CharSequenceUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;
import top.charles7c.continew.starter.core.constant.StringConstants;

import java.util.Map;

/**
 * 本地文件自动配置
 *
 * @author Charles7c
 * @since 1.1.0
 */
@EnableWebMvc
@AutoConfiguration
@EnableConfigurationProperties(LocalStorageProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.STORAGE_LOCAL, name = PropertiesConstants.ENABLED, havingValue = "true")
public class LocalStorageAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageAutoConfiguration.class);
    private final LocalStorageProperties properties;

    public LocalStorageAutoConfiguration(LocalStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Map<String, LocalStorageProperties.LocalStorageMapping> mappingMap = properties.getMapping();
        for (Map.Entry<String, LocalStorageProperties.LocalStorageMapping> mappingEntry : mappingMap.entrySet()) {
            LocalStorageProperties.LocalStorageMapping mapping = mappingEntry.getValue();
            String pathPattern = mapping.getPathPattern();
            String location = mapping.getLocation();
            if (CharSequenceUtil.isBlank(location)) {
                throw new IllegalArgumentException("Path pattern [%s] location is null.".formatted(pathPattern));
            }
            registry.addResourceHandler(CharSequenceUtil.appendIfMissing(pathPattern, StringConstants.PATH_PATTERN))
                .addResourceLocations(!location.startsWith("file:")
                    ? "file:%s".formatted(this.format(location))
                    : this.format(location))
                .setCachePeriod(0);
        }
    }

    private String format(String location) {
        return location.replace(StringConstants.BACKSLASH, StringConstants.SLASH);
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Storage-Local' completed initialization.");
    }
}
