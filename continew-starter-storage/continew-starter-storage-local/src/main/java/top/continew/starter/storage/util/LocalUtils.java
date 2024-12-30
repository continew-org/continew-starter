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

import cn.hutool.core.io.IoUtil;
import net.dreamlu.mica.core.utils.DigestUtil;

import java.io.InputStream;
import java.security.NoSuchAlgorithmException;

/**
 * 本地存储工具
 *
 * @author echo
 * @date 2024/12/27 11:58
 */
public class LocalUtils {
    public LocalUtils() {
    }

    /**
     * 计算MD5
     *
     * @param inputStream 输入流
     * @return {@link String }
     * @throws NoSuchAlgorithmException 没有这样算法例外
     */
    public static String calculateMD5(InputStream inputStream) throws NoSuchAlgorithmException {
        byte[] fileBytes = IoUtil.readBytes(inputStream);
        return DigestUtil.md5Hex(fileBytes);
    }
}
