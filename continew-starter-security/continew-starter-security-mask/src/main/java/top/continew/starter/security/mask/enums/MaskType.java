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

import cn.hutool.core.text.CharSequenceUtil;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.security.mask.strategy.IMaskStrategy;

/**
 * 脱敏类型
 *
 * @author Charles7c
 * @since 1.4.0
 */
public enum MaskType implements IMaskStrategy {

    /**
     * 自定义脱敏
     */
    CUSTOM {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.replaceByCodePoint(str, left, str.length() - right, character);
        }
    },

    /**
     * 手机号码
     * <p>保留前 3 位和后 4 位，例如：135****2210</p>
     */
    MOBILE_PHONE {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.replaceByCodePoint(str, 3, str.length() - 4, character);
        }
    },

    /**
     * 固定电话
     * <p>
     * 保留前 4 位和后 2 位
     * </p>
     */
    FIXED_PHONE {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.replaceByCodePoint(str, 4, str.length() - 2, character);
        }
    },

    /**
     * 电子邮箱
     *
     * <p>
     * 邮箱前缀仅保留第 1 个字母，@ 符号及后面的地址不脱敏，例如：d**@126.com
     * </p>
     */
    EMAIL {
        @Override
        public String mask(String str, char character, int left, int right) {
            int index = str.indexOf(StringConstants.AT);
            if (index <= 1) {
                return str;
            }
            return CharSequenceUtil.replaceByCodePoint(str, 1, index, character);
        }
    },

    /**
     * 身份证号
     * <p>
     * 保留前 1 位和后 2 位
     * </p>
     */
    ID_CARD {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.replaceByCodePoint(str, 1, str.length() - 2, character);
        }
    },

    /**
     * 银行卡
     * <p>
     * 由于银行卡号长度不定，所以只保留前 4 位，后面保留的位数根据卡号决定展示 1-4 位
     * <ul>
     * <li>1234 2222 3333 4444 6789 9 => 1234 **** **** **** **** 9</li>
     * <li>1234 2222 3333 4444 6789 91 => 1234 **** **** **** **** 91</li>
     * <li>1234 2222 3333 4444 678 => 1234 **** **** **** 678</li>
     * <li>1234 2222 3333 4444 6789 => 1234 **** **** **** 6789</li>
     * </ul>
     * </p>
     */
    BANK_CARD {
        @Override
        public String mask(String str, char character, int left, int right) {
            String cleanStr = CharSequenceUtil.cleanBlank(str);
            if (cleanStr.length() < 9) {
                return cleanStr;
            }
            final int length = cleanStr.length();
            final int endLength = length % 4 == 0 ? 4 : length % 4;
            final int midLength = length - 4 - endLength;
            final StringBuilder buffer = new StringBuilder();
            buffer.append(cleanStr, 0, 4);
            for (int i = 0; i < midLength; ++i) {
                if (i % 4 == 0) {
                    buffer.append(StringConstants.SPACE);
                }
                buffer.append(character);
            }
            buffer.append(StringConstants.SPACE).append(cleanStr, length - endLength, length);
            return buffer.toString();
        }
    },

    /**
     * 中国大陆车牌（包含普通车辆、新能源车辆）
     * <p>
     * 例如：苏D40000 => 苏D4***0
     * </p>
     */
    CAR_LICENSE {
        @Override
        public String mask(String str, char character, int left, int right) {
            // 普通车牌
            int length = str.length();
            if (length == 7) {
                return CharSequenceUtil.replaceByCodePoint(str, 3, 6, character);
            }
            // 新能源车牌
            if (length == 8) {
                return CharSequenceUtil.replaceByCodePoint(str, 3, 7, character);
            }
            return str;
        }
    },

    /**
     * 中文名
     * <p>
     * 只保留第 1 个汉字，例如：李**
     * </p>
     */
    CHINESE_NAME {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.replaceByCodePoint(str, 1, str.length(), character);
        }
    },

    /**
     * 密码
     * <p>
     * 密码的全部字符都使用脱敏符号代替，例如：******
     * </p>
     */
    PASSWORD {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.repeat(character, str.length());
        }
    },

    /**
     * 地址
     * <p>
     * 只显示到地区，不显示详细地址，例如：北京市海淀区****
     * </p>
     */
    ADDRESS {
        @Override
        public String mask(String str, char character, int left, int right) {
            int length = str.length();
            return CharSequenceUtil.replaceByCodePoint(str, length - 8, length, character);
        }
    },

    /**
     * IPv4 地址
     * <p>
     * 例如：192.0.2.1 => 192.*.*.*
     * </p>
     */
    IPV4 {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.subBefore(str, StringConstants.DOT, false) + String
                .format(".%s.%s.%s", character, character, character);
        }
    },

    /**
     * IPv6 地址
     * <p>
     * 例如：2001:0db8:86a3:08d3:1319:8a2e:0370:7344 => 2001:*:*:*:*:*:*:*
     * </p>
     */
    IPV6 {
        @Override
        public String mask(String str, char character, int left, int right) {
            return CharSequenceUtil.subBefore(str, StringConstants.COLON, false) + String
                .format(":%s:%s:%s:%s:%s:%s:%s", character, character, character, character, character, character, character);
        }
    },;
}
