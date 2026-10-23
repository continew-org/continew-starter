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
 * 敏感词命中结果
 *
 * <p>
 * {@code word} 为原文片段而非词库标准词形，便于前端直接定位高亮。
 * </p>
 *
 * @param word       命中的原文片段
 * @param startIndex 起始下标（包含）
 * @param endIndex   结束下标（不包含）
 * @author luoqiz
 * @since 3.0.0
 */
public record SensitiveWordHit(String word, int startIndex, int endIndex) {
}
