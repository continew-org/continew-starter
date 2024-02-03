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

package top.charles7c.continew.starter.core.util.db;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库表信息
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class Table implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 表名称
     */
    private String tableName;

    /**
     * 注释
     */
    private String comment;

    /**
     * 存储引擎
     */
    private String engine;

    /**
     * 字符集
     */
    private String charset;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 修改时间
     */
    private LocalDateTime updateTime;

    public Table(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
