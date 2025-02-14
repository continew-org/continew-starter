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

import cn.hutool.dfa.WordTree;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.List;

/**
 * 默认敏感词服务
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.9.0
 */
@ConditionalOnBean(SensitiveWordsConfig.class)
@ConditionalOnMissingBean(SensitiveWordsService.class)
public class DefaultSensitiveWordsService implements SensitiveWordsService {

    private final SensitiveWordsConfig sensitiveWordsConfig;
    private final WordTree tree = new WordTree();

    public DefaultSensitiveWordsService(SensitiveWordsConfig sensitiveWordsConfig) {
        this.sensitiveWordsConfig = sensitiveWordsConfig;
        if (sensitiveWordsConfig != null && sensitiveWordsConfig.getWords() != null) {
            tree.addWords(sensitiveWordsConfig.getWords());
        }
    }

    @Override
    public List<String> check(String content) {
        return tree.matchAll(content, -1, false, true);
    }
}
