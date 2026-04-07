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

package top.continew.starter.core.constant;

/**
 * 配置属性相关常量
 *
 * @author Charles7c
 * @since 1.1.1
 */
public class PropertiesConstants {

    /**
     * ContiNew Starter
     */
    public static final String CONTINEW_STARTER = "continew-starter";

    /**
     * 启用配置
     */
    public static final String ENABLED = "enabled";

    /**
     * Web 配置
     */
    public static final String WEB = CONTINEW_STARTER + StringConstants.DOT + "web";

    /**
     * Web-跨域配置
     */
    public static final String WEB_CORS = WEB + StringConstants.DOT + "cors";

    /**
     * Web-响应配置
     */
    public static final String WEB_RESPONSE = WEB + StringConstants.DOT + "response";

    /**
     * 认证-JustAuth 配置
     */
    public static final String AUTH_JUSTAUTH = CONTINEW_STARTER + StringConstants.DOT + "justauth";

    /**
     * 认证-OpenAPI 配置
     */
    public static final String AUTH_OPENAPI = CONTINEW_STARTER + StringConstants.DOT + "openapi";

    /**
     * 加密配置
     */
    public static final String ENCRYPT = CONTINEW_STARTER + StringConstants.DOT + "encrypt";

    /**
     * 加密-密码编码器
     */
    public static final String ENCRYPT_PASSWORD_ENCODER = ENCRYPT + StringConstants.DOT + "password-encoder";

    /**
     * 加密-字段加密
     */
    public static final String ENCRYPT_FIELD = ENCRYPT + StringConstants.DOT + "field";

    /**
     * 加密-API 加密
     */
    public static final String ENCRYPT_API = ENCRYPT + StringConstants.DOT + "api";

    /**
     * 安全配置
     */
    public static final String SECURITY = CONTINEW_STARTER + StringConstants.DOT + "security";

    /**
     * 安全-XSS 配置
     */
    public static final String SECURITY_XSS = SECURITY + StringConstants.DOT + "xss";

    /**
     * 安全-敏感词配置
     */
    public static final String SECURITY_SENSITIVE_WORDS = SECURITY + StringConstants.DOT + "sensitive-words";

    /**
     * 限流配置
     */
    public static final String RATE_LIMITER = CONTINEW_STARTER + StringConstants.DOT + "rate-limiter";

    /**
     * 幂等配置
     */
    public static final String IDEMPOTENT = CONTINEW_STARTER + StringConstants.DOT + "idempotent";

    /**
     * 链路追踪配置
     */
    public static final String TRACE = CONTINEW_STARTER + StringConstants.DOT + "trace";

    /**
     * 验证码配置
     */
    public static final String CAPTCHA = CONTINEW_STARTER + StringConstants.DOT + "captcha";

    /**
     * 图形验证码配置
     */
    public static final String CAPTCHA_GRAPHIC = CAPTCHA + StringConstants.DOT + "graphic";

    /**
     * 行为验证码配置
     */
    public static final String CAPTCHA_BEHAVIOR = CAPTCHA + StringConstants.DOT + "behavior";

    /**
     * 消息配置
     */
    public static final String MESSAGING = CONTINEW_STARTER + StringConstants.DOT + "messaging";

    /**
     * WebSocket 配置
     */
    public static final String MESSAGING_WEBSOCKET = MESSAGING + StringConstants.DOT + "websocket";

    /**
     * MQTT 配置
     */
    public static final String MESSAGING_MQTT = MESSAGING + StringConstants.DOT + "mqtt";

    /**
     * 日志配置
     */
    public static final String LOG = CONTINEW_STARTER + StringConstants.DOT + "log";

    /**
     * 存储配置
     */
    public static final String STORAGE = CONTINEW_STARTER + StringConstants.DOT + "storage";

    /**
     * License 配置
     */
    public static final String LICENSE = CONTINEW_STARTER + StringConstants.DOT + "license";

    /**
     * License 生成器配置
     */
    public static final String LICENSE_GENERATOR = LICENSE + StringConstants.DOT + "generator";

    /**
     * License 校验器配置
     */
    public static final String LICENSE_VERIFIER = LICENSE + StringConstants.DOT + "verifier";

    /**
     * CRUD 配置
     */
    public static final String CRUD = CONTINEW_STARTER + StringConstants.DOT + "crud";

    /**
     * 数据权限配置
     */
    public static final String DATA_PERMISSION = CONTINEW_STARTER + StringConstants.DOT + "data-permission";

    /**
     * 租户配置
     */
    public static final String TENANT = CONTINEW_STARTER + StringConstants.DOT + "tenant";

    private PropertiesConstants() {
    }
}
