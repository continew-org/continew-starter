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

import com.anji.captcha.model.common.CaptchaTypeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.captcha.behavior.enums.StorageType;
import top.continew.starter.core.constant.PropertiesConstants;

import java.awt.*;

/**
 * 行为验证码配置属性
 *
 * @author Bull-BCLS
 * @since 1.1.0
 */
@ConfigurationProperties(PropertiesConstants.CAPTCHA_BEHAVIOR)
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
     * 缓存类型
     */
    private StorageType cacheType = StorageType.DEFAULT;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getEnableAes() {
        return enableAes;
    }

    public void setEnableAes(Boolean enableAes) {
        this.enableAes = enableAes;
    }

    public CaptchaTypeEnum getType() {
        return type;
    }

    public void setType(CaptchaTypeEnum type) {
        this.type = type;
    }

    public StorageType getCacheType() {
        return cacheType;
    }

    public void setCacheType(StorageType cacheType) {
        this.cacheType = cacheType;
    }

    public String getJigsawBaseMapPath() {
        return jigsawBaseMapPath;
    }

    public void setJigsawBaseMapPath(String jigsawBaseMapPath) {
        this.jigsawBaseMapPath = jigsawBaseMapPath;
    }

    public String getSlipOffset() {
        return slipOffset;
    }

    public void setSlipOffset(String slipOffset) {
        this.slipOffset = slipOffset;
    }

    public String getPicClickBaseMapPath() {
        return picClickBaseMapPath;
    }

    public void setPicClickBaseMapPath(String picClickBaseMapPath) {
        this.picClickBaseMapPath = picClickBaseMapPath;
    }

    public String getFontType() {
        return fontType;
    }

    public void setFontType(String fontType) {
        this.fontType = fontType;
    }

    public Integer getHistoryDataClearEnable() {
        return historyDataClearEnable;
    }

    public void setHistoryDataClearEnable(Integer historyDataClearEnable) {
        this.historyDataClearEnable = historyDataClearEnable;
    }

    public Integer getReqFrequencyLimitEnable() {
        return reqFrequencyLimitEnable;
    }

    public void setReqFrequencyLimitEnable(Integer reqFrequencyLimitEnable) {
        this.reqFrequencyLimitEnable = reqFrequencyLimitEnable;
    }

    public int getReqGetLockLimit() {
        return reqGetLockLimit;
    }

    public void setReqGetLockLimit(int reqGetLockLimit) {
        this.reqGetLockLimit = reqGetLockLimit;
    }

    public int getReqGetLockSeconds() {
        return reqGetLockSeconds;
    }

    public void setReqGetLockSeconds(int reqGetLockSeconds) {
        this.reqGetLockSeconds = reqGetLockSeconds;
    }

    public int getReqGetMinuteLimit() {
        return reqGetMinuteLimit;
    }

    public void setReqGetMinuteLimit(int reqGetMinuteLimit) {
        this.reqGetMinuteLimit = reqGetMinuteLimit;
    }

    public int getReqCheckMinuteLimit() {
        return reqCheckMinuteLimit;
    }

    public void setReqCheckMinuteLimit(int reqCheckMinuteLimit) {
        this.reqCheckMinuteLimit = reqCheckMinuteLimit;
    }

    public int getReqVerifyMinuteLimit() {
        return reqVerifyMinuteLimit;
    }

    public void setReqVerifyMinuteLimit(int reqVerifyMinuteLimit) {
        this.reqVerifyMinuteLimit = reqVerifyMinuteLimit;
    }

    public String getCacheNumber() {
        return cacheNumber;
    }

    public void setCacheNumber(String cacheNumber) {
        this.cacheNumber = cacheNumber;
    }

    public String getTimingClear() {
        return timingClear;
    }

    public void setTimingClear(String timingClear) {
        this.timingClear = timingClear;
    }

    public String getWaterMark() {
        return waterMark;
    }

    public void setWaterMark(String waterMark) {
        this.waterMark = waterMark;
    }

    public String getWaterFont() {
        return waterFont;
    }

    public void setWaterFont(String waterFont) {
        this.waterFont = waterFont;
    }

    public String getInterferenceOptions() {
        return interferenceOptions;
    }

    public void setInterferenceOptions(String interferenceOptions) {
        this.interferenceOptions = interferenceOptions;
    }

    public int getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(int fontStyle) {
        this.fontStyle = fontStyle;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }
}
