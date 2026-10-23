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

import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.result.WordResultHandlers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import top.continew.starter.security.sensitivewords.model.SensitiveWordHit;
import top.continew.starter.security.sensitivewords.model.SensitiveWordsRefreshResult;
import top.continew.starter.security.sensitivewords.support.RuntimeWordAllow;
import top.continew.starter.security.sensitivewords.support.RuntimeWordDeny;
import top.continew.starter.security.sensitivewords.support.SensitiveWordsDictionary;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 默认敏感词服务
 *
 * <p>
 * 维护两条不变量：{@code staticWords} / {@code staticAllows} 只保存来自内置词库、配置和用户自定义
 * 词库的词，由 {@link #refresh()} 整体更新；运行时词由 {@code RuntimeWordDeny} / {@code RuntimeWordAllow}
 * 单独维护。检索引擎的字典是扁平的，不区分词来自哪一层，因此增删运行时词时必须用静态快照判断该词是否已由静态来源提供。
 * </p>
 *
 * @author luoqiz
 * @author Charles7c
 * @since 3.0.0
 */
public class DefaultSensitiveWordsService implements SensitiveWordsService {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(DefaultSensitiveWordsService.class);

    private static final String DENY_REFRESH_LOG =
        "[ContiNew Starter] - 敏感词库刷新：黑名单静态 {} 条（+{} / -{}），运行时 {} 条";

    private static final String ALLOW_REFRESH_LOG =
        "[ContiNew Starter] - 敏感词库刷新：白名单静态 {} 条（+{} / -{}），运行时 {} 条";

    private final SensitiveWordBs wordBs;

    private final SensitiveWordsDictionary dictionary;

    private final RuntimeWordDeny runtimeWordDeny;

    private final RuntimeWordAllow runtimeWordAllow;

    private final Object refreshLock = new Object();

    /**
     * 静态黑名单快照（不含运行时词）
     */
    private Set<String> staticWords;

    /**
     * 静态白名单快照（不含运行时词）
     */
    private Set<String> staticAllows;

    public DefaultSensitiveWordsService(SensitiveWordBs wordBs,
        SensitiveWordsDictionary dictionary,
        RuntimeWordDeny runtimeWordDeny,
        RuntimeWordAllow runtimeWordAllow) {
        this.wordBs = wordBs;
        this.dictionary = dictionary;
        this.runtimeWordDeny = runtimeWordDeny;
        this.runtimeWordAllow = runtimeWordAllow;
        // 引擎初始化阶段已加载过一次，这里再取一次作为差量基线；静态链不含运行时词库
        this.staticWords = new LinkedHashSet<>(dictionary.getStaticDeny().deny());
        this.staticAllows = new LinkedHashSet<>(dictionary.getStaticAllow().allow());
        LOGGER.info("[ContiNew Starter] - 敏感词服务初始化完成：静态黑名单 {} 条，静态白名单 {} 条",
            this.staticWords.size(),
            this.staticAllows.size());
    }

    @Override
    public boolean contains(String text) {
        if (!StringUtils.hasText(text)) {
            return false;
        }
        return wordBs.contains(text);
    }

    @Override
    public List<String> findAll(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        // 引擎按命中次数返回，这里去重，便于统计与打标
        return wordBs.findAll(text)
            .stream()
            .distinct()
            .toList();
    }

    @Override
    public String findFirst(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return wordBs.findFirst(text);
    }

    @Override
    public List<SensitiveWordHit> match(String text) {
        if (!StringUtils.hasText(text)) {
            return List.of();
        }
        return wordBs.findAll(text, WordResultHandlers.raw())
            .stream()
            .map(r -> new SensitiveWordHit(text.substring(r.startIndex(), r.endIndex()),
                r.startIndex(),
                r.endIndex()))
            .toList();
    }

    @Override
    public String replace(String text) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        return wordBs.replace(text);
    }

    @Override
    public String replace(String text, char replaceChar) {
        if (!StringUtils.hasText(text)) {
            return text;
        }
        List<SensitiveWordHit> hits = match(text);
        if (hits.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        // 从后往前替换，避免前面的替换影响后续下标
        for (int i = hits.size() - 1; i >= 0; i--) {
            SensitiveWordHit hit = hits.get(i);
            for (int j = hit.startIndex(); j < hit.endIndex(); j++) {
                sb.setCharAt(j, replaceChar);
            }
        }
        return sb.toString();
    }

    @Override
    public void addWord(String... words) {
        if (words == null) {
            return;
        }
        synchronized (refreshLock) {
            List<String> added = runtimeWordDeny.add(words);
            // 静态词库已有的词，字典中本来就有，不重复添加
            List<String> toAdd =
                added.stream().filter(word -> !staticWords.contains(word)).toList();
            if (!toAdd.isEmpty()) {
                wordBs.addWord(toAdd);
            }
        }
    }

    @Override
    public void removeWord(String... words) {
        if (words == null) {
            return;
        }
        synchronized (refreshLock) {
            // 入参中属于静态词库的词无法删除，即使从未加入过运行时词库也要提示
            List<String> notRemovable = Arrays.stream(words)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(staticWords::contains)
                .toList();
            if (!notRemovable.isEmpty()) {
                LOGGER.warn(
                    "[ContiNew Starter] - 以下敏感词来自内置或配置词库，无法通过 removeWord 删除，如需放行请使用 addWordAllow：{}",
                    notRemovable);
            }
            List<String> removed = runtimeWordDeny.remove(words);
            List<String> toRemove =
                removed.stream().filter(word -> !staticWords.contains(word)).toList();
            if (!toRemove.isEmpty()) {
                wordBs.removeWord(toRemove);
            }
        }
    }

    @Override
    public void addWordAllow(String... words) {
        if (words == null) {
            return;
        }
        synchronized (refreshLock) {
            List<String> added = runtimeWordAllow.add(words);
            List<String> toAdd =
                added.stream().filter(word -> !staticAllows.contains(word)).toList();
            if (!toAdd.isEmpty()) {
                wordBs.addWordAllow(toAdd);
            }
        }
    }

    @Override
    public void removeWordAllow(String... words) {
        if (words == null) {
            return;
        }
        synchronized (refreshLock) {
            // 入参中属于静态白名单的词无法删除，即使从未加入过运行时白名单也要提示
            List<String> notRemovable = Arrays.stream(words)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .filter(staticAllows::contains)
                .toList();
            if (!notRemovable.isEmpty()) {
                LOGGER.warn(
                    "[ContiNew Starter] - 以下白名单词来自内置或配置词库，无法通过 removeWordAllow 删除，如需重新拦截请从词库来源中移除：{}",
                    notRemovable);
            }
            List<String> removed = runtimeWordAllow.remove(words);
            List<String> toRemove =
                removed.stream().filter(word -> !staticAllows.contains(word)).toList();
            if (!toRemove.isEmpty()) {
                wordBs.removeWordAllow(toRemove);
            }
        }
    }

    @Override
    public SensitiveWordsRefreshResult refresh() {
        synchronized (refreshLock) {
            // 运行时词集只取一次，避免多次遍历集合
            Set<String> runtimeWords = new HashSet<>(runtimeWordDeny.getWords());
            Set<String> runtimeAllowWords = new HashSet<>(runtimeWordAllow.getWords());

            // 静态链不含运行时词库，直接取值即可，不做减法
            Set<String> currentStatic = new LinkedHashSet<>(dictionary.getStaticDeny().deny());

            Set<String> denyAdded = difference(currentStatic, staticWords);
            Set<String> denyRemoved = difference(staticWords, currentStatic);
            // 静态库删掉的词，如果运行时层还留着，不能从字典删除
            denyRemoved.removeAll(runtimeWords);

            if (!denyAdded.isEmpty()) {
                wordBs.addWord(denyAdded);
            }
            if (!denyRemoved.isEmpty()) {
                wordBs.removeWord(denyRemoved);
            }
            this.staticWords = currentStatic;

            Set<String> currentAllowStatic =
                new LinkedHashSet<>(dictionary.getStaticAllow().allow());

            Set<String> allowAdded = difference(currentAllowStatic, staticAllows);
            Set<String> allowRemoved = difference(staticAllows, currentAllowStatic);
            allowRemoved.removeAll(runtimeAllowWords);

            if (!allowAdded.isEmpty()) {
                wordBs.addWordAllow(allowAdded);
            }
            if (!allowRemoved.isEmpty()) {
                wordBs.removeWordAllow(allowRemoved);
            }
            this.staticAllows = currentAllowStatic;

            SensitiveWordsRefreshResult result =
                new SensitiveWordsRefreshResult(currentStatic.size(),
                    runtimeWords.size(),
                    denyAdded.size(),
                    denyRemoved.size(),
                    currentAllowStatic.size(),
                    runtimeAllowWords.size(),
                    allowAdded.size(),
                    allowRemoved.size());
            boolean changed = !denyAdded.isEmpty() || !denyRemoved.isEmpty()
                || !allowAdded.isEmpty()
                || !allowRemoved.isEmpty();
            // 无差量时降为 debug，避免定时刷新持续刷屏
            if (changed) {
                LOGGER.info(DENY_REFRESH_LOG, result.denyStaticCount(), result.denyAdded(),
                    result.denyRemoved(), result.denyRuntimeCount());
                LOGGER.info(ALLOW_REFRESH_LOG, result.allowStaticCount(), result.allowAdded(),
                    result.allowRemoved(), result.allowRuntimeCount());
            } else {
                LOGGER.debug(DENY_REFRESH_LOG, result.denyStaticCount(), result.denyAdded(),
                    result.denyRemoved(), result.denyRuntimeCount());
                LOGGER.debug(ALLOW_REFRESH_LOG, result.allowStaticCount(), result.allowAdded(),
                    result.allowRemoved(), result.allowRuntimeCount());
            }
            return result;
        }
    }

    /**
     * 计算两个词库的差集
     *
     * @param source 来源词库
     * @param target 对比词库
     * @return 存在于来源但不存在于对比的词
     */
    private Set<String> difference(Set<String> source, Set<String> target) {
        Set<String> result = new LinkedHashSet<>(source);
        result.removeAll(target);
        return result;
    }
}
