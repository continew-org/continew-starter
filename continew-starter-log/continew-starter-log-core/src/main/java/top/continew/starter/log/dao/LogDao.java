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

package top.continew.starter.log.dao;

import top.continew.starter.log.model.LogRecord;

import java.util.Collections;
import java.util.List;

/**
 * 日志持久层接口
 *
 * @author Charles7c
 * @since 1.1.0
 */
public interface LogDao {

    /**
     * 查询日志列表
     *
     * @return 日志列表
     */
    default List<LogRecord> list() {
        return Collections.emptyList();
    }

    /**
     * 记录日志
     *
     * @param logRecord 日志信息
     */
    void add(LogRecord logRecord);
}
