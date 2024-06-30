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

import cn.hutool.core.convert.Convert;
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
    private boolean enabled = true;

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
    private String historyDataClearEnable = "0";

    /**
     * 一分钟内接口请求次数限制开关（0：关闭；1：开启）
     */
    private String reqFrequencyLimitEnable = "0";

    /**
     * 一分钟内验证码最多失败次数限制（默认：5次）
     */
    private String reqGetLockLimit = "5";

    /**
     * 一分钟内验证码最多失败次数限制达标后锁定时间（默认：300秒）
     */
    private String reqGetLockSeconds = "300";

    /**
     * 获取验证码接口一分钟内请求次数限制（默认：100次）
     */
    private String reqGetMinuteLimit = "100";

    /**
     * 校验检验码接口一分内请求次数限制（默认：100次）
     */
    private String reqCheckMinuteLimit = "100";

    /**
     * 二次校验检验码接口一分钟内请求次数限制（默认：100次）
     */
    private String reqVerifyMinuteLimit = "100";

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
    private String fontStyle = Convert.toStr(Font.BOLD);

    /**
     * 点选字体大小（默认：25）
     */
    private String fontSize = "25";

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

    public String getHistoryDataClearEnable() {
        return historyDataClearEnable;
    }

    public void setHistoryDataClearEnable(String historyDataClearEnable) {
        this.historyDataClearEnable = historyDataClearEnable;
    }

    public String getReqFrequencyLimitEnable() {
        return reqFrequencyLimitEnable;
    }

    public void setReqFrequencyLimitEnable(String reqFrequencyLimitEnable) {
        this.reqFrequencyLimitEnable = reqFrequencyLimitEnable;
    }

    public String getReqGetLockLimit() {
        return reqGetLockLimit;
    }

    public void setReqGetLockLimit(String reqGetLockLimit) {
        this.reqGetLockLimit = reqGetLockLimit;
    }

    public String getReqGetLockSeconds() {
        return reqGetLockSeconds;
    }

    public void setReqGetLockSeconds(String reqGetLockSeconds) {
        this.reqGetLockSeconds = reqGetLockSeconds;
    }

    public String getReqGetMinuteLimit() {
        return reqGetMinuteLimit;
    }

    public void setReqGetMinuteLimit(String reqGetMinuteLimit) {
        this.reqGetMinuteLimit = reqGetMinuteLimit;
    }

    public String getReqCheckMinuteLimit() {
        return reqCheckMinuteLimit;
    }

    public void setReqCheckMinuteLimit(String reqCheckMinuteLimit) {
        this.reqCheckMinuteLimit = reqCheckMinuteLimit;
    }

    public String getReqVerifyMinuteLimit() {
        return reqVerifyMinuteLimit;
    }

    public void setReqVerifyMinuteLimit(String reqVerifyMinuteLimit) {
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

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
        this.fontSize = fontSize;
    }
}
