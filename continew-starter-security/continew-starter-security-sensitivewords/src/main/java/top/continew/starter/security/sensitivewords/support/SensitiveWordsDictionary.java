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

package top.continew.starter.security.sensitivewords.support;

import com.github.houbb.sensitive.word.api.IWordAllow;
import com.github.houbb.sensitive.word.api.IWordDeny;

/**
 * 敏感词库注册表
 *
 * <p>
 * 同时持有两条责任链。完整链包含运行时词库，用于引擎初始化；静态链只包含内置词库、配置和用户自定义词库，
 * 用于计算静态词快照。分开持有是为了避免从完整链里减去运行时词时，把「同时存在于静态词库与运行时词库」的词
 * 一并减掉，导致后续删词误伤静态词。
 * </p>
 *
 * @author luoqiz
 * @since 3.0.0
 */
public class SensitiveWordsDictionary {

    private final IWordDeny deny;

    private final IWordDeny staticDeny;

    private final IWordAllow allow;

    private final IWordAllow staticAllow;

    public SensitiveWordsDictionary(IWordDeny deny, IWordDeny staticDeny, IWordAllow allow,
        IWordAllow staticAllow) {
        this.deny = deny;
        this.staticDeny = staticDeny;
        this.allow = allow;
        this.staticAllow = staticAllow;
    }

    /**
     * 获取完整黑名单责任链（含运行时词库）
     *
     * @return 黑名单责任链
     */
    public IWordDeny getDeny() {
        return deny;
    }

    /**
     * 获取静态黑名单责任链（不含运行时词库）
     *
     * @return 静态黑名单责任链
     */
    public IWordDeny getStaticDeny() {
        return staticDeny;
    }

    /**
     * 获取完整白名单责任链（含运行时词库）
     *
     * @return 白名单责任链
     */
    public IWordAllow getAllow() {
        return allow;
    }

    /**
     * 获取静态白名单责任链（不含运行时词库）
     *
     * @return 静态白名单责任链
     */
    public IWordAllow getStaticAllow() {
        return staticAllow;
    }
}
