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

package top.charles7c.continew.starter.auth.satoken.autoconfigure;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpLogic;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.util.ReflectUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import top.charles7c.continew.starter.core.constant.StringConstants;
import top.charles7c.continew.starter.core.handler.GeneralPropertySourceFactory;

/**
 * Sa-Token 自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Slf4j
@AutoConfiguration
@RequiredArgsConstructor
@ComponentScan("top.charles7c.continew.starter.auth.satoken.exception")
@EnableConfigurationProperties(SaTokenExtensionProperties.class)
@ConditionalOnProperty(prefix = "sa-token.extension", name = "enabled", havingValue = "true")
@PropertySource(value = "classpath:default-auth-satoken.yml", factory = GeneralPropertySourceFactory.class)
public class SaTokenAutoConfiguration implements WebMvcConfigurer {

    private final SaTokenExtensionProperties properties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
            .addPathPatterns(StringConstants.PATH_PATTERN)
            .excludePathPatterns(properties.getSecurity().getExcludes());
    }

    /**
     * 权限认证实现类
     */
    @Bean
    @ConditionalOnMissingBean
    public StpInterface stpInterface() {
        return ReflectUtil.newInstance(properties.getPermissionImpl());
    }

    /**
     * 自定义持久层配置
     */
    @Configuration
    @Import({SaTokenDaoConfiguration.Redis.class, SaTokenDaoConfiguration.Custom.class})
    protected static class SaTokenDaoAutoConfiguration {
    }

    /**
     * 整合 JWT（简单模式）
     */
    @Bean
    public StpLogic stpLogic() {
        return new StpLogicJwtForSimple();
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'SaToken' completed initialization.");
    }
}
