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

package top.continew.starter.extension.crud.util;

import cn.hutool.core.lang.tree.TreeNodeConfig;
import top.continew.starter.core.util.validate.CheckUtils;
import top.continew.starter.extension.crud.annotation.TreeField;

/**
 * 树工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class TreeUtils {

    /**
     * 默认字段配置对象
     */
    public static final TreeNodeConfig DEFAULT_CONFIG = TreeNodeConfig.DEFAULT_CONFIG.setNameKey("title")
        .setIdKey("key")
        .setWeightKey("sort");

    private TreeUtils() {
    }

    /**
     * 根据 @TreeField 配置生成树结构配置
     *
     * @param treeField 树结构字段注解
     * @return 树结构配置
     */
    public static TreeNodeConfig genTreeNodeConfig(TreeField treeField) {
        CheckUtils.throwIfNull(treeField, "请添加并配置 @TreeField 树结构信息");
        return new TreeNodeConfig().setIdKey(treeField.value())
            .setParentIdKey(treeField.parentIdKey())
            .setNameKey(treeField.nameKey())
            .setWeightKey(treeField.weightKey())
            .setChildrenKey(treeField.childrenKey())
            .setDeep(treeField.deep() < 0 ? null : treeField.deep());
    }
}
