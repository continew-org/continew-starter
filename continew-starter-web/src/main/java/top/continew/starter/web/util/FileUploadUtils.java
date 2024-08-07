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

package top.continew.starter.web.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.URLUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * 文件工具类
 *
 * @author Zheng Jie（<a href="https://gitee.com/elunez/eladmin">ELADMIN</a>）
 * @author Charles7c
 * @since 1.0.0
 */
public class FileUploadUtils {
    private static final Logger log = LoggerFactory.getLogger(FileUploadUtils.class);

    private FileUploadUtils() {
    }

    /**
     * 上传
     *
     * @param multipartFile          源文件对象
     * @param filePath               文件路径
     * @param isKeepOriginalFilename 是否保留原文件名
     * @return 目标文件对象
     */
    public static File upload(MultipartFile multipartFile, String filePath, boolean isKeepOriginalFilename) {
        String originalFilename = multipartFile.getOriginalFilename();
        String extensionName = FileNameUtil.extName(originalFilename);
        String fileName;
        if (isKeepOriginalFilename) {
            fileName = "%s-%s.%s".formatted(FileNameUtil.getPrefix(originalFilename), DateUtil.format(LocalDateTime
                .now(), DatePattern.PURE_DATETIME_MS_PATTERN), extensionName);
        } else {
            fileName = "%s.%s".formatted(IdUtil.fastSimpleUUID(), extensionName);
        }
        try {
            String pathname = filePath + fileName;
            File dest = new File(pathname).getCanonicalFile();
            // 如果父路径不存在，自动创建
            if (!dest.getParentFile().exists() && (!dest.getParentFile().mkdirs())) {
                log.error("Create upload file parent path failed.");
            }
            // 文件写入
            multipartFile.transferTo(dest);
            return dest;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * 下载
     *
     * @param response 响应对象
     * @param file     文件
     */
    public static void download(HttpServletResponse response, File file) throws IOException {
        download(response, new FileInputStream(file), file.getName());
    }

    /**
     * 下载
     *
     * @param response    响应对象
     * @param inputStream 文件流
     * @param fileName    文件名
     * @since 2.5.0
     */
    public static void download(HttpServletResponse response,
                                InputStream inputStream,
                                String fileName) throws IOException {
        byte[] bytes = IoUtil.readBytes(inputStream);
        response.setCharacterEncoding(StandardCharsets.UTF_8.toString());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setContentLength(bytes.length);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + URLUtil.encode(fileName));
        IoUtil.write(response.getOutputStream(), true, bytes);
    }
}
