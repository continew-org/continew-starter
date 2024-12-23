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

import com.feiniaojin.gracefulresponse.advice.lifecycle.exception.BeforeControllerAdviceProcess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

/**
 * 默认回调处理器实现
 *
 * @author Charles7c
 * @since 2.6.0
 */
public class DefaultBeforeControllerAdviceProcessImpl implements BeforeControllerAdviceProcess {

    private final Logger log = LoggerFactory.getLogger(DefaultBeforeControllerAdviceProcessImpl.class);
    private final GlobalResponseProperties globalResponseProperties;

    public DefaultBeforeControllerAdviceProcessImpl(GlobalResponseProperties globalResponseProperties) {
        this.globalResponseProperties = globalResponseProperties;
    }

    @Override
    public void call(HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception e) {
        if (globalResponseProperties.isPrintExceptionInGlobalAdvice()) {
            log.error("[{}] {}", request.getMethod(), request.getRequestURI(), e);
        }
    }
}
