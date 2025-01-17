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

package top.continew.starter.apidoc.handler;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.IoUtil;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.AnnotationsUtils;
import io.swagger.v3.oas.annotations.tags.Tags;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiBuilderCustomizer;
import org.springdoc.core.customizers.ServerBaseUrlCustomizer;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.JavadocProvider;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.SecurityService;
import org.springdoc.core.utils.PropertyResolverUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.method.HandlerMethod;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 自定义 OpenApi 处理器（对源码功能进行修改，增强使用）
 *
 * @author echo
 * @since 2.4.0
 */
@SuppressWarnings("all")
public class OpenApiHandler extends OpenAPIService {

    /**
     * The Basic error controller.
     */
    private static Class<?> basicErrorController;

    /**
     * The Security parser.
     */
    private final SecurityService securityParser;

    /**
     * The Mappings map.
     */
    private final Map<String, Object> mappingsMap = new HashMap<>();

    /**
     * The Springdoc tags.
     */
    private final Map<HandlerMethod, Tag> springdocTags = new HashMap<>();

    /**
     * The Open api builder customisers.
     */
    private final Optional<List<OpenApiBuilderCustomizer>> openApiBuilderCustomisers;

    /**
     * The server base URL customisers.
     */
    private final Optional<List<ServerBaseUrlCustomizer>> serverBaseUrlCustomizers;

    /**
     * The Spring doc config properties.
     */
    private final SpringDocConfigProperties springDocConfigProperties;

    /**
     * The Cached open api map.
     */
    private final Map<String, OpenAPI> cachedOpenAPI = new HashMap<>();

    /**
     * The Property resolver utils.
     */
    private final PropertyResolverUtils propertyResolverUtils;

    /**
     * The javadoc provider.
     */
    private final Optional<JavadocProvider> javadocProvider;

    /**
     * The Context.
     */
    private ApplicationContext context;

    /**
     * The Open api.
     */
    private OpenAPI openAPI;

    /**
     * The Is servers present.
     */
    private boolean isServersPresent;

    /**
     * The Server base url.
     */
    private String serverBaseUrl;

    /**
     * Instantiates a new Open api builder.
     *
     * @param openAPI                   the open api
     * @param securityParser            the security parser
     * @param springDocConfigProperties the spring doc config properties
     * @param propertyResolverUtils     the property resolver utils
     * @param openApiBuilderCustomizers the open api builder customisers
     * @param serverBaseUrlCustomizers  the server base url customizers
     * @param javadocProvider           the javadoc provider
     */
    public OpenApiHandler(Optional<OpenAPI> openAPI,
                          SecurityService securityParser,
                          SpringDocConfigProperties springDocConfigProperties,
                          PropertyResolverUtils propertyResolverUtils,
                          Optional<List<OpenApiBuilderCustomizer>> openApiBuilderCustomizers,
                          Optional<List<ServerBaseUrlCustomizer>> serverBaseUrlCustomizers,
                          Optional<JavadocProvider> javadocProvider) {
        super(openAPI, securityParser, springDocConfigProperties, propertyResolverUtils, openApiBuilderCustomizers, serverBaseUrlCustomizers, javadocProvider);
        if (openAPI.isPresent()) {
            this.openAPI = openAPI.get();
            if (this.openAPI.getComponents() == null) {
                this.openAPI.setComponents(new Components());
            }
            if (this.openAPI.getPaths() == null) {
                this.openAPI.setPaths(new Paths());
            }
            if (CollUtil.isNotEmpty(this.openAPI.getServers())) {
                this.isServersPresent = true;
            }
        }
        this.propertyResolverUtils = propertyResolverUtils;
        this.securityParser = securityParser;
        this.springDocConfigProperties = springDocConfigProperties;
        this.openApiBuilderCustomisers = openApiBuilderCustomizers;
        this.serverBaseUrlCustomizers = serverBaseUrlCustomizers;
        this.javadocProvider = javadocProvider;
        if (springDocConfigProperties.isUseFqn()) {
            TypeNameResolver.std.setUseFqn(true);
        }
    }

