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

package top.continew.starter.idempotent.service;

/**
 * 服务接口
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 19:48
 * @Package top.continew.starter.idempotent.service.IdempotentService
 */
public interface IdempotentService {

    /**
     * 检验是否存在
     *
     * @param key     幂等key
     * @param timeout 超时时间
     * @return
     */
    boolean checkAndLock(String key, long timeout);

    /**
     * 释放对应key
     *
     * @param key 幂等key
     */
    void unlock(String key);
}