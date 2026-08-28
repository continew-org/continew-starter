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

package top.continew.starter.storage.service.impl;

import top.continew.starter.storage.domain.model.resp.FileInfo;
import top.continew.starter.storage.domain.model.resp.FilePartInfo;
import top.continew.starter.storage.service.FileRecorder;

import java.util.List;

/**
 * 默认文件记录器
 *
 * @author echo
 * @since 2.14.0
 */
public class DefaultFileRecorder implements FileRecorder {

    @Override
    public boolean save(FileInfo fileInfo) {
        return false;
    }

    @Override
    public boolean update(FileInfo fileInfo) {
        return false;
    }

    @Override
    public boolean delete(String platform, String path) {
        return false;
    }

    @Override
    public void saveFilePart(FilePartInfo filePartInfo) {

    }

    @Override
    public List<FilePartInfo> getFileParts(String fileId) {
        return List.of();
    }

    @Override
    public void deleteFileParts(String fileId) {

    }
}
