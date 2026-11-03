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

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 自动配置 imports 注册完整性断言工具
 *
 * <p>判定规则：扫描基础包下所有标注 {@link AutoConfiguration} 的顶层具体类型，与 classpath 上全部
 * {@code AutoConfiguration.imports} 资源中已注册的类型求差集，差集为空即注册完整。抽象类与嵌套类
 * 一律排除，自动配置入口类型不应属于这两种形态。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class AutoConfigurationImportsAssert {

    private AutoConfigurationImportsAssert() {
    }

    /**
     * 扫描给定基础包下所有 {@link AutoConfiguration} 类型，返回未注册进 imports 资源的全限定类名列表
     *
     * @param basePackages
     *            待扫描的基础包名，至少一个
     * @return 已扫描到但未注册进 imports 的全限定类名列表，空列表表示注册完整
     */
    public static List<String> findUnregistered(String... basePackages) {
        List<String> unregistered = new ArrayList<>();
        if (basePackages == null || basePackages.length == 0) {
            return unregistered;
        }
        Set<BeanDefinition> candidates = scanAutoConfigurationTypes(basePackages);
        if (candidates.isEmpty()) {
            return unregistered;
        }
        List<String> registered =
            ImportCandidates.load(AutoConfiguration.class, AutoConfigurationImportsAssert.class
                .getClassLoader()).getCandidates();
        for (BeanDefinition candidate : candidates) {
            String className = candidate.getBeanClassName();
            if (!registered.contains(className)) {
                unregistered.add(className);
            }
        }
        return unregistered;
    }

    /**
     * 扫描基础包下标注 {@link AutoConfiguration} 的类型
     *
     * <p>在扫描器默认过滤（独立且具体）的基础上追加顶层类型约束，静态嵌套类同样不属于自动配置入口。</p>
     *
     * @param basePackages
     *            待扫描的基础包名，至少一个
     * @return 扫描到的自动配置类型 BeanDefinition 集合，可能为空集合
     */
    private static Set<BeanDefinition> scanAutoConfigurationTypes(String... basePackages) {
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false) {

                @Override
                protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                    return beanDefinition.getMetadata().isIndependent()
                        && beanDefinition.getMetadata().isConcrete()
                        && !beanDefinition.getMetadata().hasEnclosingClass();
                }
            };
        scanner.addIncludeFilter(new AnnotationTypeFilter(AutoConfiguration.class));
        Set<BeanDefinition> candidates = new HashSet<>();
        for (String basePackage : basePackages) {
            candidates.addAll(scanner.findCandidateComponents(basePackage));
        }
        return candidates;
    }
}
