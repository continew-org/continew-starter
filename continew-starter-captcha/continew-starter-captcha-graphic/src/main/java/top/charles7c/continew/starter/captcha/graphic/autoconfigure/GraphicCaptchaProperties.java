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

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.charles7c.continew.starter.captcha.graphic.enums.GraphicCaptchaType;
import top.charles7c.continew.starter.core.constant.PropertiesConstants;

/**
 * 图形验证码配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@ConfigurationProperties(PropertiesConstants.CAPTCHA_GRAPHIC)
public class GraphicCaptchaProperties {

    /**
     * 是否启用图形验证码
     */
    private boolean enabled = false;

    /**
     * 类型
     */
    private GraphicCaptchaType type = GraphicCaptchaType.SPEC;

    /**
     * 内容长度
     */
    private int length = 4;

    /**
     * 宽度
     */
    private int width = 111;

    /**
     * 高度
     */
    private int height = 36;

    /**
     * 字体
     */
    private String fontName;

    /**
     * 字体大小
     */
    private int fontSize = 25;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GraphicCaptchaType getType() {
        return type;
    }

    public void setType(GraphicCaptchaType type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getFontName() {
        return fontName;
    }

    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }
}
