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

package top.continew.starter.storage.processor.preprocess.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.domain.model.context.UploadContext;

/**
 * 文件大小校验器单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("FileSizeValidator 单元测试")
class FileSizeValidatorTest {

    @Test
    @DisplayName("文件超过上限抛出 StorageException，异常说明超限")
    void shouldRejectFileOverLimit() {
        UploadContext context = contextWithSize(11L * 1024 * 1024);

        StorageException error = assertThrows(StorageException.class, () -> FileSizeValidator
            .maxMB(10)
            .validate(context));
        assertTrue(error.getMessage().contains("文件大小超过限制"), "异常应说明超限，实际："
            + error.getMessage());
    }

    @Test
    @DisplayName("文件恰好等于上限时放行（边界语义为超过才拒绝）")
    void shouldAcceptFileExactlyAtLimit() {
        UploadContext context = contextWithSize(1024);

        assertDoesNotThrow(() -> FileSizeValidator.maxSize(1024).validate(context));
    }

    @Test
    @DisplayName("工厂方法按对应单位换算上限（以真实文件大小的边界行为验证）")
    void shouldConvertFactoryUnitsByBoundaryBehavior() {
        assertDoesNotThrow(() -> FileSizeValidator.maxKB(1).validate(contextWithSize(1024)));
        assertThrows(StorageException.class, () -> FileSizeValidator.maxKB(1)
            .validate(contextWithSize(1025)));

        assertDoesNotThrow(() -> FileSizeValidator.maxMB(1).validate(contextWithSize(1024 * 1024)));
        assertThrows(StorageException.class, () -> FileSizeValidator.maxMB(1)
            .validate(contextWithSize(1024 * 1024 + 1)));
    }

    @Test
    @DisplayName("create 保持构造时的上限配置")
    void shouldPreserveConfiguredLimitOnCreate() {
        FileSizeValidator validator = FileSizeValidator.maxSize(100);
        assertDoesNotThrow(() -> validator.create().validate(contextWithSize(100)));
        assertThrows(StorageException.class, () -> validator.create()
            .validate(contextWithSize(101)));
    }

    @Test
    @DisplayName("support 恒为 true，getName 返回类简名")
    void shouldSupportAllContextsAndExposeName() {
        FileSizeValidator validator = FileSizeValidator.maxSize(1);
        assertTrue(validator.support(new UploadContext()));
        assertEquals(FileSizeValidator.class.getSimpleName(), validator.getName());
    }

    /**
     * 构造指定大小的上传上下文。
     *
     * @param size 文件大小（字节）
     * @return 上传上下文
     */
    private static UploadContext contextWithSize(long size) {
        UploadContext context = new UploadContext();
        // 校验器只消费 getSize()，用匿名类注入大小可覆盖 GB 级上限而无需真实分配内存
        context.setFile(new MockMultipartFile("file", "a.bin", "application/octet-stream",
            new byte[0]) {

            @Override
            public long getSize() {
                return size;
            }
        });
        return context;
    }
}
