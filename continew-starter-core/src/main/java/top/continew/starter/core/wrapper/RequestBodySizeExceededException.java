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

package top.continew.starter.core.wrapper;

import java.io.IOException;

/**
 * 请求体超过缓存大小上限异常
 *
 * <p>由 {@link RepeatReadRequestWrapper} 的有界构造在拷贝请求体时抛出，调用方可据此将
 * 超限请求与真实 IO 错误区分处理。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public class RequestBodySizeExceededException extends IOException {

    private static final long serialVersionUID = 1L;

    public RequestBodySizeExceededException(long maxBytes) {
        super("Request body exceeds the maximum allowed size of " + maxBytes + " bytes");
    }
}
