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

package top.continew.starter.storage.common.util;

import top.continew.starter.storage.common.exception.StorageException;

import java.util.Map;

/**
 * ZIP 条目名工具
 *
 * @author echo
 * @since 3.1.0
 */
public final class ZipFileUtils {

    private ZipFileUtils() {
    }

    /**
     * 规范化 ZIP 条目名：反斜杠转正斜杠、折叠重复斜杠；
     * 空名、绝对路径、Windows 驱动器路径、单段为 "."、出现 ".." 段均拒绝（防目录穿越）
     *
     * @param raw 原始条目名
     * @return 规范化后的相对条目名
     */
    public static String normalizeEntryName(String raw) {
        if (raw == null) {
            throw new StorageException("ZIP 条目名为空");
        }
        String name = raw.trim().replace('\\', '/').replaceAll("/+", "/");
        if (name.isEmpty() || ".".equals(name) || name.startsWith("/")
            || name.length() >= 2 && Character.isLetter(name.charAt(0))
                && name.charAt(1) == ':') {
            throw new StorageException("非法 ZIP 条目名: " + raw);
        }
        for (String segment : name.split("/")) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                throw new StorageException("非法 ZIP 条目名: " + raw);
            }
        }
        return name;
    }

    /**
     * 生成不重复的 ZIP 条目名：重名时在扩展名前追加 " (n)" 后缀
     *
     * @param name      规范化条目名
     * @param nameCount 已用名计数表（跨调用共享，方法内自增）
     * @return 去重后的条目名
     */
    public static String dedupEntryName(String name, Map<String, Integer> nameCount) {
        int used = nameCount.getOrDefault(name, 0);
        if (used == 0) {
            nameCount.put(name, 1);
            return name;
        }
        String base = name;
        String ext = "";
        int dot = name.lastIndexOf('.');
        int slash = name.lastIndexOf('/');
        if (dot > slash) {
            base = name.substring(0, dot);
            ext = name.substring(dot);
        }
        // 第 n 次重名追加 " (n)"，首次重名为 (1)
        String candidate;
        do {
            candidate = base + " (" + used + ")" + ext;
            used++;
        } while (nameCount.containsKey(candidate));
        nameCount.put(name, used);
        nameCount.put(candidate, 1);
        return candidate;
    }
}
