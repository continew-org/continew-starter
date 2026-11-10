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

import static org.assertj.core.api.Assertions.assertThat;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNodeConfig;
import cn.hutool.core.lang.tree.parser.NodeParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 树工具类测试
 *
 * <p>
 * 锁定树构建的根节点推导（默认取首个元素的 parentId）、多根拆分与叶子收集契约。
 * </p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class TreeUtilsTest {

    /**
     * 节点 ID 到树节点的映射器：仅搬运 id / parentId / name 三个字段
     */
    private static final NodeParser<TestNode, Long> PARSER = (node, tree) -> {
        tree.setId(node.getId());
        tree.setParentId(node.getParentId());
        tree.setName(node.getName());
    };

    /**
     * 构造两级树样例：根 0 下有节点 1、4，节点 1 下有叶子 2、3
     *
     * @return 样例节点列表
     */
    private static List<TestNode> buildSampleNodes() {
        TestNode first = new TestNode(1L, 0L, "节点1");
        TestNode second = new TestNode(2L, 1L, "节点2");
        TestNode third = new TestNode(3L, 1L, "节点3");
        TestNode fourth = new TestNode(4L, 0L, "节点4");
        return Arrays.asList(first, second, third, fourth);
    }

    @Test
    @DisplayName("build 对空列表与 null 列表返回空列表")
    void shouldReturnEmptyListForEmptyInput() {
        assertThat(TreeUtils.build(new ArrayList<TestNode>(), PARSER)).isEmpty();
        assertThat(TreeUtils.build((List<TestNode>) null, PARSER)).isEmpty();
    }

    @Test
    @DisplayName("build 不传根 ID 时以首个元素的 parentId 推导根层")
    void shouldDeriveRootFromFirstNodeParentId() {
        List<Tree<Long>> trees = TreeUtils.build(buildSampleNodes(), PARSER);
        assertThat(trees).extracting(Tree::getId).containsExactly(1L, 4L);
        assertThat(trees.get(0).getChildren()).extracting(Tree::getId)
            .containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("build 按指定根 ID 过滤层级")
    void shouldBuildBySpecifiedRootId() {
        List<Tree<Long>> trees = TreeUtils.build(buildSampleNodes(), 1L, PARSER);
        assertThat(trees).extracting(Tree::getId).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("build 指定不存在的根 ID 时返回 null（锁定当前行为：Hutool 无匹配根时不产空列表）")
    void shouldReturnNullForAbsentRootId() {
        assertThat(TreeUtils.build(buildSampleNodes(), 99L, PARSER)).isNull();
    }

    @Test
    @DisplayName("build 支持自定义 TreeNodeConfig")
    void shouldBuildWithCustomConfig() {
        List<Tree<Long>> trees = TreeUtils
            .build(buildSampleNodes(), 0L, new TreeNodeConfig(), PARSER);
        assertThat(trees).extracting(Tree::getId).containsExactly(1L, 4L);
    }

    @Test
    @DisplayName("buildMultiRoot 支持多个互不相邻的顶级节点")
    void shouldBuildMultiRootTrees() {
        List<TestNode> nodes = Arrays
            .asList(new TestNode(1L, 0L, "根A"), new TestNode(2L, 10L, "根B"),
                new TestNode(3L, 2L, "子节点"));
        List<Tree<Long>> trees =
            TreeUtils.buildMultiRoot(nodes, TestNode::getId, TestNode::getParentId, PARSER);
        assertThat(trees).extracting(Tree::getId).containsExactlyInAnyOrder(1L, 2L);
    }

    @Test
    @DisplayName("buildMultiRoot 对空列表返回空列表")
    void shouldReturnEmptyForMultiRootEmptyInput() {
        assertThat(TreeUtils
            .buildMultiRoot(new ArrayList<TestNode>(), TestNode::getId, TestNode::getParentId,
                PARSER))
            .isEmpty();
    }

    @Test
    @DisplayName("getLeafNodes 递归收集全部叶子")
    void shouldCollectAllLeafNodes() {
        List<Tree<Long>> trees = TreeUtils.build(buildSampleNodes(), PARSER);
        assertThat(TreeUtils.getLeafNodes(trees)).extracting(Tree::getId)
            .containsExactly(2L, 3L, 4L);
    }

    @Test
    @DisplayName("getLeafNodes 对空列表与 null 返回空列表")
    void shouldReturnEmptyForLeafNodesEmptyInput() {
        assertThat(TreeUtils.getLeafNodes(new ArrayList<Tree<Long>>())).isEmpty();
        assertThat(TreeUtils.getLeafNodes(null)).isEmpty();
    }

    /**
     * 树构建样例节点
     */
    private static class TestNode {

        private final Long id;

        private final Long parentId;

        private final String name;

        /**
         * 构造样例节点
         *
         * @param id       节点 ID
         * @param parentId 父节点 ID
         * @param name     节点名称
         */
        TestNode(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }

        public Long getId() {
            return id;
        }

        public Long getParentId() {
            return parentId;
        }

        public String getName() {
            return name;
        }
    }
}
