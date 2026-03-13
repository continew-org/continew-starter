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

package top.continew.starter.storage.common.constant;

import top.continew.starter.core.constant.StringConstants;

/**
 * 存储常数
 *
 * @author echo
 * @since 2.14.0
 */
public class StorageConstant {

    /**
     * 默认存储平台
     */
    public static final String DEFAULT_STORAGE_PLATFORM = "local";

    /**
     * 配置文件
     */
    public static final String CONFIG = "CONFIG";

    /**
     * 动态配置
     */
    public static final String DYNAMIC = "DYNAMIC";

    /**
     * 默认文件大小
     */
    public static final Long DEFAULT_FILE_SIZE = 1024 * 1024 * 10L;

    /**
     * 默认分片阈值（字节）
     */
    public static final Long DEFAULT_MULTIPART_UPLOAD_THRESHOLD = DEFAULT_FILE_SIZE;

    /**
     * 默认分片大小（字节）
     */
    public static final Long DEFAULT_MULTIPART_UPLOAD_PART_SIZE = 5 * 1024 * 1024L;

    /**
     * 本地分片上传临时目录
     */
    public static final String DEFAULT_LOCAL_MULTIPART_TEMP_DIR = "/tmp";

    /**
     * 默认的对象ACL
     */
    public static final String DEFAULT_ACL = "private";

    /**
     * 缩略图后缀
     */
    public static final String THUMBNAIL_SUFFIX = StringConstants.DOT + "thumb" + StringConstants.DOT;

    /**
     * ContentType 图片前缀
     */
    public static final String CONTENT_TYPE_IMAGE = "image/";

}