    @Override
    public Operation buildTags(HandlerMethod handlerMethod, Operation operation, OpenAPI openAPI, Locale locale) {

        Set<Tag> tags = new HashSet<>();
        Set<String> tagsStr = new HashSet<>();

        buildTagsFromMethod(handlerMethod.getMethod(), tags, tagsStr, locale);
        buildTagsFromClass(handlerMethod.getBeanType(), tags, tagsStr, locale);

        if (CollUtil.isNotEmpty(tagsStr)) {
            tagsStr = tagsStr.stream()
                .map(str -> propertyResolverUtils.resolve(str, locale))
                .collect(Collectors.toSet());
        }

        if (springdocTags.containsKey(handlerMethod)) {
            Tag tag = springdocTags.get(handlerMethod);
            tagsStr.add(tag.getName());
            if (openAPI.getTags() == null || !openAPI.getTags().contains(tag)) {
                openAPI.addTagsItem(tag);
            }
        }

        if (CollUtil.isNotEmpty(tagsStr)) {
            if (CollUtil.isEmpty(operation.getTags())) {
                operation.setTags(new ArrayList<>(tagsStr));
            } else {
                Set<String> operationTagsSet = new HashSet<>(operation.getTags());
                operationTagsSet.addAll(tagsStr);
                operation.getTags().clear();
                operation.getTags().addAll(operationTagsSet);
            }
        }

        if (isAutoTagClasses(operation)) {

            if (javadocProvider.isPresent()) {
                String description = javadocProvider.get().getClassJavadoc(handlerMethod.getBeanType());
                if (StringUtils.isNotBlank(description)) {
                    Tag tag = new Tag();

                    // 自定义部分 修改使用java注释当tag名
                    List<String> list = IoUtil.readLines(new StringReader(description), new ArrayList<>());
                    // tag.setName(tagAutoName);
                    tag.setName(list.get(0));
                    operation.addTagsItem(list.get(0));

                    tag.setDescription(description);
                    if (openAPI.getTags() == null || !openAPI.getTags().contains(tag)) {
                        openAPI.addTagsItem(tag);
                    }
                }
            } else {
                String tagAutoName = splitCamelCase(handlerMethod.getBeanType().getSimpleName());
                operation.addTagsItem(tagAutoName);
            }
        }

        if (CollUtil.isNotEmpty(tags)) {
            // Existing tags
            List<Tag> openApiTags = openAPI.getTags();
            if (CollUtil.isNotEmpty(openApiTags)) {
                tags.addAll(openApiTags);
            }
            openAPI.setTags(new ArrayList<>(tags));
        }

        // Handle SecurityRequirement at operation level
        io.swagger.v3.oas.annotations.security.SecurityRequirement[] securityRequirements = securityParser
            .getSecurityRequirements(handlerMethod);
        if (securityRequirements != null) {
            if (securityRequirements.length == 0) {
                operation.setSecurity(Collections.emptyList());
            } else {
                securityParser.buildSecurityRequirement(securityRequirements, operation);
            }
        }

        return operation;
    }

    private void buildTagsFromMethod(Method method, Set<Tag> tags, Set<String> tagsStr, Locale locale) {
        // method tags
        Set<Tags> tagsSet = AnnotatedElementUtils.findAllMergedAnnotations(method, Tags.class);
        Set<io.swagger.v3.oas.annotations.tags.Tag> methodTags = tagsSet.stream()
            .flatMap(x -> Stream.of(x.value()))
            .collect(Collectors.toSet());
        methodTags.addAll(AnnotatedElementUtils
            .findAllMergedAnnotations(method, io.swagger.v3.oas.annotations.tags.Tag.class));
        if (CollUtil.isNotEmpty(methodTags)) {
            tagsStr.addAll(toSet(methodTags, tag -> propertyResolverUtils.resolve(tag.name(), locale)));
            List<io.swagger.v3.oas.annotations.tags.Tag> allTags = new ArrayList<>(methodTags);
            addTags(allTags, tags, locale);
        }
    }

    private void addTags(List<io.swagger.v3.oas.annotations.tags.Tag> sourceTags, Set<Tag> tags, Locale locale) {
        Optional<Set<Tag>> optionalTagSet = AnnotationsUtils.getTags(sourceTags
            .toArray(new io.swagger.v3.oas.annotations.tags.Tag[0]), true);
        optionalTagSet.ifPresent(tagsSet -> {
            tagsSet.forEach(tag -> {
                tag.name(propertyResolverUtils.resolve(tag.getName(), locale));
                tag.description(propertyResolverUtils.resolve(tag.getDescription(), locale));
                if (tags.stream().noneMatch(t -> t.getName().equals(tag.getName()))) {
                    tags.add(tag);
                }
            });
        });
    }

    /**
     * 将collection转化为Set集合，但是两者的泛型不同<br>
     * <B>{@code Collection<E>  ------>  Set<T> } </B>
     *
     * @param collection 需要转化的集合
     * @param function   collection中的泛型转化为set泛型的lambda表达式
     * @param <E>        collection中的泛型
     * @param <T>        Set中的泛型
     * @return 转化后的Set
     */
    public static <E, T> Set<T> toSet(Collection<E> collection, Function<E, T> function) {
        if (CollUtil.isEmpty(collection) || function == null) {
            return CollUtil.newHashSet();
        }
        return collection.stream().map(function).filter(Objects::nonNull).collect(Collectors.toSet());
    }

}
