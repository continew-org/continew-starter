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

package top.continew.starter.security.sensitivewords.model;

/**
 * 敏感词库刷新结果
 *
 * <p>
 * 静态词指来自内置词库、配置、用户自定义词库的词；运行时词指通过
 * {@code addWord} 等方法临时添加的词。
 * </p>
 *
 * @param denyStaticCount   黑名单静态词数
 * @param denyRuntimeCount  黑名单运行时词数
 * @param denyAdded         黑名单新增静态词数
 * @param denyRemoved       黑名单移除静态词数
 * @param allowStaticCount  白名单静态词数
 * @param allowRuntimeCount 白名单运行时词数
 * @param allowAdded        白名单新增静态词数
 * @param allowRemoved      白名单移除静态词数
 * @author luoqiz
 * @since 3.0.0
 */
public record SensitiveWordsRefreshResult(int denyStaticCount,
    int denyRuntimeCount,
    int denyAdded,
    int denyRemoved,
    int allowStaticCount,
    int allowRuntimeCount,
    int allowAdded,
    int allowRemoved) {

    /**
     * 空结果
     *
     * @return 空结果
     */
    public static SensitiveWordsRefreshResult empty() {
        return new SensitiveWordsRefreshResult(0, 0, 0, 0, 0, 0, 0, 0);
    }
}
