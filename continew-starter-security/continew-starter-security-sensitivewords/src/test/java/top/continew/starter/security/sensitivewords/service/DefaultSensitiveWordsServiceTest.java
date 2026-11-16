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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import top.continew.starter.security.sensitivewords.autoconfigure.SensitiveWordsProperties;
import top.continew.starter.security.sensitivewords.model.SensitiveWordHit;
import top.continew.starter.security.sensitivewords.model.SensitiveWordsRefreshResult;
import top.continew.starter.security.sensitivewords.support.PropertiesWordAllow;
import top.continew.starter.security.sensitivewords.support.PropertiesWordDeny;
import top.continew.starter.security.sensitivewords.support.RuntimeWordAllow;
import top.continew.starter.security.sensitivewords.support.RuntimeWordDeny;
import top.continew.starter.security.sensitivewords.support.SensitiveWordsDictionary;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 默认敏感词服务测试
 *
 * @author luoqiz
 * @since 3.0.0
 */
class DefaultSensitiveWordsServiceTest {

    private final List<String> staticDenyWords = new ArrayList<>();

    private final List<String> staticAllowWords = new ArrayList<>();

    private RuntimeWordDeny runtimeWordDeny;

    private RuntimeWordAllow runtimeWordAllow;

    private SensitiveWordBs wordBs;

    private ListAppender<ILoggingEvent> logAppender;

    private Logger serviceLogger;

    private Level originalLogLevel;

    private DefaultSensitiveWordsService service;

    @BeforeEach
    void setUp() {
        staticDenyWords.clear();
        staticAllowWords.clear();
        staticDenyWords.add("静态词");

        runtimeWordDeny = new RuntimeWordDeny();
        runtimeWordAllow = new RuntimeWordAllow();

        SensitiveWordsProperties properties = new SensitiveWordsProperties();
        properties.setUseDefaultDeny(false);
        properties.setDenyWords(staticDenyWords);
        properties.setAllowWords(staticAllowWords);

        IWordDeny staticDeny = WordDenys.chains(new PropertiesWordDeny(properties));
        IWordAllow staticAllow = WordAllows.chains(new PropertiesWordAllow(properties));
        SensitiveWordsDictionary dictionary = new SensitiveWordsDictionary(
            WordDenys.chains(staticDeny,
                runtimeWordDeny::getWords),
            staticDeny, WordAllows.chains(staticAllow, runtimeWordAllow::getWords),
            staticAllow);
        wordBs = spy(SensitiveWordBs.newInstance()
            .wordDeny(dictionary.getDeny())
            .wordAllow(dictionary.getAllow())
            .wordFailFast(false)
            .init());
        service =
            new DefaultSensitiveWordsService(wordBs, dictionary, runtimeWordDeny, runtimeWordAllow);

        serviceLogger = (Logger) LoggerFactory.getLogger(DefaultSensitiveWordsService.class);
        originalLogLevel = serviceLogger.getLevel();
        serviceLogger.setLevel(Level.DEBUG);
        logAppender = new ListAppender<>();
        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
        logAppender.stop();
        serviceLogger.setLevel(originalLogLevel);
    }

    private long refreshLogCount(Level level) {
        return logAppender.list.stream()
            .filter(event -> event.getFormattedMessage().contains("敏感词库刷新"))
            .filter(event -> event.getLevel() == level)
            .count();
    }

    /**
     * 静态词应被识别
     */
    @Test
    @DisplayName("静态词命中")
    void staticWordMatched() {
        assertThat(service.contains("这是静态词")).isTrue();
        assertThat(service.findAll("这是静态词")).containsExactly("静态词");
    }

    /**
     * findAll 去重且返回原文片段
     *
     * <p>
     * 引擎按命中次数返回，同一个词出现多次会重复返回，findAll 在此基础上做去重；
     * 片段取自原文，大小写、全半角等归一化只影响匹配，不改变返回形态，
     * 因此同一个词的不同写法会分别返回。
     * </p>
     */
    @Test
    @DisplayName("findAll 去重并返回原文片段")
    void findAllDeduplicates() {
        service.addWord("临时词");
        assertThat(service.findAll("静态词临时词静态词"))
            .containsExactly("静态词", "临时词");

        service.addWord("abc");
        assertThat(service.findAll("ABCａｂｃabc"))
            .containsExactly("ABC", "ａｂｃ", "abc");
    }

