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

package top.continew.starter.storage.strategy;

import top.continew.starter.storage.model.resp.ThumbnailResp;
import top.continew.starter.storage.model.resp.UploadResp;

import java.io.InputStream;

/**
 * 存储策略接口
 *
 * @author echo
 * @date 2024/12/16 11:19
 */
public interface StorageStrategy<C> {

    /**
     * 获得客户端 - 用于重写时 获取对应存储 code 客户端
     *
     * @return {@link Object }
     */
    C getClient();

    /**
     * 检查桶是否存在
     * <p> S3: 检查桶是否存在 </p>
     * <p>local: 检查 默认路径 是否存在 </p>
     *
     * @param bucketName 桶名称
     * @return true 存在 false 不存在
     */
    boolean bucketExists(String bucketName);

    /**
     * 创建桶
     * <p> S3: 创建桶 </p>
     * <p> local: 创建 默认路径下 指定文件夹 </p>
     *
     * @param bucketName 桶名称
     */
    void createBucket(String bucketName);

    /**
     * 上传文件 - 默认桶
     *
     * @param fileName    文件名
     * @param inputStream 输入流
     * @param fileType    文件类型
     * @return 上传响应
     */
    UploadResp upload(String fileName, InputStream inputStream, String fileType);

    /**
     * 上传文件 - 默认桶
     *
     * @param fileName    文件名
     * @param path        路径
     * @param inputStream 输入流
     * @param fileType    文件类型
     * @param isThumbnail 是缩略图
     * @return {@link UploadResp }
     */
    UploadResp upload(String fileName, String path, InputStream inputStream, String fileType, boolean isThumbnail);

    /**
     * 上传文件
     *
     * @param bucketName  桶名称
     * @param fileName    文件名
     * @param path        路径
     * @param inputStream 输入流
     * @param fileType    文件类型
     * @param isThumbnail 是缩略图
     * @return 上传响应
     */
    UploadResp upload(String bucketName,
                      String fileName,
                      String path,
                      InputStream inputStream,
                      String fileType,
                      boolean isThumbnail);

    /**
     * 文件上传-基础上传
     *
     * @param bucketName  桶名称 - 基础上传不做处理
     * @param fileName    文件名 - 基础上传不做处理
     * @param path        路径 - 基础上传不做处理
     * @param inputStream 输入流
     * @param fileType    文件类型
     * @return {@link UploadResp }
     */
    void upload(String bucketName, String fileName, String path, InputStream inputStream, String fileType);

    /**
     * 上传缩略图
     *
     * @param bucketName  桶名称
     * @param fileName    文件名
     * @param inputStream 输入流
     * @param fileType    文件类型
     * @return {@link UploadResp }
     */
    ThumbnailResp uploadThumbnail(String bucketName,
                                  String fileName,
                                  String path,
                                  InputStream inputStream,
                                  String fileType);

    /**
     * 下载文件
     *
     * @param bucketName 桶名称
     * @param fileName   文件名
     * @return 文件输入流
     */
    InputStream download(String bucketName, String fileName);

    /**
     * 删除文件
     *
     * @param bucketName 桶名称
     * @param fileName   文件名
     */
    void delete(String bucketName, String fileName);

    /**
     * 获取图像Base64
     *
     * @param bucketName 桶名称
     * @param fileName   文件名
     * @return Base64编码的图像
     */
    String getImageBase64(String bucketName, String fileName);

}
