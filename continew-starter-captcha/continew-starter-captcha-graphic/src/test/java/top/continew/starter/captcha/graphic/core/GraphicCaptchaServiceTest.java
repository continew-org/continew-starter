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

package top.continew.starter.captcha.graphic.core;

import static org.assertj.core.api.Assertions.assertThat;

import com.wf.captcha.ArithmeticCaptcha;
import com.wf.captcha.GifCaptcha;
import com.wf.captcha.SpecCaptcha;
import com.wf.captcha.base.Captcha;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.captcha.graphic.autoconfigure.GraphicCaptchaProperties;
import top.continew.starter.captcha.graphic.enums.GraphicCaptchaType;

/**
 * {@link GraphicCaptchaService} 图形验证码服务单元测试：按配置真实构造验证码实例。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class GraphicCaptchaServiceTest {

    /**
     * 默认配置应构造 SPEC 类型验证码并传递长度。
     */
    @Test
    @DisplayName("默认配置构造 SPEC 验证码")
    void shouldBuildSpecCaptchaByDefault() {
        GraphicCaptchaProperties properties = new GraphicCaptchaProperties();
        properties.setLength(6);
        GraphicCaptchaService service = new GraphicCaptchaService(properties);

        Captcha captcha = service.getCaptcha();

        assertThat(captcha).isInstanceOf(SpecCaptcha.class);
        assertThat(captcha.getLen()).isEqualTo(6);
    }

    /**
     * ARITHMETIC 类型应构造算术验证码，内容为可计算表达式。
     */
    @Test
    @DisplayName("ARITHMETIC 构造算术验证码")
    void shouldBuildArithmeticCaptcha() {
        GraphicCaptchaProperties properties = new GraphicCaptchaProperties();
        properties.setType(GraphicCaptchaType.ARITHMETIC);
        GraphicCaptchaService service = new GraphicCaptchaService(properties);

        Captcha captcha = service.getCaptcha();

        assertThat(captcha).isInstanceOf(ArithmeticCaptcha.class);
        assertThat(captcha.text()).isNotBlank();
    }

    /**
     * GIF 类型应构造 GifCaptcha。
     */
    @Test
    @DisplayName("GIF 构造动图验证码")
    void shouldBuildGifCaptcha() {
        GraphicCaptchaProperties properties = new GraphicCaptchaProperties();
        properties.setType(GraphicCaptchaType.GIF);
        GraphicCaptchaService service = new GraphicCaptchaService(properties);

        assertThat(service.getCaptcha()).isInstanceOf(GifCaptcha.class);
    }

    /**
     * 配置字体名时应应用指定字体（名称与字号一致）。
     */
    @Test
    @DisplayName("fontName 生效应用指定字体")
    void shouldApplyConfiguredFont() {
        GraphicCaptchaProperties properties = new GraphicCaptchaProperties();
        properties.setFontName("Serif");
        properties.setFontSize(24);
        GraphicCaptchaService service = new GraphicCaptchaService(properties);

        Captcha captcha = service.getCaptcha();

        assertThat(captcha.getFont().getName()).isEqualTo("Serif");
        assertThat(captcha.getFont().getSize()).isEqualTo(24);
    }

    /**
     * fontName 为空时应保持验证码默认字体（不抛异常、宽高按配置生效）。
     */
    @Test
    @DisplayName("fontName 空保持默认字体")
    void shouldKeepDefaultFontWhenNameBlank() {
        GraphicCaptchaProperties properties = new GraphicCaptchaProperties();
        properties.setWidth(200);
        properties.setHeight(80);
        GraphicCaptchaService service = new GraphicCaptchaService(properties);

        Captcha captcha = service.getCaptcha();

        assertThat(captcha.getWidth()).isEqualTo(200);
        assertThat(captcha.getHeight()).isEqualTo(80);
    }

    /**
     * 重复构造应各自生成实例且默认字体不为 null（Font.PLAIN 兜底链路）。
     */
    @Test
    @DisplayName("两次构造生成独立实例")
    void shouldBuildIndependentInstances() {
        GraphicCaptchaService service = new GraphicCaptchaService(new GraphicCaptchaProperties());

        Captcha first = service.getCaptcha();
        Captcha second = service.getCaptcha();

        assertThat(first).isNotSameAs(second);
        assertThat(first.getFont()).isNotNull();
    }
}
