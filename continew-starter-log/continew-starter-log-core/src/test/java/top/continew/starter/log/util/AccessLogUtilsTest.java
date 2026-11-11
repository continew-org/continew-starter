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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import cn.hutool.json.JSONUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.log.http.RecordableHttpRequest;
import top.continew.starter.log.model.AccessLogProperties;
import top.continew.starter.log.model.LogProperties;

import java.util.List;
import java.util.Map;

/**
 * {@link AccessLogUtils} 访问日志工具类测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
// 用例按场景独立命名，单条失败即可定位，不合并为参数化测试
@SuppressWarnings("java:S5976")
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
     * JSON 数组参数应原样序列化输出
     */
    @Test
    @DisplayName("JSON 数组参数原样输出")
    void getParamShouldRoundTripJsonArrayParams() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"id\":1},{\"id\":2}]");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[{\"id\":1},{\"id\":2}]");
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
     * 数组参数的脱敏应逐项生效
     */
    @Test
    @DisplayName("数组参数逐项脱敏")
    void getParamShouldMaskSensitiveFieldsForArrayParams() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"password\":\"p@ss\"},{\"name\":\"n\"}]");
        String param = AccessLogUtils.getParam(request, properties);
        assertThat(param).isEqualTo("[{\"password\":\"***\"},{\"name\":\"n\"}]");
    }

    /**
     * 数组中的非对象元素不参与脱敏，但必须保留在输出中
     */
    @Test
    @DisplayName("数组中的非对象元素保持原样")
    void getParamShouldKeepNonObjectArrayElements() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"password\":\"p@ss\"},1]");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[{\"password\":\"***\"},1]");
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
     * 数组参数的超长值截断应逐项生效
     */
    @Test
    @DisplayName("数组参数逐项截断超长值")
    void getParamShouldTruncateLongValuesForArrayParams() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("[T]");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"k\":\"abcdefghijk\"}]");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[{\"k\":\"abcde[T]\"}]");
    }

    /**
     * 数组内的字符串元素同样受截断约束
     */
    @Test
    @DisplayName("数组内的超长字符串被截断")
    void getParamShouldTruncateLongStringArrayElements() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[\"abcdefghijk\"]");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("[\"abcde...\"]");
    }

    /**
     * 嵌套数组内的对象同样需要脱敏
     */
    @Test
    @DisplayName("嵌套数组参数逐层脱敏")
    void getParamShouldMaskNestedArrayElements() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[[{\"password\":\"p@ss\"}]]");
        String param = AccessLogUtils.getParam(request, properties);
        assertThat(param).isEqualTo("[[{\"password\":\"***\"}]]");
    }

    /**
     * 嵌套对象内的敏感字段同样需要脱敏
     */
    @Test
    @DisplayName("嵌套对象参数逐层脱敏")
    void getParamShouldMaskNestedObjectFields() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"user\":{\"password\":\"p@ss\"}}");
        String param = AccessLogUtils.getParam(request, properties);
        assertThat(param).isEqualTo("{\"user\":{\"password\":\"***\"}}");
    }

    /**
     * 敏感键的值为对象时整体替换为掩码，不会递归保留子树
     */
    @Test
    @DisplayName("敏感键的值为容器时整体替换")
    void getParamShouldMaskWholeValueWhenSensitiveKeyHoldsContainer() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("token"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"token\":{\"password\":\"p@ss\"}}");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("{\"token\":\"***\"}");
    }

    /**
     * 对象的值为数组时同样逐项脱敏
     */
    @Test
    @DisplayName("对象内数组元素逐项脱敏")
    void getParamShouldMaskSensitiveFieldsInArrayValue() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"users\":[{\"password\":\"p@ss\"}]}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"users\":[{\"password\":\"***\"}]}");
    }

    /**
     * 嵌套超过深度上限时只把越界子树整体掩码，外层结构保持不变
     */
    @Test
    @DisplayName("超过嵌套深度上限时越界子树整体掩码")
    void getParamShouldMaskWholeSubtreeBeyondMaxDepth() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[".repeat(12) + "{\"password\":\"p@ss\"}"
            + "]".repeat(12));
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[".repeat(11) + "\"***\"" + "]".repeat(11));
    }

    /**
     * 截断同样受嵌套深度上限保护，越界子树整体掩码
     */
    @Test
    @DisplayName("截断时越界子树整体掩码")
    void getParamShouldMaskSubtreeBeyondMaxDepthWhileTruncating() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[".repeat(12) + "\"abcdefghijklmno\"" + "]"
            .repeat(12));
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[".repeat(11) + "\"***\"" + "]".repeat(11));
    }

    /**
     * 嵌套恰好未超上限时正常脱敏，超过一层后越界子树整体掩码
     */
    @Test
    @DisplayName("嵌套深度上限的边界行为")
    void getParamShouldMaskByDepthBoundary() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);

        when(request.getParams()).thenReturn("[".repeat(10) + "{\"password\":\"p@ss\"}"
            + "]".repeat(10));
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[".repeat(10) + "{\"password\":\"***\"}" + "]".repeat(10));

        when(request.getParams()).thenReturn("[".repeat(11) + "{\"password\":\"p@ss\"}"
            + "]".repeat(11));
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[".repeat(11) + "\"***\"" + "]".repeat(11));
    }

    /**
     * 嵌套层级超过解析上限时不再解析，直接按原始文本处理，避免解析阶段栈溢出
     */
    @Test
    @DisplayName("嵌套过深时按原始文本输出")
    void getParamShouldSkipParsingWhenNestingExceedsLimit() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        // 保留空白，解析后重新序列化会压缩空格，可区分「跳过解析」与「解析后输出」
        String deepParams = "[".repeat(40) + " 1 " + "]".repeat(40);
        when(request.getParams()).thenReturn(deepParams);
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo(deepParams);
    }

    /**
     * 脱敏开关关闭时，即使配置了敏感字段也不做任何替换
     */
    @Test
    @DisplayName("脱敏开关关闭时敏感字段保持原样")
    void getParamShouldKeepSensitiveFieldsWhenMaskingDisabled() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(false);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"password\":\"p@ss\"}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"password\":\"p@ss\"}");
    }

    /**
     * 嵌套对象内的超长值同样受截断约束
     */
    @Test
    @DisplayName("嵌套对象内的超长值被截断")
    void getParamShouldTruncateNestedObjectValues() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("[T]");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"obj\":{\"k\":\"abcdefghijk\"}}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"obj\":{\"k\":\"abcde[T]\"}}");
    }

    /**
     * 最大保留长度大于阈值时以阈值为上限，截断后不会比原文更长
     */
    @Test
    @DisplayName("最大保留长度超过阈值时按阈值截断")
    void getParamShouldCapMaxLengthByThreshold() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(50);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"k\":\"abcdefghijklmnop\"}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"k\":\"abcdefghij" + properties.getLongParamSuffix() + "\"}");
    }

    /**
     * 截断阈值边界：恰好等于阈值不截断，超过一个字符即按最大保留长度截断
     */
    @Test
    @DisplayName("阈值边界上仅在超过时截断")
    void getParamShouldTruncateOnlyWhenExceedingThreshold() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("[T]");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);

        when(request.getParams()).thenReturn("{\"k\":\"0123456789\"}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"k\":\"0123456789\"}");

        when(request.getParams()).thenReturn("{\"k\":\"01234567890\"}");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("{\"k\":\"01234[T]\"}");
    }

    /**
     * 最大保留长度为 0 时只保留后缀
     */
    @Test
    @DisplayName("最大保留长度为 0 时仅保留后缀")
    void getParamShouldKeepSuffixOnlyWhenMaxLengthIsZero() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(0);
        properties.setLongParamSuffix("[T]");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"k\":\"abcdefghijk\"}");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("{\"k\":\"[T]\"}");
    }

    /**
     * 最大保留长度为负数时按 0 处理，只保留后缀
     */
    @Test
    @DisplayName("最大保留长度为负数时仅保留后缀")
    void getParamShouldKeepSuffixOnlyWhenMaxLengthIsNegative() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(-1);
        properties.setLongParamSuffix("[T]");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"k\":\"abcdefghijk\"}");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("{\"k\":\"[T]\"}");
    }

    /**
     * 截断后不短于原文时保留原文，截断不会让日志变长
     */
    @Test
    @DisplayName("截断后不变短时保留原文")
    void getParamShouldKeepOriginalWhenTruncationDoesNotShorten() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(10);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"k\":\"012345678901\"}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"k\":\"012345678901\"}");
    }

    /**
     * 开启脱敏但未配置敏感字段时，没有需要隐藏的内容，非 JSON 文本仍按原文记录
     */
    @Test
    @DisplayName("未配置敏感字段时非 JSON 文本仍记录")
    void getParamShouldKeepNonJsonParamsWhenNoSensitiveFieldConfigured() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("name=admin&role=1");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("name=admin&role=1");
    }

    /**
     * 阈值为负属于非法配置，按不截断处理而不是抛异常
     */
    @Test
    @DisplayName("阈值为负时不截断")
    void getParamShouldIgnoreNegativeThreshold() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(-1);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"k\":\"abcdefghijk\"}");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("{\"k\":\"abcdefghijk\"}");
    }

    /**
     * 脱敏与截断同时开启时互不覆盖
     */
    @Test
    @DisplayName("脱敏与截断同时生效")
    void getParamShouldMaskAndTruncateTogether() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("[T]");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"password\":\"p@ss\",\"k\":\"abcdefghijk\"}");
        Map<String, Object> parsed =
            JSONUtil.parseObj(AccessLogUtils.getParam(request, properties));
        assertThat(parsed).hasSize(2)
            .containsEntry("password", "***")
            .containsEntry("k", "abcde[T]");
    }

    /**
     * 非法 JSON 无法解析时降级为原始字符串，不向上抛异常
     */
    @Test
    @DisplayName("非法 JSON 参数降级为原始字符串")
    void getParamShouldFallbackToRawStringForMalformedJson() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"a\":1]");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("[{\"a\":1]");
    }

    /**
     * 开启脱敏但参数无法解析时跳过记录，避免无法脱敏的内容以明文写入日志
     */
    @Test
    @DisplayName("非法 JSON 且开启脱敏时不记录参数")
    void getParamShouldSkipParamsWhenMalformedJsonNeedsMasking() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"password\":\"p@ss\"}}]");
        assertThat(AccessLogUtils.getParam(request, properties)).isNull();
    }

    /**
     * 对象分支解析失败时同样跳过记录
     */
    @Test
    @DisplayName("非法 JSON 对象且开启脱敏时不记录参数")
    void getParamShouldSkipParamsWhenMalformedJsonObjectNeedsMasking() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"password\":}");
        assertThat(AccessLogUtils.getParam(request, properties)).isNull();
    }

    /**
     * 非 JSON 文本无法按字段脱敏，开启脱敏时同样跳过记录
     */
    @Test
    @DisplayName("非 JSON 文本且开启脱敏时不记录参数")
    void getParamShouldSkipParamsWhenNonJsonNeedsMasking() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setParamSensitive(true);
        properties.setSensitiveParams(List.of("password"));
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("name=admin&password=p@ss");
        assertThat(AccessLogUtils.getParam(request, properties)).isNull();
    }

    /**
     * 对象分支的非法 JSON 同样降级为原文，不向上抛异常
     */
    @Test
    @DisplayName("非法 JSON 对象参数降级为原始字符串")
    void getParamShouldFallbackToRawStringForMalformedJsonObject() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("{\"a\":}");
        assertThat(AccessLogUtils.getParam(request, properties)).isEqualTo("{\"a\":}");
    }

    /**
     * 降级为原文后超长截断照常生效
     */
    @Test
    @DisplayName("非法 JSON 降级后仍按阈值截断")
    void getParamShouldTruncateMalformedJson() {
        AccessLogProperties properties = new AccessLogProperties();
        properties.setPrintRequestParam(true);
        properties.setLongParamTruncate(true);
        properties.setLongParamThreshold(10);
        properties.setLongParamMaxLength(5);
        properties.setLongParamSuffix("...");
        RecordableHttpRequest request = mock(RecordableHttpRequest.class);
        when(request.getParams()).thenReturn("[{\"k\":\"abcdefghijk\"}}]");
        assertThat(AccessLogUtils.getParam(request, properties))
            .isEqualTo("[{\"k\"" + properties.getLongParamSuffix());
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
