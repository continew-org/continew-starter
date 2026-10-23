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

package top.continew.starter.web.autoconfigure.response;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import top.continew.starter.core.constant.PropertiesConstants;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 全局响应配置属性
 *
 * @author Charles7c
 * @since 2.5.0
 */
@Validated
@ConfigurationProperties(PropertiesConstants.WEB_RESPONSE)
public class GlobalResponseProperties {

    /**
     * 是否开启国际化
     */
    private boolean i18n = false;

    /**
     * 默认成功响应码
     */
    private String defaultSuccessCode = "0";

    /**
     * 默认成功响应信息
     */
    private String defaultSuccessMsg = "ok";

    /**
     * 默认失败响应码
     */
    private String defaultErrorCode = "1";

    /**
     * 默认失败响应信息
     */
    private String defaultErrorMsg = "error";

    /**
     * 默认参数校验失败响应码，未配置时使用默认失败响应码
     */
    private String defaultValidateErrorCode;

    /**
     * 是否打印全局异常处理器捕获的异常日志
     */
    private boolean printExceptionInGlobalAdvice = false;

    /**
     * 是否将原生异常信息填充到响应信息中
     */
    private boolean originExceptionUsingDetailMessage = false;

    /**
     * 默认失败 HTTP 状态码
     */
    @Min(100)
    @Max(599)
    private Integer defaultHttpStatusCodeOnError = 200;

    /**
     * 参数校验失败 HTTP 状态码，未配置时使用默认失败 HTTP 状态码
     */
    @Min(100)
    @Max(599)
    private Integer defaultHttpStatusCodeOnValidationError;

    /**
     * 例外包路径（支持 Ant 风格通配符），该包路径下的控制器不参与响应包装
     */
    private List<String> excludePackages;

    /**
     * 例外 URL（支持 Ant 风格通配符），匹配的请求不参与响应包装
     */
    private List<String> excludeUrls;

    /**
     * 例外返回类型，匹配的返回类型不参与响应包装
     */
    private Set<Class<?>> excludeReturnTypes;

    /**
     * 例外异常类型，匹配的异常由框架继续处理，不进行响应包装
     */
    private Set<Class<?>> excludeExceptionTypes;

    /**
     * 例外异常包路径（支持 Ant 风格通配符），匹配的异常由框架继续处理，不进行响应包装
     */
    private List<String> excludeExceptionPackages;

    /**
     * 异常别名配置，键为异常类型
     */
    private Map<Class<?>, ExceptionAliasConfig> exceptionAliasConfigMap;

    /**
     * 自定义 JSON 消息转换器类名
     */
    private String jsonHttpMessageConverter;

    public boolean isI18n() {
        return i18n;
    }

    public void setI18n(boolean i18n) {
        this.i18n = i18n;
    }

    public String getDefaultSuccessCode() {
        return defaultSuccessCode;
    }

    public void setDefaultSuccessCode(String defaultSuccessCode) {
        this.defaultSuccessCode = defaultSuccessCode;
    }

    public String getDefaultSuccessMsg() {
        return defaultSuccessMsg;
    }

    public void setDefaultSuccessMsg(String defaultSuccessMsg) {
        this.defaultSuccessMsg = defaultSuccessMsg;
    }

    public String getDefaultErrorCode() {
        return defaultErrorCode;
    }

    public void setDefaultErrorCode(String defaultErrorCode) {
        this.defaultErrorCode = defaultErrorCode;
    }

    public String getDefaultErrorMsg() {
        return defaultErrorMsg;
    }

    public void setDefaultErrorMsg(String defaultErrorMsg) {
        this.defaultErrorMsg = defaultErrorMsg;
    }

    public String getDefaultValidateErrorCode() {
        return defaultValidateErrorCode;
    }

    public void setDefaultValidateErrorCode(String defaultValidateErrorCode) {
        this.defaultValidateErrorCode = defaultValidateErrorCode;
    }

    public boolean isPrintExceptionInGlobalAdvice() {
        return printExceptionInGlobalAdvice;
    }

    public void setPrintExceptionInGlobalAdvice(boolean printExceptionInGlobalAdvice) {
        this.printExceptionInGlobalAdvice = printExceptionInGlobalAdvice;
    }

    public boolean isOriginExceptionUsingDetailMessage() {
        return originExceptionUsingDetailMessage;
    }

    public void setOriginExceptionUsingDetailMessage(boolean originExceptionUsingDetailMessage) {
        this.originExceptionUsingDetailMessage = originExceptionUsingDetailMessage;
    }

    public Integer getDefaultHttpStatusCodeOnError() {
        return defaultHttpStatusCodeOnError;
    }

    public void setDefaultHttpStatusCodeOnError(Integer defaultHttpStatusCodeOnError) {
        this.defaultHttpStatusCodeOnError = defaultHttpStatusCodeOnError;
    }

    public Integer getDefaultHttpStatusCodeOnValidationError() {
        return defaultHttpStatusCodeOnValidationError;
    }

    public void setDefaultHttpStatusCodeOnValidationError(
        Integer defaultHttpStatusCodeOnValidationError) {
        this.defaultHttpStatusCodeOnValidationError = defaultHttpStatusCodeOnValidationError;
    }

    public List<String> getExcludePackages() {
        return excludePackages;
    }

    public void setExcludePackages(List<String> excludePackages) {
        this.excludePackages = excludePackages;
    }

    public List<String> getExcludeUrls() {
        return excludeUrls;
    }

    public void setExcludeUrls(List<String> excludeUrls) {
        this.excludeUrls = excludeUrls;
    }

    public Set<Class<?>> getExcludeReturnTypes() {
        return excludeReturnTypes;
    }

    public void setExcludeReturnTypes(Set<Class<?>> excludeReturnTypes) {
        this.excludeReturnTypes = excludeReturnTypes;
    }

    public Set<Class<?>> getExcludeExceptionTypes() {
        return excludeExceptionTypes;
    }

    public void setExcludeExceptionTypes(Set<Class<?>> excludeExceptionTypes) {
        this.excludeExceptionTypes = excludeExceptionTypes;
    }

    public List<String> getExcludeExceptionPackages() {
        return excludeExceptionPackages;
    }

    public void setExcludeExceptionPackages(List<String> excludeExceptionPackages) {
        this.excludeExceptionPackages = excludeExceptionPackages;
    }

    public Map<Class<?>, ExceptionAliasConfig> getExceptionAliasConfigMap() {
        return exceptionAliasConfigMap;
    }

    public void setExceptionAliasConfigMap(
        Map<Class<?>, ExceptionAliasConfig> exceptionAliasConfigMap) {
        this.exceptionAliasConfigMap = exceptionAliasConfigMap;
    }

    public String getJsonHttpMessageConverter() {
        return jsonHttpMessageConverter;
    }

    public void setJsonHttpMessageConverter(String jsonHttpMessageConverter) {
        this.jsonHttpMessageConverter = jsonHttpMessageConverter;
    }
}