    /**
     * 空输入直接返回未命中
     */
    @Test
    @DisplayName("空输入不抛异常")
    void blankInput() {
        assertThat(service.contains(null)).isFalse();
        assertThat(service.contains("")).isFalse();
        assertThat(service.contains("   ")).isFalse();
        assertThat(service.findAll(null)).isEmpty();
        assertThat(service.findFirst(null)).isNull();
        assertThat(service.match(null)).isEmpty();
        assertThat(service.replace(null)).isNull();
        assertThat(service.replace("", '*')).isEmpty();
    }

    /**
     * 命中位置与原文片段
     */
    @Test
    @DisplayName("match 返回原文片段与下标")
    void matchReturnsOriginalFragment() {
        List<SensitiveWordHit> hits = service.match("a静态词b");
        assertThat(hits).hasSize(1);
        SensitiveWordHit hit = hits.get(0);
        assertThat(hit.word()).isEqualTo("静态词");
        assertThat(hit.startIndex()).isEqualTo(1);
        assertThat(hit.endIndex()).isEqualTo(4);
    }

    /**
     * 替换保持长度不变
     */
    @Test
    @DisplayName("替换后长度不变")
    void replaceKeepsLength() {
        assertThat(service.replace("a静态词b")).isEqualTo("a***b");
        assertThat(service.replace("a静态词b", '#')).isEqualTo("a###b");
    }

    /**
     * 双参替换未命中时原样返回
     */
    @Test
    @DisplayName("replace 未命中时返回原文")
    void replaceNoHitReturnsOriginal() {
        assertThat(service.replace("这是正常文本", '#')).isEqualTo("这是正常文本");
        assertThat(service.replace("", '#')).isEmpty();
        assertThat(service.replace(null, '#')).isNull();
    }

    /**
     * 两个 replace 重载对同一文本的遮挡结果应一致
     */
    @Test
    @DisplayName("replace 两个重载结果一致")
    void replaceOverloadsAgree() {
        // 多命中、首尾命中、未命中三种分布
        assertThat(service.replace("静态词静态词")).isEqualTo(service.replace("静态词静态词", '*'));
        assertThat(service.replace("静态词abc")).isEqualTo(service.replace("静态词abc", '*'));
        assertThat(service.replace("这是正常文本")).isEqualTo(service.replace("这是正常文本", '*'));
    }

    /**
     * 运行时新增词立即生效
     */
    @Test
    @DisplayName("addWord 立即生效")
    void addWordTakesEffectImmediately() {
        assertThat(service.contains("临时词")).isFalse();
        service.addWord("临时词");
        assertThat(service.contains("临时词")).isTrue();
    }

    /**
     * 删除运行时词
     */
    @Test
    @DisplayName("removeWord 删除运行时词")
    void removeRuntimeWord() {
        service.addWord("临时词");
        service.removeWord("临时词");
        assertThat(service.contains("临时词")).isFalse();
    }

    /**
     * 增删的词前后空白应被忽略
     */
    @Test
    @DisplayName("增删词忽略前后空白")
    void addAndRemoveTrimWhitespace() {
        service.addWord("  临时词  ");
        assertThat(runtimeWordDeny.getWords()).containsExactly("临时词");
        assertThat(service.contains("临时词")).isTrue();

        service.removeWord(" 临时词 ");
        assertThat(runtimeWordDeny.getWords()).isEmpty();
        assertThat(service.contains("临时词")).isFalse();
    }

    /**
     * 核心回归：对静态词执行 add 再 remove，不能破坏静态词
     */
    @Test
    @DisplayName("addWord 静态词后再 removeWord 不影响静态词")
    void removeStaticWordDoesNotBreakDictionary() {
        service.addWord("静态词");
        assertThat(service.contains("静态词")).isTrue();

        service.removeWord("静态词");
        assertThat(service.contains("静态词")).isTrue();
    }

    /**
     * 核心回归：上述场景刷新后仍应命中，且不产生差量
     */
    @Test
    @DisplayName("addWord 静态词后 removeWord 再刷新仍命中")
    void refreshAfterRemoveStaticWord() {
        service.addWord("静态词");
        service.removeWord("静态词");

        SensitiveWordsRefreshResult result = service.refresh();
        assertThat(service.contains("静态词")).isTrue();
        assertThat(result.denyAdded()).isZero();
        assertThat(result.denyRemoved()).isZero();
    }

