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

package top.continew.starter.storage.strategy.impl;

import org.springframework.web.multipart.MultipartFile;
import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.MultipartInitResp;
import top.continew.starter.storage.domain.model.resp.MultipartUploadResp;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * 存储策略装饰器基类
 * 支持对特定存储策略进行选择性重写
 *
 * @author echo
 * @since 2.14.0
 */
public abstract class StorageStrategyDecorator<T extends StorageStrategy>
    implements StorageStrategy {

    protected T delegate;

    /**
     * 获取被装饰的策略类型
     */
    public abstract Class<T> getTargetStrategyClass();

    /**
     * 设置被装饰的策略实例
     */
    public void setDelegate(T delegate) {
        this.delegate = delegate;
    }

    /**
     * 获取被装饰的策略实例
     */
    protected T getDelegate() {
        if (delegate == null) {
            throw new IllegalStateException("装饰器未初始化，请先设置delegate");
        }
        return delegate;
    }

    /**
     * 获取装饰器优先级（数值越小优先级越高）
     */
    public int getOrder() {
        return 0;
    }

    @Override
    public void upload(String bucket, String path, MultipartFile file) {
        getDelegate().upload(bucket, path, file);
    }

    @Override
    public InputStream download(String bucket, String path) {
        return getDelegate().download(bucket, path);
    }

    @Override
    public InputStream batchDownload(String bucket, List<String> paths) {
        return getDelegate().batchDownload(bucket, paths);
    }

    @Override
    public void delete(String bucket, String path) {
        getDelegate().delete(bucket, path);
    }

    @Override
    public void batchDelete(String bucket, List<String> paths) {
        getDelegate().batchDelete(bucket, paths);
    }

    @Override
    public boolean exists(String bucket, String path) {
        return getDelegate().exists(bucket, path);
    }

    @Override
    public FileInfo getFileInfo(String bucket, String path) {
        return getDelegate().getFileInfo(bucket, path);
    }

    @Override
    public List<FileInfo> list(String bucket, String prefix, int maxKeys) {
        return getDelegate().list(bucket, prefix, maxKeys);
    }

    @Override
    public void copy(String sourceBucket, String targetBucket, String sourcePath,
        String targetPath) {
        getDelegate().copy(sourceBucket, targetBucket, sourcePath, targetPath);
    }

    @Override
    public void move(String sourceBucket, String targetBucket, String sourcePath,
        String targetPath) {
        getDelegate().move(sourceBucket, targetBucket, sourcePath, targetPath);
    }

    @Override
    public String getPlatform() {
        return getDelegate().getPlatform();
    }

    @Override
    public String defaultBucket() {
        return getDelegate().defaultBucket();
    }

    @Override
    public String generatePresignedUrl(String bucket, String path, long expireSeconds) {
        return getDelegate().generatePresignedUrl(bucket, path, expireSeconds);
    }

    @Override
    public String generateUploadPresignedUrl(String bucket, String path, long expireSeconds) {
        return getDelegate().generateUploadPresignedUrl(bucket, path, expireSeconds);
    }

    @Override
    public MultipartInitResp initMultipartUpload(String bucket,
        String path,
        String contentType,
        Map<String, String> metadata) {
        return getDelegate().initMultipartUpload(bucket, path, contentType, metadata);
    }

    @Override
    public MultipartUploadResp uploadPart(String bucket,
        String path,
        String uploadId,
        int partNumber,
        InputStream data) {
        return getDelegate().uploadPart(bucket, path, uploadId, partNumber, data);
    }

    @Override
    public FileInfo completeMultipartUpload(String bucket,
        String path,
        String uploadId,
        List<MultipartUploadResp> parts,
        boolean verifyParts) {
        return getDelegate().completeMultipartUpload(bucket, path, uploadId, parts, verifyParts);
    }

    @Override
    public void abortMultipartUpload(String bucket, String path, String uploadId) {
        getDelegate().abortMultipartUpload(bucket, path, uploadId);
    }

    @Override
    public List<MultipartUploadResp> listParts(String bucket, String path, String uploadId) {
        return getDelegate().listParts(bucket, path, uploadId);
    }

    @Override
    public void cleanup() {
        getDelegate().cleanup();
    }
}
