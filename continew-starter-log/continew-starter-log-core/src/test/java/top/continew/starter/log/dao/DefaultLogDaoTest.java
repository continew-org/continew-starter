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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.continew.starter.log.model.LogRecord;

import java.time.Instant;
import java.util.List;

/**
 * {@link DefaultLogDao} 内存持久层测试
 *
 * @author Charles7c
 * @since 3.1.0
 */
class DefaultLogDaoTest {

    /**
     * 新建的 DAO 应返回空日志列表
     */
    @Test
    @DisplayName("新建 DAO 列表为空")
    void listShouldBeEmptyForNewDao() {
        DefaultLogDao dao = new DefaultLogDao();
        assertThat(dao.list()).isEmpty();
    }

    /**
     * 记录的日志应可从列表中查询到
     */
    @Test
    @DisplayName("记录的日志可查询到")
    void addedRecordShouldBeListed() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.add(newRecord("新增用户"));
        assertThat(dao.list()).extracting(LogRecord::getDescription).containsExactly("新增用户");
    }

    /**
     * 默认降序排列：后记录的日志排在最前
     */
    @Test
    @DisplayName("默认降序：最新记录在前")
    void listShouldBeNewestFirstByDefault() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.add(newRecord("第一条"));
        dao.add(newRecord("第二条"));
        assertThat(dao.list()).extracting(LogRecord::getDescription)
            .containsExactly("第二条", "第一条");
    }

    /**
     * 升序模式：按插入顺序排列
     */
    @Test
    @DisplayName("升序模式按插入顺序排列")
    void listShouldKeepInsertionOrderWhenReverseDisabled() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.setReverse(false);
        dao.add(newRecord("第一条"));
        dao.add(newRecord("第二条"));
        assertThat(dao.list()).extracting(LogRecord::getDescription)
            .containsExactly("第一条", "第二条");
    }

    /**
     * 默认降序下超容量淘汰最旧记录，保留最新 N 条
     */
    @Test
    @DisplayName("降序下超容量淘汰最旧记录")
    void shouldDropOldestRecordWhenCapacityExceededInReverseMode() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.setCapacity(2);
        dao.add(newRecord("第一条"));
        dao.add(newRecord("第二条"));
        dao.add(newRecord("第三条"));
        assertThat(dao.list()).extracting(LogRecord::getDescription)
            .containsExactly("第三条", "第二条");
    }

    /**
     * 升序下超容量淘汰队首记录，保留最新 N 条
     */
    @Test
    @DisplayName("升序下超容量淘汰队首记录")
    void shouldDropHeadRecordWhenCapacityExceededInAscendMode() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.setReverse(false);
        dao.setCapacity(2);
        dao.add(newRecord("第一条"));
        dao.add(newRecord("第二条"));
        dao.add(newRecord("第三条"));
        assertThat(dao.list()).extracting(LogRecord::getDescription)
            .containsExactly("第二条", "第三条");
    }

    /**
     * 运行中缩减容量：下次写入时按新容量裁剪
     */
    @Test
    @DisplayName("缩减容量后下次写入裁剪到新容量")
    void shouldTrimToNewCapacityAfterCapacityShrunk() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.setCapacity(2);
        dao.add(newRecord("第一条"));
        dao.add(newRecord("第二条"));
        dao.setCapacity(1);
        dao.add(newRecord("第三条"));
        assertThat(dao.list()).extracting(LogRecord::getDescription).containsExactly("第三条");
    }

    /**
     * 锁定当前行为：list 返回不可变快照，外部修改抛 UnsupportedOperationException
     */
    @Test
    @DisplayName("list 返回不可变快照（锁定当前行为）")
    void listShouldReturnImmutableSnapshot() {
        DefaultLogDao dao = new DefaultLogDao();
        dao.add(newRecord("新增用户"));
        List<LogRecord> snapshot = dao.list();
        LogRecord externalRecord = newRecord("外部追加");
        assertThatThrownBy(() -> snapshot.add(externalRecord))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    private LogRecord newRecord(String description) {
        LogRecord logRecord = new LogRecord(Instant.now(), null, null, null);
        logRecord.setDescription(description);
        return logRecord;
    }
}
