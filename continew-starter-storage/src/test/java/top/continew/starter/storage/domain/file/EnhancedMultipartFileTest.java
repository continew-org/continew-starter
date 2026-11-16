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

package top.continew.starter.storage.domain.file;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * 增强版 MultipartFile 测试
 *
 * <p>
 * 覆盖创建与包装两种构造模式、缓存加载与清理、字节流与 transferTo 的内容转发一致性；
 * 包装原文件使用 Mockito 桩，落盘写入 JUnit 管理的临时目录。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class EnhancedMultipartFileTest {

    private static final byte[] CONTENT = "file-content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    File tempDir;

    @Test
    void shouldExposeMetadataAndContentWhenCreatedFromBytes() throws IOException {
        EnhancedMultipartFile file = EnhancedMultipartFile.create("file", "a.txt", "text/plain",
            CONTENT);
        assertEquals("file", file.getName());
        assertEquals("a.txt", file.getOriginalFilename());
        assertEquals("text/plain", file.getContentType());
        assertFalse(file.isEmpty());
        assertEquals(CONTENT.length, file.getSize());
        assertFalse(file.isWrapped());
        assertFalse(file.isCacheEnabled());
        assertTrue(file.isCached());
        assertArrayEquals(CONTENT, file.getBytes());
        // 包装 EnhancedMultipartFile 实例时应直接返回原实例
        assertSame(file, EnhancedMultipartFile.wrap(file));
        assertSame(file, EnhancedMultipartFile.wrap(file, true));
    }

    @Test
    void shouldCreateEmptyTargetWhenCreatedWithoutContent() throws IOException {
        EnhancedMultipartFile file = EnhancedMultipartFile.create("file", "a.txt", "text/plain",
            null);
        assertTrue(file.isEmpty());
        assertEquals(0, file.getSize());
        assertNull(file.getBytes());
        try (InputStream empty = file.getInputStream()) {
            assertEquals(-1, empty.read());
        }
        File dest = this.tempDir.toPath().resolve("created.bin").toFile();
        file.transferTo(dest);
        assertTrue(dest.isFile());
        assertEquals(0, Files.size(dest.toPath()));
        assertThrows(IOException.class, () -> file.transferTo(dest));
    }

    @Test
    void shouldDelegateToOriginalWhenWrappedWithoutCache() throws IOException {
        MultipartFile original = mock(MultipartFile.class);
        when(original.getName()).thenReturn("file");
        when(original.getOriginalFilename()).thenReturn("a.txt");
        when(original.getContentType()).thenReturn("text/plain");
        when(original.isEmpty()).thenReturn(false);
        when(original.getSize()).thenReturn((long) CONTENT.length);
        when(original.getBytes()).thenReturn(CONTENT);
        when(original.getInputStream()).thenReturn(new ByteArrayInputStream(CONTENT));
        EnhancedMultipartFile file = EnhancedMultipartFile.wrap(original);
        assertEquals("file", file.getName());
        assertEquals("a.txt", file.getOriginalFilename());
        assertEquals("text/plain", file.getContentType());
        assertFalse(file.isEmpty());
        assertEquals(CONTENT.length, file.getSize());
        assertTrue(file.isWrapped());
        assertFalse(file.isCacheEnabled());
        assertFalse(file.isCached());
        assertArrayEquals(CONTENT, file.getBytes());
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
        // 未启用缓存时清理缓存应为空操作
        file.clearCache();
        assertFalse(file.isCached());
    }

    @Test
    void shouldCacheOriginalBytesAndTransferFromCacheWhenCacheEnabled() throws IOException {
        MultipartFile original = mock(MultipartFile.class);
        when(original.getSize()).thenReturn((long) CONTENT.length);
        when(original.getBytes()).thenReturn(CONTENT);
        EnhancedMultipartFile file = EnhancedMultipartFile.wrap(original, true);
        assertFalse(file.isCached());
        try (InputStream in = file.getInputStream()) {
            assertArrayEquals(CONTENT, in.readAllBytes());
        }
        assertTrue(file.isCached());
        assertArrayEquals(CONTENT, file.getBytes());
        assertEquals(CONTENT.length, file.getSize());
        verify(original, times(1)).getBytes();
        File dest = this.tempDir.toPath().resolve("cached.bin").toFile();
        file.transferTo(dest);
        assertArrayEquals(CONTENT, Files.readAllBytes(dest.toPath()));
        file.clearCache();
        assertFalse(file.isCached());
        assertNull(file.getCachedBytes());
    }

    @Test
    void shouldClearCacheOnlyWhenCacheEnabled() throws IOException {
        MultipartFile original = mock(MultipartFile.class);
        when(original.getBytes()).thenReturn(CONTENT);
        EnhancedMultipartFile cached = EnhancedMultipartFile.wrap(original, true);
        cached.getBytes();
        assertTrue(cached.isCached());
        cached.clearCache();
        assertFalse(cached.isCached());
    }
}
