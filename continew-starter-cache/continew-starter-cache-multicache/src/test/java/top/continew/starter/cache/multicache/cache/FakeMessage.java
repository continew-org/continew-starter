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

package top.continew.starter.cache.multicache.cache;

import org.springframework.data.redis.connection.Message;

import java.nio.charset.StandardCharsets;

/**
 * 模拟 Redis Message（广播失效消息），供多级缓存相关测试共用
 *
 * @author KAI
 */
class FakeMessage implements Message {

    private final byte[] body;

    FakeMessage(String body) {
        this.body = body.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public byte[] getChannel() {
        return "test-topic".getBytes(StandardCharsets.UTF_8);
    }
}
