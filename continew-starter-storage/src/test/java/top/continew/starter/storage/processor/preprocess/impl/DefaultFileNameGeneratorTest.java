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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import top.continew.starter.storage.domain.model.context.UploadContext;

/**
 * 默认文件名生成器单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("DefaultFileNameGenerator 单元测试")
class DefaultFileNameGeneratorTest {

    private final DefaultFileNameGenerator generator = new DefaultFileNameGenerator();

    @Test
    @DisplayName("仅当上下文未指定格式化文件名时接管生成")
    void shouldTakeOverOnlyWhenFormatFileNameBlank() {
        assertTrue(generator.support(context("photo.jpg", null)));
        assertTrue(generator.support(context("photo.jpg", "  ")));
        assertFalse(generator.support(context("photo.jpg", "custom-name")));
    }

    @Test
    @DisplayName("生成文件名保留原始扩展名，主体为 17 位时间戳")
    void shouldGenerateTimestampNameWithOriginalExtension() {
        String generated = generator.generate(context("photo.jpg", null));

        assertTrue(generated.matches("\\d{17}\\.jpg"), "应为 17 位时间戳 + 原扩展名，实际："
            + generated);
    }

    @Test
    @DisplayName("无扩展名的原始文件生成无后缀的时间戳文件名")
    void shouldGenerateTimestampNameWithoutExtension() {
        String generated = generator.generate(context("README", null));

        assertTrue(generated.matches("\\d{17}"), "无扩展名时应生成纯时间戳，实际：" + generated);
    }

    /**
     * 构造指定原始文件名与格式化文件名的上传上下文。
     *
     * @param originalFilename 原始文件名
     * @param formatFileName   格式化文件名
     * @return 上传上下文
     */
    private static UploadContext context(String originalFilename, String formatFileName) {
        UploadContext context = new UploadContext();
        context.setFormatFileName(formatFileName);
        context.setFile(new MockMultipartFile("file", originalFilename, "image/jpeg",
            new byte[0]));
        return context;
    }
}
