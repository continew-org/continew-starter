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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.storage.domain.model.context.UploadContext;

/**
 * 默认文件路径生成器单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("DefaultFilePathGenerator 单元测试")
class DefaultFilePathGeneratorTest {

    private final DefaultFilePathGenerator generator = new DefaultFilePathGenerator();

    @Test
    @DisplayName("仅当上下文未指定路径时接管生成")
    void shouldTakeOverOnlyWhenPathBlank() {
        assertTrue(generator.support(context(null)));
        assertTrue(generator.support(context(" ")));
        assertFalse(generator.support(context("custom/path")));
    }

    @Test
    @DisplayName("已指定路径时原样返回，不重新生成")
    void shouldKeepExplicitPathAsIs() {
        assertEquals("custom/path", generator.path(context("custom/path")));
    }

    @Test
    @DisplayName("未指定路径时按年/月/日层级生成默认路径")
    void shouldGenerateDateHierarchyWhenPathMissing() {
        String generated = generator.path(context(null));

        assertTrue(generated.matches("\\d{4}/\\d{1,2}/\\d{1,2}/"), "应为 年/月/日/ 层级，实际："
            + generated);
    }

    /**
     * 构造指定路径的上传上下文。
     *
     * @param path 上传路径
     * @return 上传上下文
     */
    private static UploadContext context(String path) {
        UploadContext context = new UploadContext();
        context.setPath(path);
        return context;
    }
}
