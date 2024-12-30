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

package top.continew.starter.storage.decorator;

import top.continew.starter.storage.model.resp.ThumbnailResp;
import top.continew.starter.storage.model.resp.UploadResp;
import top.continew.starter.storage.strategy.StorageStrategy;

import java.io.InputStream;

/**
 * 装饰器基类 - 用于重写
 *
 * @author echo
 * @date 2024/12/30 19:33
 */
public abstract class AbstractStorageDecorator<C> implements StorageStrategy<C> {

    protected StorageStrategy<C> delegate;

    protected AbstractStorageDecorator(StorageStrategy<C> delegate) {
        this.delegate = delegate;
    }

    @Override
    public C getClient() {
        return delegate.getClient();
    }

    @Override
    public boolean bucketExists(String bucketName) {
        return delegate.bucketExists(bucketName);
    }

    @Override
    public void createBucket(String bucketName) {
        delegate.createBucket(bucketName);
    }

    @Override
    public UploadResp upload(String fileName, InputStream inputStream, String fileType) {
        return delegate.upload(fileName, inputStream, fileType);
    }

    @Override
    public UploadResp upload(String fileName,
                             String path,
                             InputStream inputStream,
                             String fileType,
                             boolean isThumbnail) {
        return delegate.upload(fileName, path, inputStream, fileType, isThumbnail);
    }

    @Override
    public UploadResp upload(String bucketName,
                             String fileName,
                             String path,
                             InputStream inputStream,
                             String fileType,
                             boolean isThumbnail) {
        return delegate.upload(bucketName, fileName, path, inputStream, fileType, isThumbnail);
    }

    @Override
    public void upload(String bucketName, String fileName, String path, InputStream inputStream, String fileType) {
        delegate.upload(bucketName, fileName, path, inputStream, fileType);
    }

    @Override
    public ThumbnailResp uploadThumbnail(String bucketName,
                                         String fileName,
                                         String path,
                                         InputStream inputStream,
                                         String fileType) {
        return delegate.uploadThumbnail(bucketName, fileName, path, inputStream, fileType);
    }

    @Override
    public InputStream download(String bucketName, String fileName) {
        return delegate.download(bucketName, fileName);
    }

    @Override
    public void delete(String bucketName, String fileName) {
        delegate.delete(bucketName, fileName);
    }

    @Override
    public String getImageBase64(String bucketName, String fileName) {
        return delegate.getImageBase64(bucketName, fileName);
    }
}
