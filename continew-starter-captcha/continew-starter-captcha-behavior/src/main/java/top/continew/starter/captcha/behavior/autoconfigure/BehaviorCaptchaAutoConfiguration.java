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

package top.continew.starter.captcha.behavior.autoconfigure;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.anji.captcha.model.common.Const;
import com.anji.captcha.service.CaptchaService;
import com.anji.captcha.service.impl.CaptchaServiceFactory;
import com.anji.captcha.util.ImageUtils;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import top.continew.starter.core.constant.PropertiesConstants;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * 行为验证码自动配置
 *
 * @author Bull-BCLS
 * @since 1.1.0
 */
@AutoConfiguration
@EnableConfigurationProperties(BehaviorCaptchaProperties.class)
@ConditionalOnProperty(prefix = PropertiesConstants.CAPTCHA_BEHAVIOR, name = PropertiesConstants.ENABLED, havingValue = "true", matchIfMissing = true)
public class BehaviorCaptchaAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(BehaviorCaptchaAutoConfiguration.class);
    private final BehaviorCaptchaProperties properties;

    public BehaviorCaptchaAutoConfiguration(BehaviorCaptchaProperties properties) {
        this.properties = properties;
    }

    /**
     * 行为验证码服务接口
     */
    @Bean
    @DependsOn("captchaCacheService")
    @ConditionalOnMissingBean
    public CaptchaService captchaService() {
        Properties config = new Properties();
        config.put(Const.CAPTCHA_CACHETYPE, properties.getCacheType().name().toLowerCase());
        config.put(Const.CAPTCHA_WATER_MARK, properties.getWaterMark());
        config.put(Const.CAPTCHA_FONT_TYPE, properties.getFontType());
        config.put(Const.CAPTCHA_TYPE, properties.getType().getCodeValue());
        config.put(Const.CAPTCHA_INTERFERENCE_OPTIONS, properties.getInterferenceOptions());
        config.put(Const.ORIGINAL_PATH_JIGSAW, CharSequenceUtil.emptyIfNull(properties.getJigsawBaseMapPath()));
        config.put(Const.ORIGINAL_PATH_PIC_CLICK, CharSequenceUtil.emptyIfNull(properties.getPicClickBaseMapPath()));
        config.put(Const.CAPTCHA_SLIP_OFFSET, properties.getSlipOffset());
        config.put(Const.CAPTCHA_AES_STATUS, String.valueOf(properties.getEnableAes()));
        config.put(Const.CAPTCHA_WATER_FONT, properties.getWaterFont());
        config.put(Const.CAPTCHA_CACAHE_MAX_NUMBER, properties.getCacheNumber());
        config.put(Const.CAPTCHA_TIMING_CLEAR_SECOND, properties.getTimingClear());
        config.put(Const.HISTORY_DATA_CLEAR_ENABLE, properties.getHistoryDataClearEnable());
        config.put(Const.REQ_FREQUENCY_LIMIT_ENABLE, properties.getReqFrequencyLimitEnable());
        config.put(Const.REQ_GET_LOCK_LIMIT, properties.getReqGetLockLimit());
        config.put(Const.REQ_GET_LOCK_SECONDS, properties.getReqGetLockSeconds());
        config.put(Const.REQ_GET_MINUTE_LIMIT, properties.getReqGetMinuteLimit());
        config.put(Const.REQ_CHECK_MINUTE_LIMIT, properties.getReqCheckMinuteLimit());
        config.put(Const.REQ_VALIDATE_MINUTE_LIMIT, properties.getReqVerifyMinuteLimit());
        config.put(Const.CAPTCHA_FONT_SIZE, properties.getFontSize());
        config.put(Const.CAPTCHA_FONT_STYLE, properties.getFontStyle());
        config.put(Const.CAPTCHA_WORD_COUNT, 4);
        if (CharSequenceUtil.startWith(properties.getJigsawBaseMapPath(), "classpath:") || CharSequenceUtil
            .startWith(properties.getPicClickBaseMapPath(), "classpath:")) {
            // 自定义 resources 目录下初始化底图
            config.put(Const.CAPTCHA_INIT_ORIGINAL, true);
            initializeBaseMap(properties.getJigsawBaseMapPath(), properties.getPicClickBaseMapPath());
        }
        return CaptchaServiceFactory.getInstance(config);
    }

    /**
     * 初始化 行为/点选 验证码底图
     *
     * @param jigsaw   行为验证码底图路径
     * @param picClick 点选验证码底图路径
     */
    private static void initializeBaseMap(String jigsaw, String picClick) {
        ImageUtils
            .cacheBootImage(getResourcesImagesFile(jigsaw + "/original/*.png"), getResourcesImagesFile(jigsaw + "/slidingBlock/*.png"), getResourcesImagesFile(picClick + "/*.png"));
    }

    /**
     * 获取图片
     *
     * @param path 图片路径
     * @return key：图片文件名称；value：图片
     */
    private static Map<String, String> getResourcesImagesFile(String path) {
        Map<String, String> imgMap = new HashMap<>();
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            Resource[] resources = resolver.getResources(path);
            for (Resource resource : resources) {
                String imageName = resource.getFilename();
                byte[] imageValue = FileUtil.readBytes(resource.getFile());
                imgMap.put(imageName, Base64.encode(imageValue));
            }
        } catch (Exception e) {
            log.error("读取路径为 [{}] 下的图片文件失败", path, e);
        }
        return imgMap;
    }

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'Captcha-Behavior' completed initialization.");
    }
}