    /**
     * 核心回归：先刷新再删词，仍不能破坏静态词
     *
     * <p>
     * 静态词快照若按「完整链减运行时词」计算，刷新后该词会从快照中消失，后续删词就会误删字典里的静态词。
     * </p>
     */
    @Test
    @DisplayName("addWord 静态词后刷新再 removeWord 仍命中")
    void removeStaticWordAfterRefresh() {
        service.addWord("静态词");
        service.refresh();

        service.removeWord("静态词");
        assertThat(service.contains("静态词")).isTrue();
    }

    /**
     * 从未添加过的静态词，删除请求不进入运行时集合
     */
    @Test
    @DisplayName("removeWord 未添加过的静态词不进入运行时集合")
    void removeNeverAddedStaticWordKeepsRuntimeEmpty() {
        service.removeWord("静态词");
        assertThat(runtimeWordDeny.getWords()).isEmpty();
        assertThat(service.contains("静态词")).isTrue();
    }

    /**
     * 并发刷新与检测不抛异常，且刷新不破坏既有词、正确应用变更
     *
     * <p>
     * 检测线程每轮断言静态词始终可检测（refresh 误删即失败）；刷新线程交替增删
     * 静态词并验证检测结果跟随变更，避免「不抛异常」式的空转通过。
     * </p>
     */
    @Test
    @DisplayName("并发刷新与检测不抛异常且结果正确")
    void concurrentRefreshAndCheck() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(4);
        try {
            CountDownLatch latch = new CountDownLatch(1);
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                futures.add(pool.submit(() -> {
                    awaitQuietly(latch);
                    for (int j = 0; j < 200; j++) {
                        assertThat(service.contains("这是静态词的文本"))
                            .as("刷新不得破坏既有静态词")
                            .isTrue();
                    }
                }));
            }
            futures.add(pool.submit(() -> {
                awaitQuietly(latch);
                for (int j = 0; j < 30; j++) {
                    staticDenyWords.add("并发词");
                    service.refresh();
                    assertThat(service.contains("并发词")).isTrue();

                    staticDenyWords.remove("并发词");
                    service.refresh();
                    assertThat(service.contains("并发词")).isFalse();
                }
            }));
            latch.countDown();
            for (Future<?> future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }
        assertThat(service.contains("静态词")).isTrue();
        assertThat(service.contains("并发词")).isFalse();
    }

    private static void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * 白名单放行与撤销
     */
    @Test
    @DisplayName("addWordAllow 放行后可撤销")
    void allowThenCancel() {
        service.addWordAllow("静态词");
        assertThat(service.contains("静态词")).isFalse();

        service.removeWordAllow("静态词");
        assertThat(service.contains("静态词")).isTrue();
    }

    /**
     * 静态词库变更后刷新生效
     */
    @Test
    @DisplayName("refresh 应用静态词库变更")
    void refreshAppliesStaticChanges() {
        assertThat(service.contains("新增词")).isFalse();
        staticDenyWords.add("新增词");
        staticDenyWords.remove("静态词");

        SensitiveWordsRefreshResult result = service.refresh();
        assertThat(service.contains("新增词")).isTrue();
        assertThat(service.contains("静态词")).isFalse();
        assertThat(result.denyAdded()).isEqualTo(1);
        assertThat(result.denyRemoved()).isEqualTo(1);
    }

    /**
     * 刷新不影响运行时词
     */
    @Test
    @DisplayName("refresh 不影响运行时词")
    void refreshKeepsRuntimeWords() {
        service.addWord("临时词");
        SensitiveWordsRefreshResult result = service.refresh();
        assertThat(service.contains("临时词")).isTrue();
        assertThat(result.denyRuntimeCount()).isEqualTo(1);
        assertThat(result.denyAdded()).isZero();
    }

    /**
     * 刷新幂等
     *
     * <p>
     * 先变更静态词库让第一次刷新产生差量，再验证第二次刷新无差量；
     * 若 refresh 为空实现，第一次刷新后的断言就会失败，避免恒真通过。
     * </p>
     */
    @Test
    @DisplayName("refresh 幂等")
    void refreshIsIdempotent() {
        staticDenyWords.add("新增词");
        service.refresh();
        assertThat(service.contains("新增词")).isTrue();

        SensitiveWordsRefreshResult second = service.refresh();
        assertThat(second.denyAdded()).isZero();
        assertThat(second.denyRemoved()).isZero();
        assertThat(second.allowAdded()).isZero();
        assertThat(second.allowRemoved()).isZero();
    }

    /**
     * 重复添加与空白词过滤
     */
    @Test
    @DisplayName("重复词与空白词被过滤")
    void duplicateAndBlankFiltered() {
        service.addWord("临时词");
        service.addWord("临时词");
        service.addWord("   ");
        assertThat(runtimeWordDeny.getWords()).containsExactly("临时词");
    }

    /**
     * 多个命中时从后往前替换，下标互不影响
     */
    @Test
    @DisplayName("多命中替换与 match 顺序")
    void replaceMultipleHits() {
        service.addWord("临时词");
        List<SensitiveWordHit> hits = service.match("a静态词b临时词c");
        assertThat(hits).hasSize(2);
        assertThat(hits.get(0).word()).isEqualTo("静态词");
        assertThat(hits.get(0).startIndex()).isEqualTo(1);
        assertThat(hits.get(0).endIndex()).isEqualTo(4);
        assertThat(hits.get(1).word()).isEqualTo("临时词");
        assertThat(hits.get(1).startIndex()).isEqualTo(5);
        assertThat(hits.get(1).endIndex()).isEqualTo(8);

        assertThat(service.replace("a静态词b临时词c")).isEqualTo("a***b***c");
        assertThat(service.replace("a静态词b临时词c", '#')).isEqualTo("a###b###c");
    }

    /**
     * null 数组入参不抛异常
     */
    @Test
    @DisplayName("null 数组入参不抛异常")
    void nullVarargs() {
        service.addWord((String[]) null);
        service.removeWord((String[]) null);
        service.addWordAllow((String[]) null);
        service.removeWordAllow((String[]) null);
        assertThat(service.contains("静态词")).isTrue();
    }

    /**
     * 静态白名单词删不掉，刷新后仍放行
     *
     * <p>
     * 先通过 addWordAllow 把词放入运行时白名单集合，再 removeWordAllow。若不过滤静态白名单词，
     * 引擎里的白名单词会被误删，同名的黑名单词随即生效。
     * </p>
     */
    @Test
    @DisplayName("removeWordAllow 删静态白名单词不影响放行")
    void removeStaticAllowWordDoesNotBreakDictionary() {
        staticAllowWords.add("放行词");
        service.refresh();
        service.addWord("放行词");
        service.addWordAllow("放行词");
        assertThat(service.contains("放行词")).isFalse();

        service.removeWordAllow("放行词");
        assertThat(service.contains("放行词")).isFalse();
        assertThat(runtimeWordAllow.getWords()).isEmpty();
    }

    /**
     * 空白串不应进入检索引擎
     *
     * <p>
     * 引擎对空白串恰好返回空结果，仅断言返回值无法区分「提前拦截」与「查引擎后碰巧无命中」；
     * 对 spy 校验未触发检索才能锁定这条短路约束。
     * </p>
     */
    @Test
    @DisplayName("findFirst 空白串返回 null 且不查引擎")
    void findFirstBlankReturnsNullWithoutEngineCall() {
        assertThat(service.findFirst("   ")).isNull();
        assertThat(service.findFirst("")).isNull();
        assertThat(service.findFirst(null)).isNull();
        verify(wordBs, never()).findFirst(any());
    }

    /**
     * 命中串应返回引擎给出的完整词
     */
    @Test
    @DisplayName("findFirst 命中时返回引擎结果")
    void findFirstReturnsEngineResult() {
        assertThat(service.findFirst("这是静态词")).isEqualTo("静态词");
    }

    /**
     * 静态黑名单已提供的词不应重复写入引擎
     *
     * <p>
     * 引擎字典本已含该词，重复 addWord 不改变可观测行为；用 spy 校验增量接口未被调用
     * 才能锁定「静态词被过滤」这条约束。
     * </p>
     */
    @Test
    @DisplayName("addWord 静态库已含词不重复写入引擎")
    void addWordDoesNotReaddStaticWord() {
        service.addWord("静态词");
        assertThat(runtimeWordDeny.getWords()).containsExactly("静态词");
        verify(wordBs, never()).addWord(anyList());
    }

    /**
     * 静态白名单已提供的词不应重复写入引擎
     */
    @Test
    @DisplayName("addWordAllow 静态库已含词不重复写入引擎")
    void addWordAllowDoesNotReaddStaticAllowWord() {
        staticAllowWords.add("放行词");
        service.refresh();
        clearInvocations(wordBs);

        service.addWordAllow("放行词");
        assertThat(runtimeWordAllow.getWords()).containsExactly("放行词");
        verify(wordBs, never()).addWordAllow(anyList());
    }

    /**
     * 静态黑名单词无法删除时应留下告警
     */
    @Test
    @DisplayName("removeWord 静态词触发告警")
    void removeWordWarnsForStaticWord() {
        service.removeWord("静态词");
        assertThat(logAppender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage())
                .contains("无法通过 removeWord 删除")
                .contains("静态词");
        });
    }

    /**
     * 静态白名单词无法删除时应留下告警
     */
    @Test
    @DisplayName("removeWordAllow 静态白名单词触发告警")
    void removeWordAllowWarnsForStaticAllowWord() {
        staticAllowWords.add("放行词");
        service.refresh();
        logAppender.list.clear();

        service.removeWordAllow("放行词");
        assertThat(logAppender.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage())
                .contains("无法通过 removeWordAllow 删除")
                .contains("放行词");
        });
    }

    /**
     * 静态白名单加入的词应计入 allowAdded 差量
     */
    @Test
    @DisplayName("refresh 检出 allowAdded 差量")
    void refreshDetectsAllowAdded() {
        staticAllowWords.add("放行词");

        SensitiveWordsRefreshResult result = service.refresh();
        assertThat(result.allowAdded()).isEqualTo(1);
        assertThat(result.allowRemoved()).isZero();
        assertThat(refreshLogCount(Level.INFO)).isEqualTo(2);
    }

    /**
     * 静态白名单词被移除应计入 allowRemoved 差量
     *
     * <p>
     * 该路径要在引擎里撤销先前加入的白名单词，覆盖增量解除接口。
     * </p>
     */
    @Test
    @DisplayName("refresh 检出 allowRemoved 差量并撤销引擎白名单")
    void refreshDetectsAllowRemoved() {
        staticAllowWords.add("放行词");
        service.refresh();
        clearInvocations(wordBs);
        logAppender.list.clear();

        staticAllowWords.remove("放行词");
        SensitiveWordsRefreshResult result = service.refresh();
        assertThat(result.allowRemoved()).isEqualTo(1);
        assertThat(result.allowAdded()).isZero();
        verify(wordBs).removeWordAllow(anyCollection());
        assertThat(refreshLogCount(Level.INFO)).isEqualTo(2);
    }

    /**
     * 静态白名单词被移除后重新拦截
     */
    @Test
    @DisplayName("refresh 移除白名单词后该词重新被拦截")
    void refreshAllowRemovedRestoresBlock() {
        staticDenyWords.add("放行词");
        staticAllowWords.add("放行词");
        service.refresh();
        assertThat(service.contains("放行词")).isFalse();

        staticAllowWords.remove("放行词");
        service.refresh();
        assertThat(service.contains("放行词")).isTrue();
    }

    /**
     * 无差量的刷新应走 debug 而非 info
     *
     * <p>
     * 定时刷新会反复调用此方法，无变更时必须降级日志级别；四个差量条件任何一个取反都会让无变更刷新误入 info 分支。
     * </p>
     */
    @Test
    @DisplayName("refresh 无差量时只记 debug 日志")
    void refreshWithoutChangesLogsDebugOnly() {
        service.refresh();
        logAppender.list.clear();

        SensitiveWordsRefreshResult result = service.refresh();
        assertThat(result.denyAdded()).isZero();
        assertThat(result.denyRemoved()).isZero();
        assertThat(result.allowAdded()).isZero();
        assertThat(result.allowRemoved()).isZero();
        assertThat(refreshLogCount(Level.INFO)).isZero();
        assertThat(refreshLogCount(Level.DEBUG)).isEqualTo(2);
    }
}
