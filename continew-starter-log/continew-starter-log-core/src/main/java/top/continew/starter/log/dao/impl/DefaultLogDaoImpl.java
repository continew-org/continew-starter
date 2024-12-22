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

package top.continew.starter.log.dao.impl;

import top.continew.starter.log.dao.LogDao;
import top.continew.starter.log.model.LogRecord;

import java.util.LinkedList;
import java.util.List;

/**
 * 日志持久层接口默认实现类（基于内存）
 *
 * @author Dave Syer（Spring Boot Actuator）
 * @author Olivier Bourgain（Spring Boot Actuator）
 * @author Charles7c
 * @since 1.1.0
 */
public class DefaultLogDaoImpl implements LogDao {

    /**
     * 容量
     */
    private int capacity = 100;

    /**
     * 是否降序
     */
    private boolean reverse = true;

    /**
     * 日志列表
     */
    private final List<LogRecord> logRecords = new LinkedList<>();

    @Override
    public List<LogRecord> list() {
        synchronized (this.logRecords) {
            return List.copyOf(this.logRecords);
        }
    }

    @Override
    public void add(LogRecord logRecord) {
        synchronized (this.logRecords) {
            while (this.logRecords.size() >= this.capacity) {
                this.logRecords.remove(this.reverse ? this.capacity - 1 : 0);
            }
            if (this.reverse) {
                this.logRecords.add(0, logRecord);
            } else {
                this.logRecords.add(logRecord);
            }
        }
    }

    /**
     * 设置内存中存储的最大日志容量
     *
     * @param capacity 容量
     */
    public void setCapacity(int capacity) {
        synchronized (this.logRecords) {
            this.capacity = capacity;
        }
    }

    /**
     * 设置是否降序
     *
     * @param reverse 是否降序（默认：true）
     */
    public void setReverse(boolean reverse) {
        synchronized (this.logRecords) {
            this.reverse = reverse;
        }
    }
}