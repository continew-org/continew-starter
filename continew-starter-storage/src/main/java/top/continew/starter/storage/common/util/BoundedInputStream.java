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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 限长输入流
 *
 * <p>
 * 最多代理 {@code limit} 字节供下游读取。默认软限：达到上限返回 -1 视同 EOF；
 * 硬限模式（{@link #BoundedInputStream(InputStream, long, boolean)}）下底层还有
 * 剩余数据时抛 {@link IOException}，用于按读取侧严格限制内容长度。不主动关闭底层流。
 * </p>
 *
 * @author echo
 * @since 3.1.0
 */
public class BoundedInputStream extends FilterInputStream {

    private final long limit;
    private final boolean hard;
    private long count;
    private boolean probedBeyond;

    /**
     * 构造软限长输入流
     *
     * @param in    底层输入流
     * @param limit 可读取字节上限（必须为非负数）
     */
    public BoundedInputStream(InputStream in, long limit) {
        this(in, limit, false);
    }

    /**
     * 构造限长输入流
     *
     * @param in    底层输入流
     * @param limit 可读取字节上限（必须为非负数）
     * @param hard  是否硬限：底层超过上限时抛 IOException 而非静默截断
     */
    public BoundedInputStream(InputStream in, long limit, boolean hard) {
        super(in);
        this.limit = limit;
        this.hard = hard;
    }

    /**
     * 已代理读取的字节数
     *
     * @return 已读字节数
     */
    public long getCount() {
        return count;
    }

    @Override
    public int read() throws IOException {
        if (count >= limit) {
            return beyond();
        }
        int b = super.read();
        if (b != -1) {
            count++;
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        if (count >= limit) {
            return beyond();
        }
        int toRead = (int) Math.min(len, limit - count);
        int n = super.read(b, off, toRead);
        if (n > 0) {
            count += n;
        }
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        if (count >= limit) {
            return 0;
        }
        long skipped = super.skip(Math.min(n, limit - count));
        count += skipped;
        return skipped;
    }

    /**
     * 到达上限后的收尾：软限返回 EOF，硬限探测底层是否仍有剩余数据
     *
     * @return -1 视同 EOF
     * @throws IOException 硬限模式下底层仍有未读数据时抛出
     */
    private int beyond() throws IOException {
        if (!hard || probedBeyond) {
            return -1;
        }
        probedBeyond = true;
        if (super.read() != -1) {
            throw new IOException("内容长度超过限制: " + limit + " 字节");
        }
        return -1;
    }
}
