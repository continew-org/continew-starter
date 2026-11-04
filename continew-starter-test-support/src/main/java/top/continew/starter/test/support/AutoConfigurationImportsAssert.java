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

package top.continew.starter.test.support;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 自动配置 imports 注册完整性断言工具
 *
 * <p>判定规则：遍历基础包下全部 class 资源，读取字节码元数据，挑出直接标注
 * {@link AutoConfiguration} 的顶层具体类型，与 classpath 上全部 {@code AutoConfiguration.imports}
 * 资源中已登记的类型求差集，差集为空即注册完整。</p>
 *
 * <p>本工具只做静态字节码扫描，不评估任何运行时装配条件。
 * {@code ClassPathScanningCandidateComponentProvider} 会在
 * {@code isCandidateComponent(MetadataReader)} 阶段调用
 * {@code ConditionEvaluator.shouldSkip}，把默认关闭或 Web 环境限定的自动配置提前排除，
 * 因此不能用于漏登记检查。</p>
 *
 * <p>零候选一律视为配置错误并抛出 {@link IllegalStateException}，绝不静默当作「注册完整」，
 * 以免基础包名写错或过滤过强时给出假阳性结论。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class AutoConfigurationImportsAssert {

    private static final String CLASS_RESOURCE_SUFFIX = ".class";

    private AutoConfigurationImportsAssert() {
    }

    /**
     * 扫描给定基础包下所有 {@link AutoConfiguration} 类型，返回未登记进 imports 资源的全限定类名列表
     *
     * <p>结果按类名字典序排序，保证跨环境稳定。扫描不到任何候选时抛出
     * {@link IllegalStateException}，将错误的基础包名或过滤配置暴露为显式失败。</p>
     *
     * @param basePackages
     *            待扫描的基础包名，至少一个
     * @return 已扫描到但未登记进 imports 的全限定类名列表，空列表表示注册完整
     * @throws IllegalStateException
     *             当所有基础包合计扫描不到任何候选类型时抛出
     */
    public static List<String> findUnregistered(String... basePackages) {
        List<String> candidates = scannedCandidates(basePackages);
        if (candidates.isEmpty()) {
            throw new IllegalStateException("未扫描到任何 @AutoConfiguration 类型，请检查基础包名: "
                + describeBasePackages(basePackages));
        }
        Set<String> registered = new LinkedHashSet<>(loadRegisteredCandidates());
        List<String> unregistered = new ArrayList<>();
        for (String candidate : candidates) {
            if (!registered.contains(candidate)) {
                unregistered.add(candidate);
            }
        }
        return unregistered;
    }

    /**
     * 判断给定自动配置类型是否已登记进 classpath 上的 imports 资源
     *
     * <p>供「仅标注 {@code @AutoConfigureBefore} 等非 {@link AutoConfiguration} 注解」的入口做显式
     * 登记断言。这类类型不在候选扫描结果中，不能靠 {@link #findUnregistered(String...)} 间接覆盖。</p>
     *
     * @param autoConfigurationType
     *            待判定的自动配置类型，不可为 {@code null}
     * @return 该类型全限定名出现在 imports 候选列表中时为 {@code true}
     */
    public static boolean isRegistered(Class<?> autoConfigurationType) {
        if (autoConfigurationType == null) {
            return false;
        }
        return loadRegisteredCandidates().contains(autoConfigurationType.getName());
    }

    /**
     * 返回给定基础包下扫描到的全部 {@link AutoConfiguration} 候选类名
     *
     * <p>结果去重并按字典序排序，供工具自身测试与调试核对扫描范围使用。与
     * {@link #findUnregistered(String...)} 不同，本方法允许返回空列表，不把零候选视为错误。</p>
     *
     * @param basePackages
     *            待扫描的基础包名，可为空或 {@code null}
     * @return 扫描到的候选类型全限定类名列表，可能为空列表
     */
    public static List<String> scannedCandidates(String... basePackages) {
        if (basePackages == null || basePackages.length == 0) {
            return Collections.emptyList();
        }
        ClassLoader classLoader = AutoConfigurationImportsAssert.class.getClassLoader();
        PathMatchingResourcePatternResolver resolver =
            new PathMatchingResourcePatternResolver(classLoader);
        CachingMetadataReaderFactory metadataReaderFactory =
            new CachingMetadataReaderFactory(resolver);
        Set<String> candidates = new TreeSet<>();
        for (String basePackage : basePackages) {
            collectCandidates(basePackage, resolver, metadataReaderFactory, candidates);
        }
        return new ArrayList<>(candidates);
    }

    /**
     * 读取 classpath 上全部 imports 资源已登记的候选类名
     *
     * @return imports 候选类名列表，按资源出现顺序保留
     */
    private static List<String> loadRegisteredCandidates() {
        return ImportCandidates.load(AutoConfiguration.class, AutoConfigurationImportsAssert.class
            .getClassLoader()).getCandidates();
    }

    /**
     * 扫描单个基础包，把符合条件的候选类名收集进结果集
     *
     * @param basePackage
     *            待扫描的基础包名
     * @param resolver
     *            类路径资源解析器
     * @param metadataReaderFactory
     *            字节码元数据读取工厂
     * @param candidates
     *            候选类名收集集合
     */
    private static void collectCandidates(String basePackage,
        PathMatchingResourcePatternResolver resolver,
        CachingMetadataReaderFactory metadataReaderFactory,
        Set<String> candidates) {
        if (basePackage == null || basePackage.isBlank()) {
            return;
        }
        String packagePath = ClassUtils.convertClassNameToResourcePath(basePackage);
        String resourcePattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + packagePath
            + "/**/*" + CLASS_RESOURCE_SUFFIX;
        try {
            Resource[] resources = resolver.getResources(resourcePattern);
            for (Resource resource : resources) {
                if (!resource.isReadable()) {
                    continue;
                }
                MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
                AnnotationMetadata annotationMetadata = metadataReader.getAnnotationMetadata();
                if (isAutoConfigurationCandidate(annotationMetadata)) {
                    candidates.add(annotationMetadata.getClassName());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("扫描自动配置类型失败，基础包: " + basePackage, e);
        }
    }

    /**
     * 判断字节码元数据是否为自动配置候选类型
     *
     * <p>约束为直接标注 {@link AutoConfiguration} 且为顶层具体类型，抽象类、接口与成员类一律排除，
     * 自动配置入口不应属于这些形态。判定全程不经过任何条件评估器。</p>
     *
     * @param annotationMetadata
     *            待判定的注解元数据
     * @return 属于自动配置候选类型时为 {@code true}
     */
    private static boolean isAutoConfigurationCandidate(AnnotationMetadata annotationMetadata) {
        return annotationMetadata.hasAnnotation(AutoConfiguration.class.getName())
            && annotationMetadata.isIndependent()
            && annotationMetadata.isConcrete()
            && !annotationMetadata.hasEnclosingClass();
    }

    /**
     * 拼装错误信息中展示的基础包名
     *
     * @param basePackages
     *            基础包名数组，可为 {@code null}
     * @return 用于错误信息的可读字符串
     */
    private static String describeBasePackages(String... basePackages) {
        if (basePackages == null || basePackages.length == 0) {
            return "[]";
        }
        return String.join(", ", basePackages);
    }
}
