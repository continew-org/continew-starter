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

import top.continew.starter.security.sensitivewords.model.SensitiveWordHit;
import top.continew.starter.security.sensitivewords.model.SensitiveWordsRefreshResult;

import java.util.List;

/**
 * 空敏感词服务（模块关闭时使用）
 *
 * @author luoqiz
 * @since 3.0.0
 */
public class NoOpSensitiveWordsService implements SensitiveWordsService {

    @Override
    public boolean contains(String text) {
        return false;
    }

    @Override
    public List<String> findAll(String text) {
        return List.of();
    }

    @Override
    public String findFirst(String text) {
        return null;
    }

    @Override
    public List<SensitiveWordHit> match(String text) {
        return List.of();
    }

    @Override
    public String replace(String text) {
        return text;
    }

    @Override
    public String replace(String text, char replaceChar) {
        return text;
    }

    @Override
    public void addWord(String... words) {
    }

    @Override
    public void removeWord(String... words) {
    }

    @Override
    public void addWordAllow(String... words) {
    }

    @Override
    public void removeWordAllow(String... words) {
    }

    @Override
    public SensitiveWordsRefreshResult refresh() {
        return SensitiveWordsRefreshResult.empty();
    }
}
