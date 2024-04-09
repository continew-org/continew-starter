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

package top.continew.starter.captcha.graphic.enums;

import com.wf.captcha.*;
import com.wf.captcha.base.Captcha;

/**
 * 图形验证码类型枚举
 *
 * @author Charles7c
 * @since 1.0.0
 */
public enum GraphicCaptchaType {

    /**
     * 算术
     */
    ARITHMETIC(ArithmeticCaptcha.class),

    /**
     * 中文
     */
    CHINESE(ChineseCaptcha.class),

    /**
     * 中文闪图
     */
    CHINESE_GIF(ChineseGifCaptcha.class),

    /**
     * 闪图
     */
    GIF(GifCaptcha.class),

    /**
     * 特殊类型
     */
    SPEC(SpecCaptcha.class),;

    /**
     * 验证码实现
     */
    private final Class<? extends Captcha> captchaImpl;

    GraphicCaptchaType(Class<? extends Captcha> captchaImpl) {
        this.captchaImpl = captchaImpl;
    }

    public Class<? extends Captcha> getCaptchaImpl() {
        return captchaImpl;
    }
}