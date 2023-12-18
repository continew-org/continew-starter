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
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.charles7c.continew.starter.captcha.graphic.enums.GraphicCaptchaTypeEnum;

import java.awt.*;

/**
 * 图形验证码配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Data
@ConfigurationProperties(prefix = "continew-starter.captcha.graphic")
public class GraphicCaptchaProperties {

    /**
     * 是否启用图形验证码
     */
    private boolean enabled = false;

    /**
     * 类型
     */
    private GraphicCaptchaTypeEnum type;

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

    /**
     * 获取图形验证码
     *
     * @return 图形验证码
     */
    public Captcha getCaptcha() {
        if (this.enabled) {
            Captcha captcha = ReflectUtil.newInstance(this.type.getCaptchaImpl(), this.width, this.height);
            captcha.setLen(this.length);
            if (StrUtil.isNotBlank(this.fontName)) {
                captcha.setFont(new Font(fontName, Font.PLAIN, this.fontSize));
            }
            return captcha;
        }
        return null;
    }
}
