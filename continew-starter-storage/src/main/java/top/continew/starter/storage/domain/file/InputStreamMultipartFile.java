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

import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * 基于单次输入流的 MultipartFile
 *
 * <p>
 * {@link #getInputStream()} 仅允许调用一次，返回底层流本身；长度未知时 {@link #getSize()}
 * 返回 -1。适合真实流式上传场景，避免把内容缓冲进内存。流由服务层在上传完成后关闭。
 * </p>
 *
 * @author echo
 * @since 3.1.0
 */
public class InputStreamMultipartFile implements MultipartFile {

    private final String name;
    private final String originalFilename;
    private final String contentType;
    private final long size;
    private final InputStream inputStream;
    private volatile boolean streamConsumed;
    private volatile boolean streamClosed;
    private volatile long actualSize = -1;

    /**
     * 构造单次读流式文件
     *
     * @param name             参数名
     * @param originalFilename 原始文件名
     * @param contentType      内容类型
     * @param size             内容长度，未知时传 -1
     * @param inputStream      内容输入流
     */
    public InputStreamMultipartFile(String name, String originalFilename, String contentType,
        long size, InputStream inputStream) {
        this.name = name;
        this.originalFilename = originalFilename;
        this.contentType = contentType;
        this.size = size;
        this.inputStream = inputStream;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getOriginalFilename() {
        return originalFilename;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public long getSize() {
        return size;
    }

    @Override
    public byte[] getBytes() throws IOException {
        throw new IOException("流式文件不支持 getBytes");
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (streamConsumed) {
            throw new IOException("流式文件内容不可重复读取");
        }
        streamConsumed = true;
        return inputStream;
    }

    @Override
    public void transferTo(File dest) throws IOException, IllegalStateException {
        throw new IOException("流式文件不支持 transferTo");
    }

    /**
     * 上传过程中实际读取的字节数（未读取或未知时为 -1）
     *
     * @return 实际读取字节数
     */
    public long getActualSize() {
        return actualSize;
    }

    /**
     * 记录实际读取字节数
     *
     * @param actualSize 实际读取字节数
     */
    public void setActualSize(long actualSize) {
        this.actualSize = actualSize;
    }

    /**
     * 关闭底层输入流（幂等，底层流被外层包装关闭时同样生效）
     *
     * @throws IOException 关闭失败时抛出
     */
    public void closeStream() throws IOException {
        if (!streamClosed) {
            streamClosed = true;
            inputStream.close();
        }
    }
}
