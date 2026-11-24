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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.storage.common.exception.StorageException;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.InputStream;
import java.util.List;

/**
 * {@link StorageStrategyDecorator} 委托语义单元测试。
 *
 * <p>所有方法应原样委托给被装饰策略；未注入 delegate 时取用即抛错。</p>
 *
 * @author echo
 * @since 3.1.0
 */
@DisplayName("StorageStrategyDecorator 委托语义单元测试")
class StorageStrategyDecoratorTest {

    private static StorageStrategyDecorator<StorageStrategy> decorator(
        StorageStrategy delegate) {
        StorageStrategyDecorator<StorageStrategy> d = new StorageStrategyDecorator<>() {

            @Override
            public Class<StorageStrategy> getTargetStrategyClass() {
                return StorageStrategy.class;
            }
        };
        d.setDelegate(delegate);
        return d;
    }

    @Test
    @DisplayName("未注入 delegate 时取用抛 IllegalStateException")
    void shouldThrowWhenDelegateMissing() {
        StorageStrategyDecorator<StorageStrategy> d = new StorageStrategyDecorator<>() {

            @Override
            public Class<StorageStrategy> getTargetStrategyClass() {
                return StorageStrategy.class;
            }
        };
        assertThrows(IllegalStateException.class, () -> d.upload("b", "p", null));
    }

    @Test
    @DisplayName("查询与生命周期方法均原样委托")
    void shouldDelegateQueryAndLifecycle() {
        StorageStrategy delegate = mock(StorageStrategy.class);
        when(delegate.getPlatform()).thenReturn("p1");
        when(delegate.supportUploadStream()).thenReturn(true);
        when(delegate.exists("b", "k")).thenReturn(true);
        StorageStrategyDecorator<StorageStrategy> d = decorator(delegate);

        assertEquals("p1", d.getPlatform());
        assertTrue(d.supportUploadStream());
        assertTrue(d.exists("b", "k"));
        d.cleanup();
        d.delete("b", "k");
        d.batchDelete("b", List.of("a", "c"));
        verify(delegate).cleanup();
        verify(delegate).delete("b", "k");
        verify(delegate).batchDelete("b", List.of("a", "c"));
    }

    @Test
    @DisplayName("IO 委托方法透传参数与返回")
    void shouldDelegateIoMethods() throws Exception {
        StorageStrategy delegate = mock(StorageStrategy.class);
        InputStream rangeIn = InputStream.nullInputStream();
        when(delegate.downloadRange("b", "k", 1, 3)).thenReturn(rangeIn);
        StorageStrategyDecorator<StorageStrategy> d = decorator(delegate);

        try (InputStream in = d.downloadRange("b", "k", 1, 3)) {
            assertEquals(rangeIn, in);
        }
        d.uploadStream("b", "k", InputStream.nullInputStream(), "text/plain", 0);
        verify(delegate).uploadStream(org.mockito.ArgumentMatchers.eq("b"),
            org.mockito.ArgumentMatchers.eq("k"), org.mockito.ArgumentMatchers.any(),
            org.mockito.ArgumentMatchers.eq("text/plain"),
            org.mockito.ArgumentMatchers.eq(0L));
        d.copy("sb", "tb", "sp", "tp");
        verify(delegate).copy("sb", "tb", "sp", "tp");
    }

    @Test
    @DisplayName("不支持操作由被装饰策略决定并冒泡")
    void shouldPropagateDelegateFailures() {
        StorageStrategy delegate = mock(StorageStrategy.class);
        when(delegate.downloadRange("b", "k", 0, 1))
            .thenThrow(new StorageException("不支持范围下载"));
        StorageStrategyDecorator<StorageStrategy> d = decorator(delegate);
        assertThrows(StorageException.class, () -> d.downloadRange("b", "k", 0, 1));
    }
}
