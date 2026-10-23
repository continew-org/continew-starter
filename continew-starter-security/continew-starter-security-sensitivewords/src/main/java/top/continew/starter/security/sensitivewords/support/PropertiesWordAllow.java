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
import top.continew.starter.security.sensitivewords.autoconfigure.SensitiveWordsProperties;

import java.util.List;

/**
 * 配置白名单提供者
 *
 * @author luoqiz
 * @since 3.0.0
 */
public class PropertiesWordAllow implements IWordAllow {

    private final SensitiveWordsProperties properties;

    public PropertiesWordAllow(SensitiveWordsProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<String> allow() {
        return PropertiesWordDeny.filter(properties.getAllowWords());
    }
}
