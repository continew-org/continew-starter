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

package top.continew.starter.storage.dao.impl;

import top.continew.starter.storage.dao.StorageDao;
import top.continew.starter.storage.model.resp.UploadResp;

/**
 * 存储记录持久层接口默认实现
 * <p>此类并不能真正保存记录，只是用来脱离数据库运行，保证文件上传功能可以正常使用</p>
 *
 * @author echo
 * @since 2.9.0
 */
public class StorageDaoDefaultImpl implements StorageDao {

    @Override
    public void add(UploadResp uploadResp) {
    }
}
