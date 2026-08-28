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

package top.continew.starter.core.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.lang.tree.parser.NodeParser;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ReflectUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 树工具类
 *
 * <p>
 * 扩展 Hutool TreeUtil 封装树构建
 * </p>
 *
 * @author Lion Li（<a href="https://gitee.com/dromara/RuoYi-Vue-Plus">RuoYi-Vue-Plus</a>）
 * @author lishuyan
 */
public class TreeUtils extends TreeUtil {

    private TreeUtils() {
    }

    /**
     * 构建树形结构
     *
     * @param <T>        输入节点的类型
     * @param <K>        节点ID的类型
     * @param list       节点列表，其中包含了要构建树形结构的所有节点
     * @param nodeParser 解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<Tree<K>> build(List<T> list, NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        K k = ReflectUtil.invoke(list.get(0), CharSequenceUtil.genGetter("parentId"));
        return TreeUtil.build(list, k, TreeNodeConfig.DEFAULT_CONFIG, nodeParser);
    }

    /**
     * 构建树形结构
     *
     * @param <T>        输入节点的类型
     * @param <K>        节点ID的类型
     * @param parentId   顶级节点
     * @param list       节点列表，其中包含了要构建树形结构的所有节点
     * @param nodeParser 解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<Tree<K>> build(List<T> list, K parentId,
        NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        return TreeUtil.build(list, parentId, TreeNodeConfig.DEFAULT_CONFIG, nodeParser);
    }

    /**
     * 构建树形结构
     *
     * @param <T>            输入节点的类型
     * @param <K>            节点ID的类型
     * @param list           节点列表，其中包含了要构建树形结构的所有节点
     * @param parentId       顶级节点
     * @param treeNodeConfig 树节点配置
     * @param nodeParser     解析器，用于将输入节点转换为树节点
     * @return 构建好的树形结构列表
     */
    public static <T, K> List<Tree<K>> build(List<T> list,
        K parentId,
        TreeNodeConfig treeNodeConfig,
        NodeParser<T, K> nodeParser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        return TreeUtil.build(list, parentId, treeNodeConfig, nodeParser);
    }

    /**
     * 构建多根节点的树结构（支持多个顶级节点）
     *
     * @param list        原始数据列表
     * @param getId       获取节点 ID 的方法引用，例如：node -> node.getId()
     * @param getParentId 获取节点父级 ID 的方法引用，例如：node -> node.getParentId()
     * @param parser      树节点属性映射器，用于将原始节点 T 转为 Tree 节点
     * @param <T>         原始数据类型（如实体类、DTO 等）
     * @param <K>         节点 ID 类型（如 Long、String）
     * @return 构建完成的树形结构（可能包含多个顶级根节点）
     */
    public static <T, K> List<Tree<K>> buildMultiRoot(List<T> list,
        Function<T, K> getId,
        Function<T, K> getParentId,
        NodeParser<T, K> parser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        Set<K> rootParentIds = CollUtils.mapToSet(list, getParentId);
        rootParentIds.removeAll(CollUtils.mapToSet(list, getId));
        // 构建每一个根 parentId 下的树，并合并成最终结果列表
        return rootParentIds.stream()
            .flatMap(rootParentId -> TreeUtil.build(list, rootParentId, parser).stream())
            .collect(Collectors.toList());
    }

    /**
     * 构建多根节点的树结构（支持多个顶级节点）
     *
     * @param <T>            原始数据类型（如实体类、DTO 等）
     * @param <K>            节点 ID 类型（如 Long、String）
     * @param list           原始数据列表
     * @param getId          获取节点 ID 的方法引用，例如：node -> node.getId()
     * @param getParentId    获取节点父级 ID 的方法引用，例如：node -> node.getParentId()
     * @param treeNodeConfig 树节点配置
     * @param parser         树节点属性映射器，用于将原始节点 T 转为 Tree 节点
     * @return 构建完成的树形结构（可能包含多个顶级根节点）
     */
    public static <T, K> List<Tree<K>> buildMultiRoot(List<T> list,
        Function<T, K> getId,
        Function<T, K> getParentId,
        TreeNodeConfig treeNodeConfig,
        NodeParser<T, K> parser) {
        if (CollUtil.isEmpty(list)) {
            return new ArrayList<>(0);
        }
        Set<K> rootParentIds = CollUtils.mapToSet(list, getParentId);
        rootParentIds.removeAll(CollUtils.mapToSet(list, getId));
        // 构建每一个根 parentId 下的树，并合并成最终结果列表
        return rootParentIds.stream()
            .flatMap(
                rootParentId -> TreeUtil.build(list, rootParentId, treeNodeConfig, parser).stream())
            .collect(Collectors.toList());
    }

    /**
     * 获取节点列表中所有节点的叶子节点
     *
     * @param <K>   节点ID的类型
     * @param nodes 节点列表
     * @return 包含所有叶子节点的列表
     */
    public static <K> List<Tree<K>> getLeafNodes(List<Tree<K>> nodes) {
        if (CollUtil.isEmpty(nodes)) {
            return new ArrayList<>(0);
        }
        return nodes.stream().flatMap(TreeUtils::extractLeafNodes).collect(Collectors.toList());
    }

    /**
     * 获取指定节点下的所有叶子节点
     *
     * @param <K>  节点ID的类型
     * @param node 要查找叶子节点的根节点
     * @return 包含所有叶子节点的列表
     */
    private static <K> Stream<Tree<K>> extractLeafNodes(Tree<K> node) {
        if (!node.hasChild()) {
            return Stream.of(node);
        } else {
            // 递归调用，获取所有子节点的叶子节点
            return node.getChildren().stream().flatMap(TreeUtils::extractLeafNodes);
        }
    }

}
