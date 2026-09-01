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

import cn.hutool.json.JSONUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.common.exception.StorageException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

/**
 * 文件包装器，用于统一处理不同类型的输入
 *
 * @author echo
 * @since 2.14.0
 */
public class FileWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileWrapper.class);

    private MultipartFile multipartFile;
    private byte[] bytes;
    private InputStream inputStream;
    private String originalFilename;
    private String contentType;
    private long size;

    private FileWrapper() {
    }

    /**
     * 从 MultipartFile 创建
     */
    public static FileWrapper of(MultipartFile file) {
        FileWrapper wrapper = new FileWrapper();
        wrapper.multipartFile = file;
        wrapper.originalFilename = file.getOriginalFilename();
        wrapper.contentType = file.getContentType();
        wrapper.size = file.getSize();
        return wrapper;
    }

    /**
     * 从 byte[] 创建
     *
     * @param bytes       字节
     * @param filename    文件名
     * @param contentType 内容类型
     * @return {@link FileWrapper }
     */
    public static FileWrapper of(byte[] bytes, String filename, String contentType) {
        FileWrapper wrapper = createBaseWrapper(filename, contentType);
        wrapper.bytes = bytes;
        wrapper.size = bytes != null ? bytes.length : 0;
        return wrapper;
    }

    /**
     * 从 InputStream 创建
     *
     * @param inputStream 输入流
     * @param filename    文件名
     * @param contentType 内容类型
     * @return {@link FileWrapper }
     */
    public static FileWrapper of(InputStream inputStream, String filename, String contentType) {
        FileWrapper wrapper = createBaseWrapper(filename, contentType);
        wrapper.inputStream = inputStream;
        wrapper.size = -1;
        return wrapper;
    }

    /**
     * 从 Object 创建（智能识别）
     */
    public static FileWrapper of(Object obj) {
        return of(obj, null, null);
    }

    /**
     * 从 Object 创建，可指定文件名和类型
     */
    public static FileWrapper of(Object obj, String filename, String contentType) {
        if (obj == null) {
            throw new StorageException("对象不能为空");
        }

        // 如果是 MultipartFile，直接处理
        if (obj instanceof MultipartFile file) {
            return of(file);
        }

        // 如果是 byte[]
        if (obj instanceof byte[] bytes) {
            return of(bytes, filename, contentType);
        }

        // 如果是 InputStream
        if (obj instanceof InputStream inputStream) {
            return of(inputStream, filename, contentType);
        }

        // 其他对象，转换为 JSON
        String json = convertToJson(obj);
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        String finalFilename = filename != null ? filename : "data.json";
        String finalContentType =
            contentType != null ? contentType : MediaType.APPLICATION_JSON_VALUE;

        return of(jsonBytes, finalFilename, finalContentType);
    }

    /**
     * 创建基本包装器
     *
     * @param filename    文件名
     * @param contentType 内容类型
     * @return {@link FileWrapper }
     */
    private static FileWrapper createBaseWrapper(String filename, String contentType) {
        // 如果没有提供，尝试从请求中获取
        if (filename == null) {
            filename = tryGetFilenameFromRequest();
        }
        if (contentType == null) {
            contentType = tryGetContentTypeFromRequest();
        }

        // 最终校验
        if (filename == null || filename.trim().isEmpty()) {
            throw new StorageException("文件名不能为空");
        }
        if (contentType == null || contentType.trim().isEmpty()) {
            throw new StorageException("文件类型不能为空");
        }

        FileWrapper wrapper = new FileWrapper();
        wrapper.originalFilename = filename;
        wrapper.contentType = contentType;
        return wrapper;
    }

    /**
     * 尝试从当前 HTTP 请求中获取文件名
     */
    private static String tryGetFilenameFromRequest() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();

                // 检查是否是 multipart 请求
                String requestContentType = request.getContentType();
                if (requestContentType != null
                    && requestContentType.toLowerCase().startsWith("multipart/")) {
                    Collection<Part> parts = request.getParts();

                    for (Part part : parts) {
                        String submittedFilename = part.getSubmittedFileName();
                        if (submittedFilename != null && !submittedFilename.trim().isEmpty()) {
                            return submittedFilename;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("从请求中获取文件名时发生异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 尝试从当前 HTTP 请求中获取 ContentType
     */
    private static String tryGetContentTypeFromRequest() {
        try {
            RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
            if (requestAttributes instanceof ServletRequestAttributes servletRequestAttributes) {
                HttpServletRequest request = servletRequestAttributes.getRequest();

                // 检查是否是 multipart 请求
                String requestContentType = request.getContentType();
                if (requestContentType != null
                    && requestContentType.toLowerCase().startsWith("multipart/")) {
                    Collection<Part> parts = request.getParts();

                    for (Part part : parts) {
                        // 只处理文件部分
                        if (part.getSubmittedFileName() != null) {
                            String partContentType = part.getContentType();
                            if (partContentType != null && !partContentType.trim().isEmpty()) {
                                return partContentType;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.debug("从请求中获取 ContentType 时发生异常: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 转换为 MultipartFile
     */
    public MultipartFile toMultipartFile() {
        if (multipartFile != null) {
            return multipartFile;
        }

        if (bytes != null) {
            return new EnhancedMultipartFile(getFilenameWithoutExtension(originalFilename),
                originalFilename,
                contentType, bytes);
        }

        if (inputStream != null) {
            try {
                byte[] data = inputStream.readAllBytes();
                return new EnhancedMultipartFile(getFilenameWithoutExtension(originalFilename),
                    originalFilename,
                    contentType, data);
            } catch (IOException e) {
                throw new StorageException("读取输入流失败", e);
            }
        }

        throw new IllegalStateException("无法转换为 MultipartFile");
    }

    private static String getFilenameWithoutExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(0, lastDotIndex) : filename;
    }

    private static String convertToJson(Object obj) {
        try {
            return JSONUtil.toJsonStr(obj);
        } catch (Exception e) {
            throw new StorageException("对象转换为 JSON 失败", e);
        }
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSize() {
        return size;
    }
}
