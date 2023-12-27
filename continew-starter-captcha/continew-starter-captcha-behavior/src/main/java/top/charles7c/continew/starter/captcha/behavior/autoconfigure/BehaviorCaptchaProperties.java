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

package top.charles7c.continew.starter.captcha.behavior.autoconfigure;

import com.anji.captcha.model.common.CaptchaTypeEnum;
import com.anji.captcha.service.CaptchaCacheService;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.charles7c.continew.starter.captcha.behavior.enums.StorageType;

import java.awt.*;

/**
 * 行为验证码配置属性
 *
 * @author Bull-BCLS
 * @since 1.1.0
 */
@Data
@ConfigurationProperties(prefix = "continew-starter.captcha.behavior")
public class BehaviorCaptchaProperties {

    /**
     * 是否启用行为验证码
     */
    private boolean enabled = false;

    /**
     * 是否开启 AES 坐标加密（默认：true）
     */
    private Boolean enableAes = true;

    /**
     * 验证码类型（默认：滑动验证码）
     */
    private CaptchaTypeEnum type = CaptchaTypeEnum.BLOCKPUZZLE;

    /**
     * 缓存类型（默认：LOCAL 内存）
     */
    private StorageType cacheType = StorageType.LOCAL;

    /**
     * 自定义缓存类型（当 cacheType 为 CUSTOM 时必填）
     */
    private Class<? extends CaptchaCacheService> cacheImpl;

    /**
     * 滑动拼图底图路径（为空则使用默认底图）（路径下需要有两个文件夹，分别为 original（存放底图）slidingBlock（存放滑块））
     */
    private String jigsawBaseMapPath;

    /**
     * 校验滑动拼图允许误差偏移量（默认：5像素）
     */
    private String slipOffset = "5";

    /**
     * 点选文字底图路径（为空则使用默认底图）
     */
    private String picClickBaseMapPath;

    /**
     * 点选文字验证码的文字字体（默认：文泉驿正黑）
     */
    private String fontType = "WenQuanZhengHei.ttf";

    /**
     * 历史数据清除开关（0：关闭；1：开启）
     */
    private Integer historyDataClearEnable = 0;

    /**
     * 一分钟内接口请求次数限制开关（0：关闭；1：开启）
     */
    private Integer reqFrequencyLimitEnable = 0;

    /**
     * 一分钟内验证码最多失败次数限制（默认：5次）
     */
    private int reqGetLockLimit = 5;

    /**
     * 一分钟内验证码最多失败次数限制达标后锁定时间（默认：300秒）
     */
    private int reqGetLockSeconds = 300;

    /**
     * 获取验证码接口一分钟内请求次数限制（默认：100次）
     */
    private int reqGetMinuteLimit = 100;

    /**
     * 校验检验码接口一分内请求次数限制（默认：100次）
     */
    private int reqCheckMinuteLimit = 100;

    /**
     * 二次校验检验码接口一分钟内请求次数限制（默认：100次）
     */
    private int reqVerifyMinuteLimit = 100;

    /**
     * local缓存的阈值（默认：1000个）
     */
    private String cacheNumber = "1000";

    /**
     * 定时清理过期local缓存（默认：180秒）
     */
    private String timingClear = "180";

    /**
     * 右下角水印文字
     */
    private String waterMark = "我的水印";

    /**
     * 右下角水印字体（默认：文泉驿正黑）
     */
    private String waterFont = "WenQuanZhengHei.ttf";

    /**
     * 滑块干扰项（默认：0）
     */
    private String interferenceOptions = "0";

    /**
     * 点选字体样式（默认：BOLD）
     */
    private int fontStyle = Font.BOLD;

    /**
     * 点选字体大小（默认：25）
     */
    private int fontSize = 25;
}
