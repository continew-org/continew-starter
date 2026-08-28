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

package top.continew.starter.core.util.multipart;

import cn.hutool.core.io.IoUtil;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.core.exception.BaseException;

import java.io.*;
import java.nio.file.Files;

/**
 * MultipartFile 工具类
 *
 * @author Charles7c
 * @since 2.15.0
 */
public class MultipartFileUtils {

    private MultipartFileUtils() {
    }

    /**
     * 转换为 MultipartFile
     *
     * @param file 文件
     * @return MultipartFile
     */
    public static MultipartFile toMultipartFile(File file) throws IOException {
        FileItem fileItem = createFileItem(Files.newInputStream(file.toPath()), file.getName());
        return new CommonsMultipartFile(fileItem);
    }

    /**
     * 转换为 MultipartFile
     *
     * @param bytes    文件字节
     * @param fileName 文件名
     * @return MultipartFile
     */
    public static MultipartFile toMultipartFile(byte[] bytes, String fileName) {
        FileItem fileItem = createFileItem(new ByteArrayInputStream(bytes), fileName);
        return new CommonsMultipartFile(fileItem);
    }

    /**
     * 创建 FileItem
     *
     * @param is       输入流
     * @param fileName 文件名
     * @return FileItem
     */
    public static FileItem createFileItem(InputStream is, String fileName) {
        return createFileItem(is, "file", fileName, MediaType.MULTIPART_FORM_DATA_VALUE);
    }

    /**
     * 创建 FileItem
     *
     * @param is          输入流
     * @param fieldName   字段名
     * @param fileName    文件名
     * @param contentType 内容类型
     * @return FileItem
     */
    public static FileItem createFileItem(InputStream is, String fieldName, String fileName,
        String contentType) {
        DiskFileItemFactory factory = new DiskFileItemFactory();
        FileItem fileItem = factory.createItem(fieldName, contentType, true, fileName);
        // 拷贝流
        try (OutputStream os = fileItem.getOutputStream()) {
            IoUtil.copy(is, os);
        } catch (IOException e) {
            throw new BaseException("创建文件项失败", e);
        } finally {
            IoUtil.close(is);
        }
        return fileItem;
    }
}
