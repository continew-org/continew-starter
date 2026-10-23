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

package top.continew.starter.security.sensitivewords.autoconfigure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import top.continew.starter.core.constant.PropertiesConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * 敏感词配置属性
 *
 * @author luoqiz
 * @author Charles7c
 * @since 3.0.0
 */
@ConfigurationProperties(PropertiesConstants.SECURITY_SENSITIVE_WORDS)
public class SensitiveWordsProperties {

    /**
     * 是否启用检测
     */
    private boolean enabled = true;

    /**
     * 是否加载内置词库（黑名单与白名单）
     */
    private boolean useDefaultDeny = true;

    /**
     * 追加敏感词
     */
    private List<String> denyWords = new ArrayList<>();

    /**
     * 白名单
     */
    private List<String> allowWords = new ArrayList<>();

    /**
     * 是否忽略重复字符
     */
    private boolean ignoreRepeat = false;

    /**
     * 是否检测数字（8 位及以上连续数字，如手机号）
     */
    private boolean numCheck = false;

    /**
     * 是否检测邮箱
     */
    private boolean emailCheck = false;

    /**
     * 是否检测网址
     */
    private boolean urlCheck = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isUseDefaultDeny() {
        return useDefaultDeny;
    }

    public void setUseDefaultDeny(boolean useDefaultDeny) {
        this.useDefaultDeny = useDefaultDeny;
    }

    public List<String> getDenyWords() {
        return denyWords;
    }

    public void setDenyWords(List<String> denyWords) {
        this.denyWords = denyWords;
    }

    public List<String> getAllowWords() {
        return allowWords;
    }

    public void setAllowWords(List<String> allowWords) {
        this.allowWords = allowWords;
    }

    public boolean isIgnoreRepeat() {
        return ignoreRepeat;
    }

    public void setIgnoreRepeat(boolean ignoreRepeat) {
        this.ignoreRepeat = ignoreRepeat;
    }

    public boolean isNumCheck() {
        return numCheck;
    }

    public void setNumCheck(boolean numCheck) {
        this.numCheck = numCheck;
    }

    public boolean isEmailCheck() {
        return emailCheck;
    }

    public void setEmailCheck(boolean emailCheck) {
        this.emailCheck = emailCheck;
    }

    public boolean isUrlCheck() {
        return urlCheck;
    }

    public void setUrlCheck(boolean urlCheck) {
        this.urlCheck = urlCheck;
    }
}
