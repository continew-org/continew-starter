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

package top.charles7c.continew.starter.core.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.stereotype.Component;

/**
 * 项目配置属性
 *
 * @author Charles7c
 * @since 1.0.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "project")
public class ProjectProperties {

    /**
     * 名称
     */
    private String name;

    /**
     * 应用名称
     */
    private String appName;

    /**
     * 版本
     */
    private String version;

    /**
     * 描述
     */
    private String description;

    /**
     * URL
     */
    private String url;

    /**
     * 基本包
     */
    private String basePackage;

    /**
     * 联系人
     */
    @NestedConfigurationProperty
    private Contact contact;

    /**
     * 许可协议
     */
    @NestedConfigurationProperty
    private License license;

    /**
     * 是否为生产环境
     */
    private boolean production = false;

    /**
     * 联系人配置属性
     */
    @Data
    public static class Contact {
        /**
         * 名称
         */
        private String name;

        /**
         * 邮箱
         */
        private String email;

        /**
         * URL
         */
        private String url;
    }

    /**
     * 许可协议配置属性
     */
    @Data
    public static class License {
        /**
         * 名称
         */
        private String name;

        /**
         * URL
         */
        private String url;
    }
}
