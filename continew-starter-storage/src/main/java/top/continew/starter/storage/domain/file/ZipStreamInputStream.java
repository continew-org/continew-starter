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

import top.continew.starter.storage.common.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 按需生成的 ZIP 输入流
 *
 * <p>
 * 采用拉取模式：调用方每 {@code read()} 一次，内部才把存储源文件内容写入 ZIP 结构，
 * 内存占用以复制缓冲区（8KB）与压缩器内部窗口为界，不依赖后台线程或执行器，
 * 也不会因为消费者缓慢而无限堆积。条目名在构造时预规范化去重，非法名在打开该条目时
 * 触发 {@link StorageException}。
 * </p>
 *
 * @author echo
 * @since 3.1.0
 */
public class ZipStreamInputStream extends InputStream {

    private static final int COPY_BUFFER_SIZE = 8192;

    private final Iterator<String> paths;
    private final Function<String, InputStream> opener;
    private final Function<String, String> entryNamer;
    private final ChunkCollectingOutputStream sink = new ChunkCollectingOutputStream();
    private final ZipOutputStream zipOut = new ZipOutputStream(sink);
    private final byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
    private final byte[] singleByte = new byte[1];

    private Deque<byte[]> pending;
    private byte[] currentChunk;
    private int currentOffset;
    private InputStream currentEntryStream;
    private boolean finished;

    /**
     * 构造按需生成的 ZIP 输入流
     *
     * @param paths      源路径列表（按顺序写入 ZIP）
     * @param opener     按路径打开源内容流，由本流负责关闭
     * @param entryNamer 由路径映射 ZIP 条目名（含去重）
     */
    public ZipStreamInputStream(List<String> paths, Function<String, InputStream> opener,
        Function<String, String> entryNamer) {
        this.paths = paths.iterator();
        this.opener = opener;
        this.entryNamer = entryNamer;
        this.pending = new ArrayDeque<>();
    }

    @Override
    public int read() throws IOException {
        int n = read(singleByte, 0, 1);
        return n <= 0 ? -1 : singleByte[0] & 0xFF;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        Objects.checkFromIndexSize(off, len, b.length);
        if (len == 0) {
            return 0;
        }
        if (currentChunk == null) {
            nextChunk();
            if (currentChunk == null) {
                return -1;
            }
        }
        int n = Math.min(len, currentChunk.length - currentOffset);
        System.arraycopy(currentChunk, currentOffset, b, off, n);
        currentOffset += n;
        if (currentOffset == currentChunk.length) {
            currentChunk = null;
        }
        return n;
    }

    @Override
    public int available() {
        return currentChunk == null ? 0 : currentChunk.length - currentOffset;
    }

    @Override
    public void close() throws IOException {
        finished = true;
        try {
            closeCurrentEntryStream();
        } finally {
            // 即使源流关闭失败也必须释放 Deflater 本地资源；汇为内存队列，仅清空即可
            zipOut.close();
            pending.clear();
            currentChunk = null;
        }
    }

    /**
     * 取下一个已生成的 ZIP 数据块；块队列耗尽时驱动 ZIP 生成器继续写入
     *
     * @throws IOException 生成或读取源内容失败时抛出
     */
    private void nextChunk() throws IOException {
        while (currentChunk == null) {
            byte[] chunk = pending.poll();
            if (chunk != null && chunk.length > 0) {
                currentChunk = chunk;
                currentOffset = 0;
                continue;
            }
            if (chunk != null) {
                continue;
            }
            if (finished || !fill()) {
                return;
            }
        }
    }

    /**
     * 驱动 ZIP 生成：持续向 ZipOutputStream 喂入源内容直到产出至少一个压缩数据块或写完
     *
     * @return 是否仍可继续生成（返回 false 表示整个 ZIP 写完）
     * @throws IOException 读写失败时抛出
     */
    private boolean fill() throws IOException {
        while (!finished && pending.isEmpty()) {
            if (currentEntryStream != null) {
                int n = currentEntryStream.read(copyBuffer);
                if (n >= 0) {
                    zipOut.write(copyBuffer, 0, n);
                    continue;
                }
                closeCurrentEntryStream();
                zipOut.closeEntry();
                continue;
            }
            if (paths.hasNext()) {
                String path = paths.next();
                zipOut.putNextEntry(new ZipEntry(entryNamer.apply(path)));
                currentEntryStream = opener.apply(path);
                continue;
            }
            zipOut.finish();
            finished = true;
        }
        return !pending.isEmpty();
    }

    /**
     * 关闭当前条目源流
     *
     * @throws IOException 关闭失败时抛出
     */
    private void closeCurrentEntryStream() throws IOException {
        if (currentEntryStream == null) {
            return;
        }
        try {
            currentEntryStream.close();
        } finally {
            currentEntryStream = null;
        }
    }

    /**
     * 把 ZIP 压缩输出收集为数据块队列的输出汇
     */
    private class ChunkCollectingOutputStream extends OutputStream {

        private final byte[] single = new byte[1];

        @Override
        public void write(int b) {
            single[0] = (byte) b;
            write(single, 0, 1);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            byte[] chunk = new byte[len];
            System.arraycopy(b, off, chunk, 0, len);
            pending.add(chunk);
        }
    }
}
