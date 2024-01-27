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

package top.charles7c.continew.starter.core.constant;

/**
 * 配置属性相关常量
 *
 * @author Charles7c
 * @since 1.1.1
 */
public class PropertiesConstants {

    private PropertiesConstants() {
    }

    /**
     * ContiNew Starter
     */
    public static final String CONTINEW_STARTER = "continew-starter";

    /**
     * 启用配置
     */
    public static final String ENABLED = "enabled";

    /**
     * 线程池配置
     */
    public static final String THREAD_POOL = CONTINEW_STARTER + ".thread-pool";

    /**
     * Spring Doc 配置
     */
    public static final String SPRINGDOC = "springdoc";

    /**
     * Spring Doc Swagger UI 配置
     */
    public static final String SPRINGDOC_SWAGGER_UI = SPRINGDOC + ".swagger-ui";

    /**
     * Web 配置
     */
    public static final String WEB = CONTINEW_STARTER + ".web";

    /**
     * 跨域配置
     */
    public static final String CORS = WEB + ".cors";

    /**
     * 日志配置
     */
    public static final String LOG = CONTINEW_STARTER + ".log";

    /**
     * 存储配置
     */
    public static final String STORAGE = CONTINEW_STARTER + ".storage";

    /**
     * 本地存储配置
     */
    public static final String STORAGE_LOCAL = STORAGE + ".local";

    /**
     * 验证码配置
     */
    public static final String CAPTCHA = CONTINEW_STARTER + ".captcha";

    /**
     * 图形验证码配置
     */
    public static final String CAPTCHA_GRAPHIC = CAPTCHA + ".graphic";

    /**
     * 行为验证码配置
     */
    public static final String CAPTCHA_BEHAVIOR = CAPTCHA + ".behavior";
}
