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

package top.continew.starter.security.sensitivewords.service;

import top.continew.starter.security.sensitivewords.autoconfigure.SensitiveWordsProperties;

import java.util.List;

/**
 * 默认敏感词配置
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.9.0
 */
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
