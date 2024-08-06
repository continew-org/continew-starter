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

package top.continew.starter.auth.satoken.autoconfigure;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.extra.spring.SpringUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.continew.starter.auth.satoken.autoconfigure.dao.SaTokenDaoConfiguration;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;

/**
 * Sa-Token 自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(SaTokenExtensionProperties.class)
@ConditionalOnProperty(prefix = "sa-token.extension", name = PropertiesConstants.ENABLED, havingValue = "true")
@PropertySource(value = "classpath:default-auth-satoken.yml", factory = GeneralPropertySourceFactory.class)
public class SaTokenAutoConfiguration implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(SaTokenAutoConfiguration.class);
    private final SaTokenExtensionProperties properties;

    public SaTokenAutoConfiguration(SaTokenExtensionProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(SpringUtil.getBean(SaInterceptor.class)).addPathPatterns(StringConstants.PATH_PATTERN);
    }

    /**
     * SaToken 拦截器配置
     */
    @Bean
    @ConditionalOnMissingBean
    public SaInterceptor saInterceptor() {
        return new SaInterceptor(handle -> SaRouter.match(StringConstants.PATH_PATTERN)
            .notMatch(properties.getSecurity().getExcludes())
            .check(r -> StpUtil.checkLogin()));
    }

    /**
     * 持久层配置
     */
    @Configuration
    @Import({SaTokenDaoConfiguration.Default.class, SaTokenDaoConfiguration.Redis.class,
        SaTokenDaoConfiguration.Custom.class})
    protected static class SaTokenDaoAutoConfiguration {
    }

    /**
     * 整合 JWT（简单模式）
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "sa-token.extension", name = "enableJwt", havingValue = "true")
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'SaToken' completed initialization.");
    }
}
