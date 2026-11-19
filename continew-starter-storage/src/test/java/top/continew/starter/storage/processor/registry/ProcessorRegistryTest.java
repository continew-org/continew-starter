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

package top.continew.starter.storage.processor.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.storage.domain.model.context.UploadContext;
import top.continew.starter.storage.processor.preprocess.FileNameGenerator;
import top.continew.starter.storage.processor.preprocess.FileValidator;
import top.continew.starter.storage.processor.preprocess.ThumbnailProcessor;
import top.continew.starter.storage.service.FileProcessor;

import java.util.List;

/**
 * 全局处理器注册表单元测试。
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("ProcessorRegistry 单元测试")
class ProcessorRegistryTest {

    private static final UploadContext CONTEXT = new UploadContext();

    private ProcessorRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new ProcessorRegistry();
    }

    @Test
    @DisplayName("按接口类型自动识别并归类处理器")
    void shouldAutoDetectProcessorType() {
        FileNameGenerator generator = new StubFileNameGenerator(0);
        FileValidator validator = new StubFileValidator(0);
        FileProcessor plain = new StubProcessor(0);

        registry.register(generator);
        registry.register(validator);
        registry.register(plain);

        assertEquals(List.of(generator),
            registry.getProcessors(FileNameGenerator.class, null, CONTEXT));
        assertEquals(List.of(validator),
            registry.getProcessors(FileValidator.class, null, CONTEXT));
        assertEquals(List.of(plain), registry.getProcessors(FileProcessor.class, null, CONTEXT));
        assertTrue(registry.getProcessors(ThumbnailProcessor.class, null, CONTEXT).isEmpty());
    }

    @Test
    @DisplayName("全局与平台特定处理器合并返回")
    void shouldMergeGlobalAndPlatformProcessors() {
        FileNameGenerator global = new StubFileNameGenerator(0);
        FileNameGenerator platform = new StubFileNameGenerator(0);
        registry.register(global);
        registry.register(platform, "oss");

        assertEquals(List.of(global),
            registry.getProcessors(FileNameGenerator.class, null, CONTEXT));
        assertEquals(List.of(global, platform),
            registry.getProcessors(FileNameGenerator.class, "oss", CONTEXT));
        assertEquals(List.of(global),
            registry.getProcessors(FileNameGenerator.class, "s3", CONTEXT));
    }

    @Test
    @DisplayName("过滤不支持当前上下文的处理器")
    void shouldFilterProcessorsNotSupportingContext() {
        FileNameGenerator supported = new StubFileNameGenerator(0);
        FileNameGenerator unsupported = new StubFileNameGenerator(0) {

            @Override
            public boolean support(UploadContext context) {
                return false;
            }
        };
        registry.register(supported);
        registry.register(unsupported);

        assertEquals(List.of(supported),
            registry.getProcessors(FileNameGenerator.class, null, CONTEXT));
    }

    @Test
    @DisplayName("按优先级降序返回处理器")
    void shouldSortProcessorsByDescendingOrder() {
        FileNameGenerator low = new StubFileNameGenerator(1);
        FileNameGenerator high = new StubFileNameGenerator(9);
        FileNameGenerator middle = new StubFileNameGenerator(5);
        registry.register(low);
        registry.register(high);
        registry.register(middle);

        assertEquals(List.of(high, middle, low),
            registry.getProcessors(FileNameGenerator.class, null, CONTEXT));
    }

    @Test
    @DisplayName("getProcessor 返回最高优先级处理器，无匹配时返回 null")
    void shouldReturnHighestOrderProcessorOrNull() {
        assertNull(registry.getProcessor(FileNameGenerator.class, null, CONTEXT));

        FileNameGenerator low = new StubFileNameGenerator(2);
        FileNameGenerator high = new StubFileNameGenerator(8);
        registry.register(low);
        registry.register(high);

        assertSame(high, registry.getProcessor(FileNameGenerator.class, null, CONTEXT));
    }

    @Test
    @DisplayName("平台无对应类型处理器时仅返回全局处理器")
    void shouldReturnGlobalOnlyWhenPlatformHasNoType() {
        FileNameGenerator global = new StubFileNameGenerator(0);
        FileValidator platformValidator = new StubFileValidator(0);
        registry.register(global);
        registry.register(platformValidator, "oss");

        assertEquals(List.of(global),
            registry.getProcessors(FileNameGenerator.class, "oss", CONTEXT));
    }

    private static class StubProcessor implements FileProcessor {

        private final int order;

        StubProcessor(int order) {
            this.order = order;
        }

        @Override
        public String getName() {
            return "stub";
        }

        @Override
        public int getOrder() {
            return order;
        }

        @Override
        public boolean support(UploadContext context) {
            return true;
        }
    }

    private static class StubFileNameGenerator extends StubProcessor implements FileNameGenerator {

        StubFileNameGenerator(int order) {
            super(order);
        }

        @Override
        public String generate(UploadContext context) {
            return "generated";
        }
    }

    private static class StubFileValidator extends StubProcessor implements FileValidator {

        StubFileValidator(int order) {
            super(order);
        }

        @Override
        public void validate(UploadContext context) {
            // 测试替身无需校验逻辑
        }
    }
}
