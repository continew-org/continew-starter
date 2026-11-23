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

package top.continew.starter.storage.domain.model.resp;

import java.util.ArrayList;
import java.util.List;

/**
 * ZIP 导入结果
 *
 * @author echo
 * @since 3.1.0
 */
public class ZipImportResp {

    /**
     * 已扫描条目总数（含目录条目）
     */
    private long total;

    /**
     * 成功条数
     */
    private long success;

    /**
     * 失败条数
     */
    private long failed;

    /**
     * 逐条明细
     */
    private final List<Entry> entries = new ArrayList<>();

    /**
     * 记录一条导入明细并同步成功/失败计数
     *
     * @param name    原始条目名
     * @param success 是否成功
     * @param size    解压后字节数
     * @param error   失败原因（成功为 null）
     */
    public void addEntry(String name, boolean success, long size, String error) {
        entries.add(new Entry(name, success, size, error));
        if (success) {
            this.success++;
        } else {
            this.failed++;
        }
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getSuccess() {
        return success;
    }

    public long getFailed() {
        return failed;
    }

    public List<Entry> getEntries() {
        return entries;
    }

    /**
     * 单条导入明细
     */
    public static class Entry {

        /**
         * 原始条目名
         */
        private final String name;

        /**
         * 是否导入成功
         */
        private final boolean success;

        /**
         * 解压后字节数（未能计量时为 0）
         */
        private final long size;

        /**
         * 失败原因（成功为 null）
         */
        private final String error;

        public Entry(String name, boolean success, long size, String error) {
            this.name = name;
            this.success = success;
            this.size = size;
            this.error = error;
        }

        public String getName() {
            return name;
        }

        public boolean isSuccess() {
            return success;
        }

        public long getSize() {
            return size;
        }

        public String getError() {
            return error;
        }
    }
}
