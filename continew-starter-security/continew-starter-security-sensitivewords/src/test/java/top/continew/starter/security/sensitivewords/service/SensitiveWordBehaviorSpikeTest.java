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

import com.github.houbb.sensitive.word.api.IWordResult;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.result.WordResultHandlers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * 上游敏感词引擎行为验证
 *
 * <p>
 * 用于在正式断言前钉死三方库的实际行为，避免基于文档或印象做设计。
 * </p>
 *
 * @author luoqiz
 * @since 3.0.0
 */
class SensitiveWordBehaviorSpikeTest {

    /**
     * 关闭快速失败后应返回最长匹配
     */
    @Test
    @DisplayName("wordFailFast=false 时返回最长匹配")
    void longestMatchWhenFailFastDisabled() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮", "法轮功"))
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .init();
        assertThat(wordBs.findAll("法轮功")).containsExactly("法轮功");
        assertThat(wordBs.replace("法轮功")).isEqualTo("***");
    }

    /**
     * 默认配置下快速失败为 true，只返回第一个命中
     */
    @Test
    @DisplayName("默认 wordFailFast=true 时只返回短词")
    void shortMatchWhenFailFastEnabled() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮", "法轮功"))
            .wordAllow(() -> List.of())
            .init();
        assertThat(wordBs.findAll("法轮功")).containsExactly("法轮");
    }

    /**
     * 下标为原文坐标，endIndex 排他
     */
    @Test
    @DisplayName("endIndex 为排他下标")
    void endIndexIsExclusive() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .init();
        String text = "a法轮功b";
        List<IWordResult> results = wordBs.findAll(text, WordResultHandlers.raw());
        assertThat(results).hasSize(1);
        IWordResult result = results.get(0);
        assertThat(text.substring(result.startIndex(), result.endIndex())).isEqualTo("法轮功");
    }

    /**
     * 重复字符归一化
     */
    @Test
    @DisplayName("ignoreRepeat=true 时识别重复字符")
    void ignoreRepeat() {
        SensitiveWordBs repeated = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of())
            .ignoreRepeat(true)
            .wordFailFast(false)
            .init();
        assertThat(repeated.contains("法法轮轮功")).isTrue();

        SensitiveWordBs plain = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of())
            .ignoreRepeat(false)
            .wordFailFast(false)
            .init();
        assertThat(plain.contains("法法轮轮功")).isFalse();
    }

    /**
     * 重复字符归一化只影响匹配，不改变下标语义
     */
    @Test
    @DisplayName("ignoreRepeat 命中区间覆盖原文完整重复段")
    void ignoreRepeatIndicesCoverFullOriginalSpan() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of())
            .ignoreRepeat(true)
            .wordFailFast(false)
            .init();

        String text = "前法法轮功后";
        List<IWordResult> results = wordBs.findAll(text, WordResultHandlers.raw());
        assertThat(results).hasSize(1);
        IWordResult result = results.get(0);
        assertThat(result.startIndex()).isEqualTo(1);
        assertThat(result.endIndex()).isEqualTo(5);
        assertThat(text.substring(result.startIndex(), result.endIndex())).as("归一化只影响匹配，下标仍为原文坐标")
            .isEqualTo("法法轮功");
        // 重复字符被一并遮挡，长度保持
        assertThat(wordBs.replace(text)).isEqualTo("前****后");
    }

    /**
     * 代理对（emoji）不破坏下标：仍按 Java char 计数
     */
    @Test
    @DisplayName("代理对文本的下标按 char 计数")
    void surrogatePairIndicesAreCharBased() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("敏感词"))
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .init();

        // 首尾各一个 emoji（U+1F600 / U+1F601，各占 2 个 char）
        String text = "😀敏感词🙂";
        List<IWordResult> results = wordBs.findAll(text, WordResultHandlers.raw());
        assertThat(results).hasSize(1);
        IWordResult result = results.get(0);
        assertThat(result.startIndex()).as("emoji 占 2 个 char，命中起点为 2").isEqualTo(2);
        assertThat(result.endIndex()).isEqualTo(5);
        assertThat(text.substring(result.startIndex(), result.endIndex())).as("切片不得截断代理对")
            .isEqualTo("敏感词");
        assertThat(wordBs.replace(text)).as("替换后 emoji 完整保留")
            .isEqualTo("😀***🙂");
    }

    /**
     * findFirst 按文本位置返回最左命中
     */
    @Test
    @DisplayName("findFirst 返回文本最左的命中")
    void findFirstReturnsLeftmostHit() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("甲词", "乙词"))
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .init();
        assertThat(wordBs.findFirst("乙词X甲词")).as("按文本位置而非词库顺序").isEqualTo("乙词");
        assertThat(wordBs.findFirst("甲词X乙词")).isEqualTo("甲词");
        assertThat(wordBs.findFirst("无命中内容")).isNull();
    }

    /**
     * 数字检测的阈值为 8 位及以上连续数字
     */
    @Test
    @DisplayName("numCheck 只命中 8 位及以上连续数字")
    void numCheckThreshold() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of())
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .enableNumCheck(true)
            .init();
        assertThat(wordBs.findAll("1234567")).isEmpty();
        assertThat(wordBs.findAll("12345678")).containsExactly("12345678");
        assertThat(wordBs.findAll("联系电话13800138000")).containsExactly("13800138000");
    }

    /**
     * 显式配置的词走词库匹配，与 num-check 之类的特殊检测开关无关
     *
     * <p>
     * 开关只决定「未显式配置的」数字、邮箱、网址是否自动视为敏感内容，关掉开关不会让已配置的词失效。
     * </p>
     */
    @Test
    @DisplayName("配置词命中不受特殊检测开关影响")
    void configuredWordIndependentOfSpecialChecks() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("13800138000"))
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .enableNumCheck(false)
            .init();
        assertThat(wordBs.findAll("联系13800138000")).as("显式配置的号码按词库词命中")
            .containsExactly("13800138000");
        assertThat(wordBs.findAll("联系13900139000")).as("未配置的号码不命中").isEmpty();
    }

    /**
     * 数字、邮箱、网址检测的命中下标同为原文坐标
     *
     * <p>
     * 服务层 match 直接对原文截取片段，上游若改回归一化后的坐标会错位甚至越界，此处钉死。
     * </p>
     */
    @Test
    @DisplayName("特殊检测命中下标为原文坐标")
    void specialCheckIndicesAreOriginal() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of())
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .enableNumCheck(true)
            .enableEmailCheck(true)
            .enableUrlCheck(true)
            .init();
        String text = "联系电话13800138000 邮箱 admin@example.com 网址 http://example.com/page";
        List<IWordResult> results = wordBs.findAll(text, WordResultHandlers.raw());
        assertThat(results).hasSize(3);
        List<String> fragments = results.stream()
            .map(r -> text.substring(r.startIndex(), r.endIndex()))
            .toList();
        assertThat(fragments).containsExactlyInAnyOrder("13800138000",
            "admin@example.com",
            "http://example.com/page");
    }

    /**
     * 白名单可放行数字、邮箱、网址检测的命中
     *
     * <p>
     * 完整匹配的白名单词参与特殊检测的比较，可用于放行特定的手机号、邮箱或网址。
     * </p>
     */
    @Test
    @DisplayName("白名单可放行特殊检测命中")
    void allowExemptsSpecialCheck() {
        SensitiveWordBs numBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of())
            .wordAllow(() -> List.of("13800138000"))
            .wordFailFast(false)
            .enableNumCheck(true)
            .init();
        assertThat(numBs.findAll("13800138000")).isEmpty();
        assertThat(numBs.findAll("13800138001")).containsExactly("13800138001");

        SensitiveWordBs emailBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of())
            .wordAllow(() -> List.of("admin@example.com"))
            .wordFailFast(false)
            .enableEmailCheck(true)
            .init();
        assertThat(emailBs.findAll("admin@example.com")).isEmpty();
        assertThat(emailBs.findAll("other@example.com")).containsExactly("other@example.com");
    }

    /**
     * 黑白名单同名时白名单优先
     */
    @Test
    @DisplayName("黑白名单同名时白名单胜出")
    void allowWinsOnSameWord() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of("法轮功"))
            .wordFailFast(false)
            .init();
        assertThat(wordBs.contains("法轮功")).isFalse();
    }

    /**
     * 白名单短于黑名单时不放行更长的词
     */
    @Test
    @DisplayName("白名单短于黑名单时仍命中长词")
    void shorterAllowDoesNotExemptLongerWord() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of("法轮"))
            .wordFailFast(false)
            .init();
        assertThat(wordBs.findAll("法轮功")).containsExactly("法轮功");
    }

    /**
     * 白名单更长时，挡不住文本中较短的敏感词
     *
     * <p>
     * 白名单词只有能在文本中完整匹配才参与比较：文本只出现较短的敏感词时，
     * 更长的白名单词无法完整匹配，按 0 长度参与比较，敏感词仍然命中。
     * </p>
     */
    @Test
    @DisplayName("白名单更长时仍命中较短的敏感词")
    void longerAllowDoesNotExemptShorterWord() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("法轮功"))
            .wordAllow(() -> List.of("法轮功啊"))
            .wordFailFast(false)
            .init();
        assertThat(wordBs.findAll("法轮功")).containsExactly("法轮功");
        assertThat(wordBs.findAll("法轮功啊")).isEmpty();
    }

    /**
     * 白名单跨过黑名单起始位置时会漏检，记录实际行为
     */
    @Test
    @DisplayName("白名单跳过字符时可能掩盖相邻敏感词")
    void allowSkipMayHideAdjacentWord() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of("BCD"))
            .wordAllow(() -> List.of("ABC"))
            .wordFailFast(false)
            .init();
        List<String> hits = wordBs.findAll("ABCD");
        assertThat(hits).as("记录上游行为：白名单跳过 3 个字符后从 D 继续，BCD 被漏检").isEmpty();
    }

    /**
     * 增量 API 在 init 之后仍然可用
     */
    @Test
    @DisplayName("init 后增量增删生效")
    void incrementalApiWorksAfterInit() {
        SensitiveWordBs wordBs = SensitiveWordBs.newInstance()
            .wordDeny(() -> List.of())
            .wordAllow(() -> List.of())
            .wordFailFast(false)
            .init();
        assertThat(wordBs.contains("新词")).isFalse();

        wordBs.addWord(List.of("新词"));
        assertThat(wordBs.contains("新词")).isTrue();

        wordBs.removeWord(List.of("新词"));
        assertThat(wordBs.contains("新词")).isFalse();

        wordBs.addWordAllow(List.of("放行词"));
        wordBs.addWord(List.of("放行词"));
        assertThat(wordBs.contains("放行词")).isFalse();
    }
}
