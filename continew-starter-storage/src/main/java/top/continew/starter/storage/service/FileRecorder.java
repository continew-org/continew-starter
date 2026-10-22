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

import cn.hutool.core.util.StrUtil;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.FilePartInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;

import java.util.List;
import java.util.Map;

/**
 * 文件记录器接口，用于保存文件上传记录
 *
 * <p>
 * MD5 / 会话复用键映射的生命周期（过期时间、清理策略）由实现方自行负责，Starter 不内置 TTL 或兜底清理。
 * </p>
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
     *
     * @param md5 文件 MD5
     * @return 分片上传 ID，未命中返回 null
     */
    default String getUploadIdByMd5(String md5) {
        return null;
    }

    /**
     * 缓存 MD5 到 uploadId 映射（可选实现）
     *
     * @param md5      文件 MD5
     * @param uploadId 分片上传 ID
     */
    default void setMd5Mapping(String md5, String uploadId) {
    }

    /**
     * 删除 MD5 映射（可选实现）
     *
     * @param md5 文件 MD5
     */
    default void deleteMd5Mapping(String md5) {
    }

    /**
     * 根据会话复用键获取 uploadId（可选实现）
     *
     * <p>
     * 会话复用键由 Starter 按文件 MD5、存储平台、存储桶、原始文件名与上级目录派生，对实现方不透明。
     * 默认实现退化为 {@link #getUploadIdByMd5(String)}，实现方通常无需覆写。
     * </p>
     *
     * @param sessionKey 会话复用键
     * @return 分片上传 ID，未命中返回 null
     */
    default String getUploadIdBySessionKey(String sessionKey) {
        return getUploadIdByMd5(sessionKey);
    }

    /**
     * 缓存会话复用键到 uploadId 的映射（可选实现）
     *
     * <p>
     * 默认实现退化为 {@link #setMd5Mapping(String, String)}，实现方通常无需覆写。
     * </p>
     *
     * @param sessionKey 会话复用键
     * @param uploadId   分片上传 ID
     */
    default void setSessionKeyMapping(String sessionKey, String uploadId) {
        setMd5Mapping(sessionKey, uploadId);
    }

    /**
     * 删除会话复用键的映射（可选实现）
     *
     * <p>
     * 默认实现退化为 {@link #deleteMd5Mapping(String)}，实现方通常无需覆写。
     * </p>
     *
     * @param sessionKey 会话复用键
     */
    default void deleteSessionKeyMapping(String sessionKey) {
        deleteMd5Mapping(sessionKey);
    }

    /**
     * 仅当会话复用键仍映射到指定 uploadId 时删除（可选实现）
     *
     * <p>
     * 默认实现为先查后删，属于尽力而为：查询与删除之间仍存在极小的竞态窗口；需要强一致的实现方应覆写为
     * 存储层的原子比较删除（如 Redis Lua 脚本、数据库条件 DELETE）。映射的生命周期（过期、清理）由实现方
     * 负责，见类注释。
     * </p>
     *
     * @param sessionKey 会话复用键
     * @param uploadId   预期映射的分片上传 ID
     * @return 是否执行了删除
     */
    default boolean deleteSessionKeyMapping(String sessionKey, String uploadId) {
        if (StrUtil.equals(uploadId, getUploadIdBySessionKey(sessionKey))) {
            deleteSessionKeyMapping(sessionKey);
            return true;
        }
        return false;
    }

    /**
     * 保存分片上传会话（可选实现）
     *
     * @param uploadId 分片上传 ID
     * @param initResp 分片上传初始化结果
     * @param metadata 元数据
     */
    default void saveMultipartSession(String uploadId, MultipartInitResp initResp,
        Map<String, String> metadata) {
    }

    /**
     * 获取分片上传会话（可选实现）
     *
     * @param uploadId 分片上传 ID
     * @return 分片上传初始化信息，不存在返回 null
     */
    default MultipartInitResp getMultipartSession(String uploadId) {
        return null;
    }

    /**
     * 删除分片上传会话（可选实现）
     *
     * @param uploadId 分片上传 ID
     */
    default void deleteMultipartSession(String uploadId) {
    }
}
