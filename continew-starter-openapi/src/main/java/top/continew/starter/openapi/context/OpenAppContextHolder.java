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

package top.continew.starter.openapi.context;

/**
 * 开放应用上下文持有器（ThreadLocal）
 *
 * <p>验签过滤器在签名校验通过后 set，请求结束时必须 remove，防止线程复用导致身份泄漏
 * 到后续请求。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class OpenAppContextHolder {

    private static final ThreadLocal<OpenAppContext> CONTEXT = new ThreadLocal<>();

    private OpenAppContextHolder() {
    }

    /**
     * 设置当前线程的开放应用上下文
     *
     * @param context 开放应用上下文
     */
    public static void setContext(OpenAppContext context) {
        CONTEXT.set(context);
    }

    /**
     * 获取当前线程的开放应用上下文
     *
     * @return 当前线程的开放应用上下文，未设置时返回 null
     */
    public static OpenAppContext getContext() {
        return CONTEXT.get();
    }

    /**
     * 清理当前线程的开放应用上下文
     */
    public static void remove() {
        CONTEXT.remove();
    }
}
