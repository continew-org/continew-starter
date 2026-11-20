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

package top.continew.starter.openapi.spi;

import java.time.Duration;

/**
 * 重放防护存储 SPI
 *
 * <p>以「原子占位 + 有效期」语义实现 nonce 一次性判定。占位成功后若验签后续步骤失败，
 * 过滤器会调用 {@link #release(String)} 释放占位，保证接入方可重试。
 * 使用方定义同名 Bean 覆盖默认实现（多实例部署须覆盖为集中式存储版本）。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public interface ReplayGuard {

    /**
     * 原子占位：键不存在或已过期时写入并在有效期内拒绝相同键
     *
     * @param nonceKey nonce 去重键
     * @param ttl      占位有效期
     * @return 占位成功（首次出现）返回 true，键在有效期内已存在返回 false
     */
    boolean tryAcquire(String nonceKey, Duration ttl);

    /**
     * 释放占位，删除去重键
     *
     * @param nonceKey nonce 去重键
     */
    void release(String nonceKey);
}
