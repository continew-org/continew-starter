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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.storage.common.exception.StorageException;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link ZipFileUtils} 条目名规范化与去重单元测试。
 *
 * <p>目录穿越拒绝与重名追加 " (n)" 后缀是 ZIP 导入导出的安全与正确性边界。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("ZipFileUtils 条目名规范化与去重单元测试")
class ZipFileUtilsTest {

    @Test
    @DisplayName("反斜杠与重复斜杠折叠为单正斜杠")
    void shouldNormalizeSeparators() {
        assertEquals("a/b/c.txt", ZipFileUtils.normalizeEntryName("a\\b//c.txt"));
        assertEquals("a/b.txt", ZipFileUtils.normalizeEntryName("a\\\\b.txt"));
    }

    @Test
    @DisplayName("空名、绝对路径、驱动器路径与 . 名一律拒绝")
    void shouldRejectInvalidNames() {
        assertThrows(StorageException.class, () -> ZipFileUtils.normalizeEntryName(null));
        assertThrows(StorageException.class, () -> ZipFileUtils.normalizeEntryName("  "));
        assertThrows(StorageException.class, () -> ZipFileUtils.normalizeEntryName("/abs/a"));
        assertThrows(StorageException.class, () -> ZipFileUtils.normalizeEntryName("C:/win/a"));
        assertThrows(StorageException.class, () -> ZipFileUtils.normalizeEntryName("."));
        assertThrows(StorageException.class,
            () -> ZipFileUtils.normalizeEntryName("a/./b.txt"));
        assertThrows(StorageException.class,
            () -> ZipFileUtils.normalizeEntryName("a/../b.txt"));
    }

    @Test
    @DisplayName("首次重名追加 (1)，同名且候选被占时递增序号")
    void shouldDedupEntryNames() {
        Map<String, Integer> count = new HashMap<>();
        assertEquals("a.txt", ZipFileUtils.dedupEntryName("a.txt", count));
        assertEquals("a (1).txt", ZipFileUtils.dedupEntryName("a.txt", count));
        assertEquals("a (2).txt", ZipFileUtils.dedupEntryName("a.txt", count));
        // 无扩展名时在末尾追加序号
        Map<String, Integer> noext = new HashMap<>();
        noext.put("noext", 1);
        assertEquals("noext (1)", ZipFileUtils.dedupEntryName("noext", noext));
    }
}
