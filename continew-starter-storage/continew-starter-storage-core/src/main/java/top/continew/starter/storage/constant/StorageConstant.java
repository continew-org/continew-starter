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

package top.continew.starter.storage.constant;

/**
 * 存储 常量
 *
 * @author echo
 * @date 2024/12/16 19:09
 */
public class StorageConstant {

    /**
     * 默认存储 Key
     */
    public static final String DEFAULT_KEY = "storage:default_config";

    /**
     * 云服务商 域名前缀
     * <p>目前只支持 阿里云-oss 华为云-obs 腾讯云-cos</p>
     */
    public static final String[] CLOUD_SERVICE_PREFIX = new String[] {"oss", "cos", "obs"};

    /**
     * 缩略图后缀
     */
    public static final String SMALL_SUFFIX = "small";
}
