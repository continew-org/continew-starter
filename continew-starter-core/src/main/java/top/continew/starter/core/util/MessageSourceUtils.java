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

package top.continew.starter.core.util;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 国际化工具类
 *
 * @author Jasmine
 * @since 2.2.0
 */
public class MessageSourceUtils {

    private static final MessageSource MESSAGE_SOURCE = SpringUtil.getBean(MessageSource.class);
    private static final Object[] EMPTY_ARGS = {};

    private MessageSourceUtils() {
    }

    /**
     * 根据消息编码获取
     *
     * @param code 消息编码
     * @return 国际化后的消息
     */
    public static String getMessage(String code) {
        return getMessage(code, EMPTY_ARGS);
    }

    /**
     * 根据消息编码获取
     *
     * @param code 消息编码
     * @param args 参数
     * @return 国际化后的消息
     */
    public static String getMessage(String code, Object... args) {
        return getMessage(code, code, args);
    }

    /**
     * 根据消息编码获取
     *
     * @param code           消息编码
     * @param defaultMessage 默认消息
     * @return 国际化后的消息
     */
    public static String getMessage(String code, String defaultMessage) {
        return getMessage(code, defaultMessage, EMPTY_ARGS);
    }

    /**
     * 根据消息编码获取
     *
     * @param code           消息编码
     * @param defaultMessage 默认消息
     * @param args           参数
     * @return 国际化后的消息
     */
    public static String getMessage(String code, String defaultMessage, Object... args) {
        try {
            return MESSAGE_SOURCE.getMessage(code, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return defaultMessage;
        }
    }
}
