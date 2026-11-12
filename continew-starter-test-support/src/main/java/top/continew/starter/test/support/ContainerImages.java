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

package top.continew.starter.test.support;

/**
 * 集成测试容器镜像常量
 *
 * <p>镜像版本集中管理，升级只改本类一处，避免散落在各测试类中造成版本漂移。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public final class ContainerImages {

    /**
     * Redis 容器镜像（轻量 alpine 发行版）
     */
    public static final String REDIS_IMAGE = "redis:7.2-alpine";

    /**
     * MySQL 容器镜像
     */
    public static final String MYSQL_IMAGE = "mysql:8.0";

    /**
     * MinIO 容器镜像（S3 协议兼容对象存储）
     */
    public static final String MINIO_IMAGE = "minio/minio:RELEASE.2025-07-23T15-54-02Z";

    /**
     * HiveMQ 容器镜像（MQTT Broker，官方 Testcontainers 模块 CI 同版本）
     */
    public static final String HIVEMQ_IMAGE = "hivemq/hivemq4:4.7.4";

    /**
     * GreenMail standalone 容器镜像（内置 SMTP/SMTPS/POP3/IMAP 与管理 REST API）
     */
    public static final String GREENMAIL_IMAGE = "greenmail/standalone:2.1.3";

    private ContainerImages() {
    }
}
