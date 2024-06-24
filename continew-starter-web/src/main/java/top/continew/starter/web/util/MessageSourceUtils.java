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

package top.continew.starter.web.util;

import cn.hutool.extra.spring.SpringUtil;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * @author Jasmine
 * @since 2.2.0
 */
public class MessageSourceUtils {

    private static final MessageSource messageSource = SpringUtil.getBean(MessageSource.class);

    private static final Object[] emptyArray = new Object[] {};

    public static String getMessage(String key) {
        return getMessage(key, emptyArray);
    }

    public static String getMessage(String key, String defaultMessage) {
        return getMessage(key, defaultMessage, emptyArray);
    }

    public static String getMessage(String msgKey, Object... args) {
        return getMessage(msgKey, msgKey, args);
    }

    public static String getMessage(String msgKey, String defaultMessage, Object... args) {
        try {
            return messageSource.getMessage(msgKey, args, LocaleContextHolder.getLocale());
        } catch (Exception e) {
            return defaultMessage;
        }
    }

}
