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

package top.continew.starter.data.mf.it.mapper;

import top.continew.starter.data.base.BaseMapper;
import top.continew.starter.data.mf.it.entity.ItUser;

/**
 * 测试用户 Mapper
 *
 * <p>继承模块扩展基类 {@code top.continew.starter.data.base.BaseMapper}，批量默认方法
 * 经 Flex 的 {@code Db} 工具按全局配置解析真实会话工厂。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
public interface ItUserMapper extends BaseMapper<ItUser> {
}
