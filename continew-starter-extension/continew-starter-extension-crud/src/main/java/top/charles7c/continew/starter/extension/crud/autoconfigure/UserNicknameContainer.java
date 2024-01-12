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

package top.charles7c.continew.starter.extension.crud.autoconfigure;

import cn.crane4j.core.container.Container;
import cn.hutool.core.collection.CollUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import top.charles7c.continew.starter.extension.crud.base.CommonUserService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * 用户昵称容器
 *
 * @author Charles7c
 * @since 1.2.0
 */
@Component
@RequiredArgsConstructor
public class UserNicknameContainer implements Container<Long> {

    private final CommonUserService userService;

    @Override
    public String getNamespace() {
        return "userNickname";
    }

    @Override
    public Map<Long, String> get(Collection<Long> ids) {
        Long id = CollUtil.getFirst(ids);
        String name = userService.getNicknameById(id);
        return Collections.singletonMap(id, name);
    }
}