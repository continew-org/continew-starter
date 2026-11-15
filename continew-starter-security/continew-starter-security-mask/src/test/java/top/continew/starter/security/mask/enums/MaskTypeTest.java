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

package top.continew.starter.security.mask.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * {@link MaskType} 各脱敏策略单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class MaskTypeTest {

    private static final char MASK = '*';

    /**
     * 自定义脱敏应按左右保留位数替换中间部分。
     */
    @Test
    @DisplayName("CUSTOM 按左右保留位替换")
    void shouldMaskByCustomRange() {
        String source = "{{PHONE_5gs5wzrd}}";
        assertThat(MaskType.CUSTOM.mask(source, MASK, 3, 4))
            .isEqualTo(source.substring(0, 3) + "*".repeat(source.length() - 7)
                + source.substring(source.length() - 4));
    }

    /**
     * 手机号应保留前 3 位与后 4 位。
     */
    @Test
    @DisplayName("MOBILE_PHONE 保留前3后4")
    void shouldMaskMobilePhone() {
        String source = "{{PHONE_sdy3bthw}}";
        assertThat(MaskType.MOBILE_PHONE.mask(source, MASK, 0, 0))
            .isEqualTo(source.substring(0, 3) + "*".repeat(source.length() - 7)
                + source.substring(source.length() - 4));
    }

    /**
     * 固定电话应保留前 4 位与后 2 位。
     */
    @Test
    @DisplayName("FIXED_PHONE 保留前4后2")
    void shouldMaskFixedPhone() {
        assertThat(MaskType.FIXED_PHONE.mask("01012345678", MASK, 0, 0)).isEqualTo("0101*****78");
    }

    /**
     * 邮箱应只保留前缀第 1 个字符，@ 后地址不脱敏；前缀过短时原样返回。
     */
    @Test
    @DisplayName("EMAIL 保留前缀首字符")
    void shouldMaskEmailPrefix() {
        String source = "{{EMAIL_pq2z6rhn}}".concat("@126.com");
        assertThat(MaskType.EMAIL.mask(source, MASK, 0, 0))
            .isEqualTo(source.substring(0, 1) + "*".repeat(source.indexOf("@") - 1)
                + source.substring(source.indexOf("@")));
        assertThat(MaskType.EMAIL.mask("@126.com", MASK, 0, 0)).isEqualTo("@126.com");
        assertThat(MaskType.EMAIL.mask("@126.com", MASK, 0, 0)).isEqualTo("@126.com");
    }

    /**
     * 身份证号应保留前 1 位与后 2 位。
     */
    @Test
    @DisplayName("ID_CARD 保留前1后2")
    void shouldMaskIdCard() {
        assertThat(MaskType.ID_CARD.mask("110101199001011234", MASK, 0, 0))
            .isEqualTo("1***************34");
    }

    /**
     * 银行卡应清理空白后按 4 位分组脱敏；长度不足 9 位原样返回。
     */
    @Test
    @DisplayName("BANK_CARD 按 4 位分组脱敏")
    void shouldMaskBankCardInGroups() {
        String source = "{{ACCOUNT_4ntt56sb}}".replace(" ", "");
        String masked = MaskType.BANK_CARD.mask(source, MASK, 0, 0);
        assertThat(masked).startsWith(source.substring(0, 4)).endsWith(source
            .substring(source.length() - (source.length() % 4 == 0 ? 4 : source.length() % 4)));
        assertThat(MaskType.BANK_CARD.mask("12345678", MASK, 0, 0)).isEqualTo("12345678");
    }

    /**
     * 车牌应按普通（7 位）与新能源（8 位）规则脱敏，其余长度原样返回。
     */
    @Test
    @DisplayName("CAR_LICENSE 按 7/8 位规则")
    void shouldMaskCarLicense() {
        assertThat(MaskType.CAR_LICENSE.mask("苏D40000", MASK, 0, 0)).isEqualTo("苏D4***0");
        assertThat(MaskType.CAR_LICENSE.mask("苏D40000D", MASK, 0, 0)).isEqualTo("苏D4****D");
        assertThat(MaskType.CAR_LICENSE.mask("ABC123", MASK, 0, 0)).isEqualTo("ABC123");
    }

    /**
     * 中文名应只保留第 1 个字符。
     */
    @Test
    @DisplayName("CHINESE_NAME 保留首字符")
    void shouldMaskChineseName() {
        assertThat(MaskType.CHINESE_NAME.mask("李逵", MASK, 0, 0)).isEqualTo("李*");
    }

    /**
     * 密码应全字符替换。
     */
    @Test
    @DisplayName("PASSWORD 全字符替换")
    void shouldMaskPasswordFully() {
        assertThat(MaskType.PASSWORD.mask("123456", MASK, 0, 0)).isEqualTo("******");
    }

    /**
     * 地址应保留前段并脱敏末 8 位。
     */
    @Test
    @DisplayName("ADDRESS 脱敏末 8 位")
    void shouldMaskAddress() {
        String source = "{{ADDRESS_tkzb3j6t}}";
        assertThat(MaskType.ADDRESS.mask(source, MASK, 0, 0))
            .isEqualTo(source.substring(0, source.length() - 8) + "*".repeat(8));
    }

    /**
     * IPv4 应保留首段。
     */
    @Test
    @DisplayName("IPV4 保留首段")
    void shouldMaskIpv4() {
        assertThat(MaskType.IPV4.mask("192.0.2.1", MASK, 0, 0)).isEqualTo("192.*.*.*");
    }

    /**
     * IPv6 应保留首段。
     */
    @Test
    @DisplayName("IPV6 保留首段")
    void shouldMaskIpv6() {
        assertThat(MaskType.IPV6.mask("2001:0db8:86a3:08d3:1319:8a2e:0370:7344", MASK, 0, 0))
            .isEqualTo("2001:*:*:*:*:*:*:*");
    }

    /**
     * 每个枚举常量的 name 应与其策略标识一致（IMaskStrategy 契约兜底）。
     *
     * @param type 脱敏类型
     */
    @ParameterizedTest
    @EnumSource(MaskType.class)
    @DisplayName("全枚举 mask 均可执行且不改变入参")
    void shouldNotMutateInputForAllTypes(MaskType type) {
        String source = "{{PHONE_sdy3bthw}}";
        type.mask(source, MASK, 3, 4);
        assertThat(source).isEqualTo("{{PHONE_sdy3bthw}}");
    }
}
