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

package top.continew.starter.storage.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.file.FileNameUtil;
import cn.hutool.core.util.StrUtil;
import top.continew.starter.core.constant.StringConstants;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 储存工具
 *
 * @author echo
 * @date 2024/12/16 19:55
 */
public class StorageUtils {
    public StorageUtils() {
    }

    /**
     * 格式文件名
     *
     * @param fileName 文件名
     * @return {@link String }
     */
    public static String formatFileName(String fileName) {
        // 获取文件后缀名
        String suffix = FileUtil.extName(fileName);
        // 获取当前时间的年月日时分秒格式
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String datetime = LocalDateTime.now().format(formatter);
        // 获取当前时间戳
        String timestamp = String.valueOf(System.currentTimeMillis());
        // 生成新的文件名
        return datetime + timestamp + "." + suffix;
    }

    /**
     * 默认文件目录
     *
     * @param fileName 文件名
     * @return {@link String }
     */
    public static String defaultFileDir(String fileName) {
        LocalDate today = LocalDate.now();
        return Paths.get(String.valueOf(today.getYear()), String.valueOf(today.getMonthValue()), String.valueOf(today
            .getDayOfMonth()), fileName).toString();
    }

    /**
     * 本地存储默认路径地址 格式
     * <p>mac/linux ： 2024/03/10/</>
     * <p>windows ： 2024\03\10\</>
     *
     * @return {@link String }
     */
    public static String localDefaultPath() {
        LocalDate today = LocalDate.now();
        return Paths.get(String.valueOf(today.getYear()), String.valueOf(today.getMonthValue()), String.valueOf(today
            .getDayOfMonth())) + StringConstants.SLASH;
    }

    /**
     * 对象存储默认路径 格式 2024/03/10/
     *
     * @return {@link String }
     */
    public static String ossDefaultPath() {
        LocalDate today = LocalDate.now();
        return today.getYear() + StringConstants.SLASH + today.getMonthValue() + StringConstants.SLASH + today
            .getDayOfMonth() + StringConstants.SLASH;
    }

    /**
     * 根据 endpoint 判断是否带有 http 或 https，如果没有则加上 http 前缀。
     *
     * @param endpoint 输入的 endpoint 字符串
     * @return URI 对象
     */
    public static URI createUriWithProtocol(String endpoint) {
        // 判断 endpoint 是否包含 http:// 或 https:// 前缀
        if (!endpoint.startsWith("http://") && !endpoint.startsWith("https://")) {
            // 如果没有协议前缀，则加上 http://
            endpoint = "http://" + endpoint;
        }
        // 返回 URI 对象
        return URI.create(endpoint);
    }

    /**
     * 生成缩略图文件名
     *
     * @param fileName 文件名
     * @param suffix   后缀
     * @return {@link String }
     */
    public static String buildThumbnailFileName(String fileName, String suffix) {
        // 获取文件的扩展名
        String extName = FileNameUtil.extName(fileName);
        // 去掉扩展名
        String baseName = StrUtil.subBefore(fileName, StringConstants.DOT, true);
        // 拼接新的路径：原始路径 + .缩略图后缀 + .扩展名
        return baseName + "." + suffix + "." + extName;
    }

    /**
     * 可重复读流
     *
     * @param inputStream 输入流
     * @return {@link InputStream }
     */
    public static InputStream ensureByteArrayStream(InputStream inputStream) {
        return (inputStream instanceof ByteArrayInputStream)
            ? inputStream
            : new ByteArrayInputStream(IoUtil.readBytes(inputStream));
    }

}
