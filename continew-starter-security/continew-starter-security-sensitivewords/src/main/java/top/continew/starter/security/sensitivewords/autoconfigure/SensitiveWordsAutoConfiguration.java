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

package top.continew.starter.security.sensitivewords.autoconfigure;

import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;
import com.github.houbb.sensitive.word.bs.SensitiveWordBs;
import com.github.houbb.sensitive.word.support.allow.WordAllows;
import com.github.houbb.sensitive.word.support.deny.WordDenys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.security.sensitivewords.service.DefaultSensitiveWordsService;
import top.continew.starter.security.sensitivewords.service.NoOpSensitiveWordsService;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsService;
import top.continew.starter.security.sensitivewords.support.PropertiesWordAllow;
import top.continew.starter.security.sensitivewords.support.PropertiesWordDeny;
import top.continew.starter.security.sensitivewords.support.RuntimeWordAllow;
import top.continew.starter.security.sensitivewords.support.RuntimeWordDeny;
import top.continew.starter.security.sensitivewords.support.SensitiveWordsDictionary;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词自动配置
 *
 * @author luoqiz
 * @author Charles7c
 * @since 3.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SensitiveWordsProperties.class)
public class SensitiveWordsAutoConfiguration {

    private static final Logger LOGGER =
        LoggerFactory.getLogger(SensitiveWordsAutoConfiguration.class);

    /**
     * 运行时敏感词库
     *
     * <p>
     * 内部状态载体，不开放替换。替换后可能出现「增删操作的集合与责任链读取的集合不是同一个实例」的问题。
     * </p>
     *
     * @return 运行时敏感词库
     */
    @Bean
    public RuntimeWordDeny runtimeWordDeny() {
        return new RuntimeWordDeny();
    }

    /**
     * 运行时白名单
     *
     * @return 运行时白名单
     */
    @Bean
    public RuntimeWordAllow runtimeWordAllow() {
        return new RuntimeWordAllow();
    }

    /**
     * 敏感词库注册表
     *
     * <p>
     * 内部结构，不开放替换。替换后责任链里不会有运行时词库，运行时增删会失效。
     * </p>
     *
     * @param properties        敏感词配置属性
     * @param runtimeWordDeny   运行时敏感词库
     * @param runtimeWordAllow  运行时白名单
     * @param denyProvider      自定义敏感词库
     * @param allowProvider     自定义白名单
     * @return 敏感词库注册表
     */
    @Bean
    public SensitiveWordsDictionary sensitiveWordsDictionary(SensitiveWordsProperties properties,
        RuntimeWordDeny runtimeWordDeny,
        RuntimeWordAllow runtimeWordAllow,
        ObjectProvider<IWordDeny> denyProvider,
        ObjectProvider<IWordAllow> allowProvider) {
        List<IWordDeny> staticDenies = new ArrayList<>();
        List<IWordAllow> staticAllows = new ArrayList<>();
        if (properties.isUseDefaultDeny()) {
            staticDenies.add(WordDenys.defaults());
            staticAllows.add(WordAllows.defaults());
        }
        staticDenies.add(new PropertiesWordDeny(properties));
        staticAllows.add(new PropertiesWordAllow(properties));
        denyProvider.orderedStream().forEach(staticDenies::add);
        allowProvider.orderedStream().forEach(staticAllows::add);

        IWordDeny staticDeny = chainDenies(staticDenies);
        IWordAllow staticAllow = chainAllows(staticAllows);
        // 运行时词库通过方法引用适配，不实现 IWordDeny/IWordAllow，避免被 ObjectProvider 重复收集
        IWordDeny deny = WordDenys.chains(staticDeny, runtimeWordDeny::getWords);
        IWordAllow allow = WordAllows.chains(staticAllow, runtimeWordAllow::getWords);
        return new SensitiveWordsDictionary(deny, staticDeny, allow, staticAllow);
    }

    /**
     * 敏感词引擎
     *
     * <p>
     * 显式关闭 {@code wordFailFast}。该参数默认为 {@code true}，会让匹配返回第一个命中而非最长匹配，
     * 导致「法轮功」只匹配到「法轮」，替换后残留「功」。
     * </p>
     *
     * @param properties 敏感词配置属性
     * @param dictionary 敏感词库注册表
     * @return 敏感词引擎
     */
    @Bean(destroyMethod = "destroy")
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = PropertiesConstants.SECURITY_SENSITIVE_WORDS,
        name = PropertiesConstants.ENABLED,
        havingValue = "true",
        matchIfMissing = true)
    public SensitiveWordBs sensitiveWordBs(SensitiveWordsProperties properties,
        SensitiveWordsDictionary dictionary) {
        return SensitiveWordBs.newInstance()
            .wordDeny(dictionary.getDeny())
            .wordAllow(dictionary.getAllow())
            .ignoreRepeat(properties.isIgnoreRepeat())
            .enableNumCheck(properties.isNumCheck())
            .enableEmailCheck(properties.isEmailCheck())
            .enableUrlCheck(properties.isUrlCheck())
            .wordFailFast(false)
            .init();
    }

    /**
     * 敏感词服务
     *
     * <p>
     * {@code enabled} 为 {@code false} 时直接返回空实现。此时即使容器里存在自定义
     * {@code SensitiveWordBs} Bean（逃生舱用法），也不启用检测，保证开关语义无条件成立。
     * </p>
     *
     * @param wordBsProvider     敏感词引擎
     * @param dictionaryProvider 敏感词库注册表
     * @param runtimeWordDeny    运行时敏感词库
     * @param runtimeWordAllow   运行时白名单
     * @param properties         敏感词配置属性
     * @return 敏感词服务
     */
    @Bean
    @ConditionalOnMissingBean
    public SensitiveWordsService sensitiveWordsService(
        ObjectProvider<SensitiveWordBs> wordBsProvider,
        ObjectProvider<SensitiveWordsDictionary> dictionaryProvider,
        RuntimeWordDeny runtimeWordDeny,
        RuntimeWordAllow runtimeWordAllow,
        SensitiveWordsProperties properties) {
        if (!properties.isEnabled()) {
            return new NoOpSensitiveWordsService();
        }
        SensitiveWordBs wordBs = wordBsProvider.getIfAvailable();
        if (wordBs == null) {
            return new NoOpSensitiveWordsService();
        }
        return new DefaultSensitiveWordsService(wordBs,
            dictionaryProvider.getObject(),
            runtimeWordDeny,
            runtimeWordAllow);
    }

    /**
     * 串联黑名单责任链
     *
     * @param denies 黑名单列表
     * @return 黑名单责任链
     */
    private static IWordDeny chainDenies(List<IWordDeny> denies) {
        IWordDeny first = denies.get(0);
        return WordDenys.chains(first, denies.subList(1, denies.size()).toArray(new IWordDeny[0]));
    }

    /**
     * 串联白名单责任链
     *
     * @param allows 白名单列表
     * @return 白名单责任链
     */
    private static IWordAllow chainAllows(List<IWordAllow> allows) {
        IWordAllow first = allows.get(0);
        return WordAllows.chains(first,
            allows.subList(1, allows.size()).toArray(new IWordAllow[0]));
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.debug(
            "[ContiNew Starter] - Auto Configuration 'Security-Sensitive Words' completed initialization.");
    }
}
