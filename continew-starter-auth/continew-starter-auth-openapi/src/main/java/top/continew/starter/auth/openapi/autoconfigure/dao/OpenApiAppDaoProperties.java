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

package top.continew.starter.auth.openapi.autoconfigure.dao;

import java.time.Duration;

/**
 * 开放 API DAO 配置属性
 *
 * @author Charles7c
 * @since 2.16.0
 */
public class OpenApiAppDaoProperties {

    /**
     * 类型
     */
    private DaoType type = DaoType.DEFAULT;

    /**
     * 前缀
     * <p>
     * 目前仅 {@link #type DaoType.REDIS} 生效
     * </p>
     */
    private String prefix = "CONTINEW-STARTER::OPENAPI::";

    /**
     * 超时时长
     * <p>
     * 目前仅 {@link #type DaoType.REDIS} 生效
     * </p>
     */
    private Duration timeout = Duration.ofMinutes(3);

    public DaoType getType() {
        return type;
    }

    public void setType(DaoType type) {
        this.type = type;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }

    /**
     * DAO 类型枚举
     */
    public enum DaoType {

        /**
         * 使用内存
         */
        DEFAULT,

        /**
         * 使用 Redis
         */
        REDIS,

        /**
         * 自定义
         */
        CUSTOM
    }
}
