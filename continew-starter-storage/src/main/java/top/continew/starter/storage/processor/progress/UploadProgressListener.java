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

package top.continew.starter.storage.processor.progress;

/**
 * 上传进度监听器
 *
 * @author echo
 * @since 2.14.0
 **/
public interface UploadProgressListener {

    /**
     * 进度更新回调
     *
     * @param bytesRead  已读取字节数
     * @param totalBytes 总字节数（-1表示未知）
     * @param percentage 百分比（0-100）
     */
    void onProgress(long bytesRead, long totalBytes, int percentage);

    /**
     * 上传开始
     */
    default void onStart() {
    }

    /**
     * 上传完成
     */
    default void onComplete() {
    }

    /**
     * 上传失败
     *
     * @param e 异常
     */
    default void onError(Exception e) {
    }
}
