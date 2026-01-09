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

package top.continew.starter.apidoc.processor;

import cn.hutool.core.util.ClassUtil;
import org.springframework.stereotype.Component;
import top.continew.starter.core.enums.BaseEnum;
import top.nextdoc4j.enums.resolver.EnumMetadataResolver;

/**
 * BaseEnum 枚举处理器 - NexDoc4j 枚举插件展示枚举值
 *
 * @author echo
 * @since 2.15.0
 */
@Component
public class BaseEnumProcessor implements EnumMetadataResolver {

    @Override
    public boolean supports(Class<?> aClass) {
        return aClass != null && aClass.isEnum() && ClassUtil.isAssignable(BaseEnum.class, aClass);
    }

    @Override
    public Class<?> getEnumInterfaceType() {
        return BaseEnum.class;
    }

}
