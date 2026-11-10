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

package top.continew.starter.log.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.model.AccessLogProperties;
import top.continew.starter.log.model.LogProperties;

import java.util.List;

/**
 * {@link AccessLogUtils} 访问日志工具类测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class AccessLogUtilsTest {

    /**
     * 未开启参数打印时不输出参数信息
     */
    @Test
    @DisplayName("未开启参数打印返回 null")
    void getParamShouldBeNullWhenPrintDisabled() {
        AccessLogProperties properties = new AccessLogProperties();
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        assertThat(AccessLogUtils.getParam(request, properties)).isNull();
    }

    /**
     * 开启参数打印但参数为空白时返回 null
     */
    @Test
    @DisplayName("参数为空白时返回 null")
    void getParamShouldBeNullWhenParamsBlank() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("   ");
        assertThat(AccessLogUtils.getParam(request, properties)).isNull();
    }

    /**
     * JSON 对象参数应原样序列化输出
     */
    @Test
    @DisplayName("JSON 对象参数原样输出")
    void getParamShouldRoundTripJsonObjectParams() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"page\":1}");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("{\"page\":1}");
    }

    /**
     * 锁定当前行为：JSON 数组参数解析即抛出 JSONException，参数不会输出
     */
    @Test
    @DisplayName("JSON 数组参数抛出 JSONException（锁定当前行为）")
    void getParamShouldThrowForJsonArrayParams() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"id\":1},{\"id\":2}]");
        // 锁定当前行为：数组参数经 JSONUtil.toBean(String, Class) 解析按对象处理而抛异常
        assertThatThrownBy(() -> AccessLogUtils.getParam(request, properties))
            .isInstanceOf(JSONException.class);
    }

    /**
     * 锁定当前行为：非 JSON 参数经 JSONUtil.toJsonStr 原样返回（不加引号）
     */
    @Test
    @DisplayName("非 JSON 参数原样返回（锁定当前行为）")
    void getParamShouldWrapNonJsonParamsAsStringLiteral() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("raw-text");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("raw-text");
    }

    /**
     * 敏感参数过滤：命中的字段值替换为掩码
     */
    @Test
    @DisplayName("敏感字段值替换为掩码")
    void getParamShouldMaskSensitiveFields() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"username\":\"admin\",\"password\":\"p@ss\"}");
        String param = AccessLogUtils.getParam(request, properties);
        assertThat(param).contains("\"password\":\"***\"")
            .contains("\"username\":\"admin\"")
            .doesNotContain("p@ss");
    }

    /**
     * 锁定当前行为：数组参数在脱敏前已因解析抛出 JSONException，掩码不会生效
     */
    @Test
    @DisplayName("数组参数脱敏前抛出 JSONException（锁定当前行为）")
    void getParamShouldThrowBeforeMaskingForArrayParams() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"password\":\"p@ss\"},{\"name\":\"n\"}]");
        assertThatThrownBy(() -> AccessLogUtils.getParam(request, properties))
            .isInstanceOf(JSONException.class);
    }

    /**
     * 超长参数截断：超过阈值的值截断到最大长度并追加后缀
     */
    @Test
    @DisplayName("超过阈值的值截断并追加后缀")
    void getParamShouldTruncateValueBeyondThreshold() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"k\":\"abcdefghijk\",\"s\":\"abc\"}");
        String param = AccessLogUtils.getParam(request, properties);
        assertThat(param).contains("\"k\":\"abcde...\"").contains("\"s\":\"abc\"");
    }

    /**
     * 锁定当前行为：脱敏后仍保留敏感键（值为掩码），未命中的键不受影响
     */
    @Test
    @DisplayName("未命中的键不受脱敏影响（锁定当前行为）")
    void getParamShouldKeepUnmatchedKeysUntouched() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("Password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"password\":\"p@ss\"}");
        // 敏感字段精确匹配且区分大小写，password 未被配置项 Password 命中
        assertThat(AccessLogUtils.getParam(request, properties)).contains("\"password\":\"p@ss\"");
    }

    /**
     * 内置放行路径（错误页）应被排除
     */
    @Test
    @DisplayName("内置错误页路径被排除")
    void exclusionPathShouldExcludeBuiltinErrorPath() {
        assertThat(AccessLogUtils.exclusionPath(new LogProperties(), "/error")).isTrue();
    }

    /**
     * 内置放行路径（actuator）应被排除
     */
    @Test
    @DisplayName("内置 actuator 路径被排除")
    void exclusionPathShouldExcludeBuiltinActuatorPath() {
        assertThat(AccessLogUtils.exclusionPath(new LogProperties(), "/actuator/health")).isTrue();
    }

    /**
     * 普通业务路径不应被排除
     */
    @Test
    @DisplayName("普通业务路径不被排除")
    void exclusionPathShouldKeepBusinessPath() {
        assertThat(AccessLogUtils.exclusionPath(new LogProperties(), "/api/users")).isFalse();
    }

    /**
     * 配置的放行路由应参与排除判断
     */
    @Test
    @DisplayName("配置的放行路由参与排除")
    void exclusionPathShouldExcludeConfiguredPattern() {
        LogProperties properties = new LogProperties();
        properties.setExcludePatterns(List.of("/api/**"));
        assertThat(AccessLogUtils.exclusionPath(properties, "/api/users")).isTrue();
    }
}
