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

package top.continew.starter.data.mf.autoconfigure;

import com.mybatisflex.core.dialect.DbType;
import com.mybatisflex.core.dialect.DialectFactory;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.mybatis.spring.annotation.MapperScan;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import top.continew.starter.core.constant.PropertiesConstants;
import top.continew.starter.core.util.GeneralPropertySourceFactory;
import top.continew.starter.data.mf.datapermission.DataPermissionDialect;
import top.continew.starter.data.mf.datapermission.DataPermissionFilter;

/**
 * MyBatis Flex 自动配置
 *
 * @author hellokaton
 * @since 2.0.2
 */
@AutoConfiguration
@MapperScan("${mybatis-flex.extension.mapper-package}")
@EnableTransactionManagement(proxyTargetClass = true)
@EnableConfigurationProperties(MyBatisFlexExtensionProperties.class)
@ConditionalOnProperty(prefix = "mybatis-flex.extension", name = PropertiesConstants.ENABLED, havingValue = "true")
@PropertySource(value = "classpath:default-data-mybatis-flex.yml", factory = GeneralPropertySourceFactory.class)
public class MybatisFlexAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(MybatisFlexAutoConfiguration.class);

    @Resource
    private DataPermissionFilter dataPermissionFilter;

    @PostConstruct
    public void postConstruct() {
        log.debug("[ContiNew Starter] - Auto Configuration 'MyBatis Flex' completed initialization.");
        DialectFactory.registerDialect(DbType.MYSQL, new DataPermissionDialect(dataPermissionFilter));
    }

}
