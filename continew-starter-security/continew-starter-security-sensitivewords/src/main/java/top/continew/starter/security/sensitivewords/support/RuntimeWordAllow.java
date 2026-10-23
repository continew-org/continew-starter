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

package top.continew.starter.security.sensitivewords.support;

import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 运行时白名单（进程内可变，重启丢失）
 *
 * <p>
 * 与敏感词库分开实现而不提取基类，便于后续分别演进匹配策略。
 * </p>
 *
 * @author luoqiz
 * @since 3.0.0
 */
public class RuntimeWordAllow {

    private final Set<String> words = ConcurrentHashMap.newKeySet();

    /**
     * 新增白名单词
     *
     * @param words 白名单词
     * @return 实际新增的词（已过滤空白与重复项）
     */
    public List<String> add(String... words) {
        List<String> added = new ArrayList<>();
        for (String word : words) {
            if (StringUtils.hasText(word)) {
                String trimmed = word.trim();
                if (this.words.add(trimmed)) {
                    added.add(trimmed);
                }
            }
        }
        return added;
    }

    /**
     * 删除白名单词
     *
     * @param words 白名单词
     * @return 实际删除的词
     */
    public List<String> remove(String... words) {
        List<String> removed = new ArrayList<>();
        for (String word : words) {
            if (StringUtils.hasText(word)) {
                String trimmed = word.trim();
                if (this.words.remove(trimmed)) {
                    removed.add(trimmed);
                }
            }
        }
        return removed;
    }

    /**
     * 获取全部运行时白名单词
     *
     * @return 白名单词列表
     */
    public List<String> getWords() {
        return new ArrayList<>(words);
    }
}
