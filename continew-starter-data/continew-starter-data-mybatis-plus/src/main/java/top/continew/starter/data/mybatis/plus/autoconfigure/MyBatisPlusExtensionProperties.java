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

package top.continew.starter.data.mybatis.plus.autoconfigure;

import com.baomidou.mybatisplus.annotation.DbType;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import top.continew.starter.data.mybatis.plus.autoconfigure.idgenerator.MyBatisPlusIdGeneratorProperties;

/**
 * MyBatis Plus 扩展配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "mybatis-plus.extension")
public class MyBatisPlusExtensionProperties {

    /**
     * 是否启用扩展
     */
    private boolean enabled = false;

    /**
     * Mapper 接口扫描包（配置时必须使用：mapper-package 键名）
     * <p>
     * e.g. com.example.**.mapper
     * </p>
     */
    private String mapperPackage;

    /**
     * ID 生成器
     */
    @NestedConfigurationProperty
    private MyBatisPlusIdGeneratorProperties idGenerator;

    /**
     * 数据权限插件配置
     */
    private DataPermissionProperties dataPermission;

    /**
     * 分页插件配置
     */
    private PaginationProperties pagination;

    /**
     * 启用防全表更新与删除插件
     */
    private boolean blockAttackPluginEnabled = true;

    /**
     * 数据权限插件配置属性
     */
    public static class DataPermissionProperties {

        /**
         * 是否启用数据权限插件
         */
        private boolean enabled = false;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }

    /**
     * 分页插件配置属性
     */
    public static class PaginationProperties {

        /**
         * 是否启用分页插件
         */
        private boolean enabled = true;

        /**
         * 数据库类型
         */
        private DbType dbType;

        /**
         * 是否溢出处理
         */
        private boolean overflow = false;

        /**
         * 单页分页条数限制（默认：-1 表示无限制）
         */
        private Long maxLimit = -1L;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public DbType getDbType() {
            return dbType;
        }

        public void setDbType(DbType dbType) {
            this.dbType = dbType;
        }

        public boolean isOverflow() {
            return overflow;
        }

        public void setOverflow(boolean overflow) {
            this.overflow = overflow;
        }

        public Long getMaxLimit() {
            return maxLimit;
        }

        public void setMaxLimit(Long maxLimit) {
            this.maxLimit = maxLimit;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMapperPackage() {
        return mapperPackage;
    }

    public void setMapperPackage(String mapperPackage) {
        this.mapperPackage = mapperPackage;
    }

    public MyBatisPlusIdGeneratorProperties getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(MyBatisPlusIdGeneratorProperties idGenerator) {
        this.idGenerator = idGenerator;
    }

    public DataPermissionProperties getDataPermission() {
        return dataPermission;
    }

    public void setDataPermission(DataPermissionProperties dataPermission) {
        this.dataPermission = dataPermission;
    }

    public PaginationProperties getPagination() {
        return pagination;
    }

    public void setPagination(PaginationProperties pagination) {
        this.pagination = pagination;
    }

    public boolean isBlockAttackPluginEnabled() {
        return blockAttackPluginEnabled;
    }

    public void setBlockAttackPluginEnabled(boolean blockAttackPluginEnabled) {
        this.blockAttackPluginEnabled = blockAttackPluginEnabled;
    }
}
