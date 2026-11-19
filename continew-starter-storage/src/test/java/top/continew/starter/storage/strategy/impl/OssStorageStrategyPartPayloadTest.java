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

package top.continew.starter.storage.strategy.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * 分片载荷值相等性测试。
 *
 * <p>PartPayload 是私有 record，其 equals 按字节数组内容而非引用比较，属于不易从公开 API 触发的契约。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
@DisplayName("OssStorageStrategy.PartPayload 单元测试")
class OssStorageStrategyPartPayloadTest {

    @Test
    @DisplayName("equals 比较字节内容、临时文件路径与大小")
    void shouldCompareByContentPathAndSize() throws Exception {
        Object payload = payload(new byte[] {1, 2, 3}, Path.of("a"), 3L);
        Object equal = payload(new byte[] {1, 2, 3}, Path.of("a"), 3L);
        Object differentBytes = payload(new byte[] {1, 2, 4}, Path.of("a"), 3L);
        Object differentPath = payload(new byte[] {1, 2, 3}, Path.of("b"), 3L);
        Object differentSize = payload(new byte[] {1, 2, 3}, Path.of("a"), 4L);

        assertThat(payload).isSameAs(payload);
        assertThat(payload).isEqualTo(equal);
        assertThat(payload).isNotEqualTo(differentBytes);
        assertThat(payload).isNotEqualTo(differentPath);
        assertThat(payload).isNotEqualTo(differentSize);
        assertThat(payload).isNotEqualTo("not a payload");
    }

    @Test
    @DisplayName("hashCode 与 equals 对等值载荷保持一致")
    void shouldKeepHashCodeConsistentWithEquals() throws Exception {
        Object payload = payload(new byte[] {1, 2, 3}, Path.of("a"), 3L);
        Object equal = payload(new byte[] {1, 2, 3}, Path.of("a"), 3L);

        assertThat(payload).hasSameHashCodeAs(equal);
    }

    private static Object payload(byte[] bytes, Path tempFile, long size) throws Exception {
        Class<?> type = Class.forName(
            "top.continew.starter.storage.strategy.impl.OssStorageStrategy$PartPayload");
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] parameterTypes = Arrays.stream(components).map(RecordComponent::getType)
            .toArray(Class<?>[]::new);
        Constructor<?> constructor = type.getDeclaredConstructor(parameterTypes);
        constructor.setAccessible(true);
        return constructor.newInstance(bytes, tempFile, size);
    }
}
