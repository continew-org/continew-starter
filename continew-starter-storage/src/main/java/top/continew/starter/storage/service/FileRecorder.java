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

package top.continew.starter.storage.service;

import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.FilePartInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;

import java.util.List;
import java.util.Map;

/**
 * 文件记录器接口，用于保存文件上传记录
 *
 * @author echo
 * @since 2.14.0
 */
public interface FileRecorder {

    /**
     * 保存文件记录
     * 
     * @param fileInfo 文件信息
     * @return 是否保存成功
     */
    boolean save(FileInfo fileInfo);

    /**
     * 更新文件记录
     * 
     * @param fileInfo 文件信息
     * @return 是否更新成功
     */
    boolean update(FileInfo fileInfo);

    /**
     * 删除文件记录
     * 
     * @param platform 存储平台
     * @param path     文件路径
     * @return 是否删除成功
     */
    boolean delete(String platform, String path);

    /**
     * 按 URL 获取文件信息（可选实现）
     *
     * @param url 文件 URL
     * @return 文件信息
     */
    default FileInfo getByUrl(String url) {
        return null;
    }

    /**
     * 保存文件分片信息
     * 
     * @param filePartInfo 文件分片信息
     */
    void saveFilePart(FilePartInfo filePartInfo);

    /**
     * 获取文件所有分片信息
     * 
     * @param fileId 文件ID
     * @return 分片信息列表
     */
    List<FilePartInfo> getFileParts(String fileId);

    /**
     * 删除文件分片信息
     * 
     * @param fileId 文件ID
     */
    void deleteFileParts(String fileId);

    /**
     * 根据文件 MD5 获取 uploadId（可选实现）
     */
    default String getUploadIdByMd5(String md5) {
        return null;
    }

    /**
     * 缓存 MD5 到 uploadId 映射（可选实现）
     */
    default void setMd5Mapping(String md5, String uploadId) {
    }

    /**
     * 删除 MD5 映射（可选实现）
     */
    default void deleteMd5Mapping(String md5) {
    }

    /**
     * 保存分片上传会话（可选实现）
     */
    default void saveMultipartSession(String uploadId, MultipartInitResp initResp, Map<String, String> metadata) {
    }

    /**
     * 获取分片上传会话（可选实现）
     */
    default MultipartInitResp getMultipartSession(String uploadId) {
        return null;
    }

    /**
     * 删除分片上传会话（可选实现）
     */
    default void deleteMultipartSession(String uploadId) {
    }
}
