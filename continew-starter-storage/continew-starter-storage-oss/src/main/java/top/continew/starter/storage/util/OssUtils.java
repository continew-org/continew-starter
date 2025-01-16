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

package top.continew.starter.storage.util;

import cn.hutool.core.util.StrUtil;
import software.amazon.awssdk.regions.Region;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.storage.constant.StorageConstant;

/**
 * OSS 工具
 *
 * @author echo
 * @date 2024/12/17 13:48
 */
public class OssUtils {
    public OssUtils() {
    }

    /**
     * 获取作用域
     * <p>如果 region 参数非空，使用 Region.of 方法创建对应的 S3 区域对象，否则返回默认区域</p>
     *
     * @param region 区域
     * @return {@link Region }
     */
    public static Region getRegion(String region) {
        return StrUtil.isEmpty(region) ? Region.US_EAST_1 : Region.of(region);
    }

    /**
     * 获取url
     *
     * @param endpoint   端点
     * @param bucketName 桶名称
     * @return {@link String }
     */
    public static String getUrl(String endpoint, String bucketName) {
        // 如果是云服务商，直接返回域名或终端点
        if (StrUtil.containsAny(endpoint, StorageConstant.CLOUD_SERVICE_PREFIX)) {
            return "http://" + bucketName + StringConstants.DOT + endpoint;
        } else {
            return "http://" + endpoint + StringConstants.SLASH + bucketName;
        }
    }

}
