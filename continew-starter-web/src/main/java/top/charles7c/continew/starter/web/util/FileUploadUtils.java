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

package top.charles7c.continew.starter.web.util;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.IdUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
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
     * @param request    请求对象
     * @param response   响应对象
     * @param file       文件
     * @param autoDelete 下载后自动删除
     */
    public static void download(HttpServletRequest request,
                                HttpServletResponse response,
                                File file,
                                boolean autoDelete) {
        response.setCharacterEncoding(request.getCharacterEncoding());
        response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
        try (FileInputStream fis = new FileInputStream(file)) {
            IoUtil.copy(fis, response.getOutputStream());
            response.flushBuffer();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (autoDelete) {
                file.deleteOnExit();
            }
        }
    }
}
