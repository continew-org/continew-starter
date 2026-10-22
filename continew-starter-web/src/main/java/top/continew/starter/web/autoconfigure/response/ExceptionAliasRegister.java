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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.web.annotation.ExceptionAliasFor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异常别名注册器
 *
 * @author Charles7c
 * @since 3.0.0
 */
public class ExceptionAliasRegister {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExceptionAliasRegister.class);

    private final Map<Class<? extends Throwable>, ExceptionAliasFor> aliasForMap =
        new ConcurrentHashMap<>();

    /**
     * 注册异常别名
     *
     * @param throwableClass 被别名的异常类型，需标注 {@link ExceptionAliasFor}
     * @return 当前注册器
     */
    public ExceptionAliasRegister doRegisterExceptionAlias(
        Class<? extends Throwable> throwableClass) {
        ExceptionAliasFor exceptionAliasFor = throwableClass.getAnnotation(ExceptionAliasFor.class);
        if (exceptionAliasFor == null) {
            LOGGER.error("注册了未标注 @ExceptionAliasFor 的异常，throwableClass={}", throwableClass);
            throw new IllegalArgumentException("注册了未标注 @ExceptionAliasFor 的异常：" + throwableClass
                .getName());
        }
        for (Class<? extends Throwable> aliasFor : exceptionAliasFor.aliasFor()) {
            aliasForMap.put(aliasFor, exceptionAliasFor);
        }
        return this;
    }

    /**
     * 获取异常别名
     *
     * @param throwableClass 异常类型
     * @return 异常别名，未注册时返回 {@code null}
     */
    public ExceptionAliasFor getExceptionAliasFor(Class<? extends Throwable> throwableClass) {
        return aliasForMap.get(throwableClass);
    }
}
