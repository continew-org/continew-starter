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

package top.charles7c.continew.starter.captcha.graphic.autoconfigure;

import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import com.wf.captcha.base.Captcha;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

import java.awt.*;

/**
 * 图形验证码自动配置
 *
 * @author Charles7c
 * @since 1.0.0
 */
@AutoConfiguration
@EnableConfigurationProperties(GraphicCaptchaProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.CAPTCHA_GRAPHIC, name = PropertiesConstants.ENABLED, havingValue = "true")
public class GraphicCaptchaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(GraphicCaptchaAutoConfiguration.class);

    /**
     * 验证码配置
     */
    @Bean
    @ConditionalOnMissingBean
    public Captcha captcha(GraphicCaptchaProperties properties) {
        Captcha captcha = ReflectUtil.newInstance(properties.getType().getCaptchaImpl(), properties
            .getWidth(), properties.getHeight());
        captcha.setLen(properties.getLength());
        if (StrUtil.isNotBlank(properties.getFontName())) {
            captcha.setFont(new Font(properties.getFontName(), Font.PLAIN, properties.getFontSize()));
        }
        return captcha;
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Captcha-Graphic' completed initialization.");
    }
}
