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

package top.continew.starter.sensitive.words.service;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import top.continew.starter.sensitive.words.autoconfigure.SensitiveWordsProperties;

import java.util.List;

/**
 * 默认敏感词配置
 */
@Component
@ConditionalOnProperty(prefix = "continew.sensitive-words", name = "type", havingValue = "default", matchIfMissing = true)
public class DefaultSensitiveWordsConfig implements SensitiveWordsConfig {

    private final SensitiveWordsProperties properties;

    public DefaultSensitiveWordsConfig(SensitiveWordsProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> getWords() {
        if (properties.getValues() != null) {
            return properties.getValues();
        }
        return List.of();
    }
}
