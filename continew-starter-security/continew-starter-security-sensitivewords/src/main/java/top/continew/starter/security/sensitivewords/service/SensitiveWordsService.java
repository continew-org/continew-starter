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
 * 敏感词服务
 *
 * @author luoqiz
 * @author Charles7c
 * @since 3.0.0
 */
public interface SensitiveWordsService {

    /**
     * 是否包含敏感词
     *
     * @param text 待检测文本
     * @return 是否包含
     */
    boolean contains(String text);

    /**
     * 查找所有敏感词
     *
     * <p>
     * 返回去重后的命中片段。片段取自原文，全半角、大小写等归一化只影响匹配，不改变返回形态，
     * 因此同一个词的不同写法会分别返回。适合统计与打标，需要原文位置或全部命中时用 {@link #match(String)}。
     * </p>
     *
     * @param text 待检测文本
     * @return 敏感词列表，未命中返回空列表
     */
    List<String> findAll(String text);

    /**
     * 查找第一个敏感词
     *
     * <p>
     * 返回文本中最靠左命中的片段，与词库中的声明顺序无关；词库同时存在长短词（如「敏感」与「敏感词」）时
     * 按最长匹配命中。
     * </p>
     *
     * @param text 待检测文本
     * @return 第一个命中的敏感词片段，未命中返回 {@code null}
     */
    String findFirst(String text);

    /**
     * 匹配敏感词及其位置
     *
     * <p>
     * 返回原文片段与下标，适合前端高亮与审核定位。
     * </p>
     *
     * @param text 待检测文本
     * @return 命中结果列表，未命中返回空列表
     */
    List<SensitiveWordHit> match(String text);

    /**
     * 替换敏感词
     *
     * @param text 待检测文本
     * @return 替换后的文本
     */
    String replace(String text);

    /**
     * 替换敏感词（替换后长度与原文一致）
     *
     * <p>
     * 每个命中的 char 替换为一个 {@code replaceChar}，长度保持不变，便于继续按原下标定位文本。
     * 需要替换成其他长度或形态时，请基于 {@link #match(String)} 自行处理。
     * </p>
     *
     * @param text        待检测文本
     * @param replaceChar 替换字符
     * @return 替换后的文本
     */
    String replace(String text, char replaceChar);

    /**
     * 新增运行时敏感词（立即生效，进程内有效，重启丢失）
     *
     * <p>
     * 仅当前节点生效。多实例部署时请参考文档的多节点一致性说明。
     * </p>
     *
     * @param words 敏感词
     */
    void addWord(String... words);

    /**
     * 删除运行时敏感词（立即生效）
     *
     * <p>
     * 只能删除通过 {@link #addWord(String...)} 添加的词。内置词库与配置词库中的词删不掉，
     * 需要放行请使用 {@link #addWordAllow(String...)}。
     * </p>
     *
     * @param words 敏感词
     */
    void removeWord(String... words);

    /**
     * 新增运行时白名单（立即生效，进程内有效，重启丢失）
     *
     * <p>
     * 白名单与黑名单按最长匹配长度比较，长度相同时白名单优先，因此可以放行已存在的黑名单词。
     * </p>
     *
     * @param words 白名单词
     */
    void addWordAllow(String... words);

    /**
     * 删除运行时白名单（立即生效）
     *
     * <p>
     * 只能删除通过 {@link #addWordAllow(String...)} 添加的词。内置词库与配置词库中的白名单词删不掉。
     * </p>
     *
     * @param words 白名单词
     */
    void removeWordAllow(String... words);

    /**
     * 刷新词库（从各词库来源重新加载并差量应用，幂等，不影响运行时词库）
     *
     * @return 刷新结果统计
     */
    SensitiveWordsRefreshResult refresh();
}
