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

package top.continew.starter.security.sensitivewords.service;

import java.util.List;

/**
 * 敏感词服务接口
 *
 * @author luoqiz
 * @author Charles7c
 * @since 2.9.0
 */
public interface SensitiveWordsService {

    /**
     * 检查敏感词
     *
     * @param content 待检测字符串
     * @return 敏感词列表
     */
    List<String> check(String content);
}
