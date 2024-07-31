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

package top.continew.starter.apidoc.autoconfigure;

import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ClassUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.databind.type.SimpleType;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.configuration.SpringDocConfiguration;
import org.springdoc.core.customizers.*;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.JavadocProvider;
import org.springdoc.core.service.OpenAPIService;
import org.springdoc.core.service.SecurityService;
import org.springdoc.core.utils.PropertyResolverUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.continew.starter.apidoc.handler.OpenApiHandler;
import top.continew.starter.apidoc.util.EnumTypeUtils;
import top.continew.starter.core.autoconfigure.project.ProjectProperties;
import top.continew.starter.core.enums.BaseEnum;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * API 文档自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@EnableWebMvc
@AutoConfiguration(before = SpringDocConfiguration.class)
@EnableConfigurationProperties(SpringDocExtensionProperties.class)
@PropertySource(value = "classpath:default-api-doc.yml", factory = GeneralPropertySourceFactory.class)
public class SpringDocAutoConfiguration implements WebMvcConfigurer {
    private static final Logger log = LoggerFactory.getLogger(SpringDocAutoConfiguration.class);

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/favicon.ico").addResourceLocations("classpath:/");
        registry.addResourceHandler("/doc.html").addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
            .addResourceLocations("classpath:/META-INF/resources/webjars/")
            .setCacheControl(CacheControl.maxAge(5, TimeUnit.HOURS).cachePublic());
    }

    /**
     * Open API 配置
     */
    @Bean
    @ConditionalOnMissingBean
    public OpenAPI openApi(ProjectProperties projectProperties, SpringDocExtensionProperties properties) {
        Info info = new Info().title("%s %s".formatted(projectProperties.getName(), "API 文档"))
            .version(projectProperties.getVersion())
            .description(projectProperties.getDescription());
        ProjectProperties.Contact contact = projectProperties.getContact();
        if (null != contact) {
            info.contact(new Contact().name(contact.getName()).email(contact.getEmail()).url(contact.getUrl()));
        }
        ProjectProperties.License license = projectProperties.getLicense();
        if (null != license) {
            info.license(new License().name(license.getName()).url(license.getUrl()));
        }
        OpenAPI openApi = new OpenAPI();
        openApi.info(info);
        Components components = properties.getComponents();
        if (null != components) {
            openApi.components(components);
            // 鉴权配置
            Map<String, SecurityScheme> securitySchemeMap = components.getSecuritySchemes();
            if (MapUtil.isNotEmpty(securitySchemeMap)) {
                SecurityRequirement securityRequirement = new SecurityRequirement();
                List<String> list = securitySchemeMap.values().stream().map(SecurityScheme::getName).toList();
                list.forEach(securityRequirement::addList);
                openApi.addSecurityItem(securityRequirement);
            }
        }
        return openApi;
    }

    /**
     * 全局自定义配置（全局添加鉴权参数）
     */
    @Bean
    public GlobalOpenApiCustomizer globalOpenApiCustomizer(SpringDocExtensionProperties properties) {
        return openApi -> {
            if (null != openApi.getPaths()) {
                openApi.getPaths().forEach((s, pathItem) -> {
                    // 为所有接口添加鉴权
                    Components components = properties.getComponents();
                    if (null != components && MapUtil.isNotEmpty(components.getSecuritySchemes())) {
                        Map<String, SecurityScheme> securitySchemeMap = components.getSecuritySchemes();
                        pathItem.readOperations().forEach(operation -> {
                            SecurityRequirement securityRequirement = new SecurityRequirement();
                            List<String> list = securitySchemeMap.values()
                                .stream()
                                .map(SecurityScheme::getName)
                                .toList();
                            list.forEach(securityRequirement::addList);
                            operation.addSecurityItem(securityRequirement);
                        });
                    }
                });
            }
        };
    }

    /**
     * 自定义 openapi 处理器
     */
    @Bean
    public OpenAPIService openApiBuilder(Optional<OpenAPI> openAPI,
                                         SecurityService securityParser,
                                         SpringDocConfigProperties springDocConfigProperties,
                                         PropertyResolverUtils propertyResolverUtils,
                                         Optional<List<OpenApiBuilderCustomizer>> openApiBuilderCustomisers,
                                         Optional<List<ServerBaseUrlCustomizer>> serverBaseUrlCustomisers,
                                         Optional<JavadocProvider> javadocProvider) {
        return new OpenApiHandler(openAPI, securityParser, springDocConfigProperties, propertyResolverUtils, openApiBuilderCustomisers, serverBaseUrlCustomisers, javadocProvider);
    }

    /**
     * 展示 枚举类型和值
     *
     * @return
     */
    @Bean
    public ParameterCustomizer customParameterCustomizer() {
        return (parameterModel, methodParameter) -> {
            // 判断方法参数类型是否为 IBaseEnum 的子类型
            if (ClassUtil.isAssignable(BaseEnum.class, methodParameter.getParameterType())) {
                String description = parameterModel.getDescription();
                // TODO 会重复调用，有什么优雅的判读方式吗？
                if (StrUtil.contains(description, "color:red")) {
                    return parameterModel;
                }
                Schema schema = parameterModel.getSchema();

                // 获取方法参数类型的所有枚举常量
                BaseEnum[] enumConstants = (BaseEnum[])methodParameter.getParameterType().getEnumConstants();
                List<String> list = new ArrayList<>();
                Map<Object, String> descMap = new HashMap<>();

                // 遍历枚举常量，获取其值和描述
                for (BaseEnum constant : enumConstants) {
                    list.add(constant.getValue().toString());
                    descMap.put(constant.getValue(), constant.getDescription());
                }

                // 枚举值类型
                String enumValueType = EnumTypeUtils.getEnumValueTypeAsString(methodParameter.getParameterType());
                schema.setType(enumValueType);
                switch (enumValueType) {
                    case "integer" -> schema.setFormat("int32");
                    case "long" -> schema.setFormat("int64");
                    case "number" -> schema.setFormat("double");
                }

                // 设置枚举值列表和描述
                schema.setEnum(list);
                parameterModel.setDescription(description + "<span style='color:red'>" + descMap + "</span>");
            }
            return parameterModel;
        };
    }

    /**
     * 展示 枚举类型和值
     *
     * @return
     */
    @Bean
    public PropertyCustomizer customPropertyCustomizer() {
        return (schema, type) -> {
            Class<?> rawClass;
            // 获取原始类的类型
            if (type.getType() instanceof SimpleType) {
                rawClass = ((SimpleType)type.getType()).getRawClass();
            } else if (type.getType() instanceof CollectionType) {
                rawClass = ((CollectionType)type.getType()).getContentType().getRawClass();
            } else {
                rawClass = Object.class;
            }

            // 检查原始类是否实现了 IBaseEnum 接口
            if (ClassUtil.isAssignable(BaseEnum.class, rawClass)) {
                BaseEnum[] enumConstants = (BaseEnum[])rawClass.getEnumConstants();
                List<String> list = new ArrayList<>();
                Map<Object, String> descMap = new HashMap<>();
                // 遍历枚举常量，获取其值和描述
                for (BaseEnum constant : enumConstants) {
                    list.add(constant.getValue().toString());
                    descMap.put(constant.getValue(), constant.getDescription());
                }
                // 获取泛型类型
                String enumValueType = EnumTypeUtils.getEnumValueTypeAsString(rawClass);
                schema.setType(enumValueType);
                // 根据枚举值类型设置 schema 的格式
                switch (enumValueType) {
                    case "integer" -> schema.setFormat("int32");
                    case "long" -> schema.setFormat("int64");
                    case "number" -> schema.setFormat("double");
                }

                // 设置枚举值列表和描述
                schema.setEnum(list);
                schema.setDescription(schema.getDescription() + "<span style='color:red'>" + descMap + "</span>");
                return schema;
            }
            return schema;
        };
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'ApiDoc' completed initialization.");
    }
}
