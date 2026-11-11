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

package top.continew.starter.data.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import top.continew.starter.data.enums.LogicalRelation;
import top.continew.starter.data.enums.QueryType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.List;

/**
 * 查询注解契约测试
 *
 * <p>锁定 {@link Query} 的注解默认值（列名回落、等值查询、或关系多列），以及
 * {@link QueryType} 与 {@link LogicalRelation} 的枚举成员集合——下游 ORM 实现按
 * 这些值分派查询条件构造，枚举扩缩容属于破坏性变更，需显式更新此契约。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class QueryAnnotationContractTest {

    /**
     * 注解未显式配置时应保持默认：列名回落到字段名、等值查询、多列或关系。
     */
    @Test
    void shouldKeepQueryDefaults() throws Exception {
        Query query = this.annotationOf("defaultUsage");
        assertThat(query.columns()).isEmpty();
        assertThat(query.type()).isEqualTo(QueryType.EQ);
        assertThat(query.logicalRelation()).isEqualTo(LogicalRelation.OR);
    }

    /**
     * 显式配置的属性应原样暴露。
     */
    @Test
    void shouldExposeExplicitConfiguration() throws Exception {
        Query query = this.annotationOf("explicitUsage");
        assertThat(query.columns()).containsExactly("dept_id", "dept_name");
        assertThat(query.type()).isEqualTo(QueryType.LIKE);
        assertThat(query.logicalRelation()).isEqualTo(LogicalRelation.AND);
    }

    /**
     * 注解应只标在字段上且运行期可见，否则 ORM 反射扫描拿不到元数据。
     */
    @Test
    void shouldTargetFieldAndRetainRuntime() {
        assertThat(Query.class.isAnnotation()).isTrue();
        Target targetMethod = Query.class.getAnnotation(Target.class);
        assertThat(List.of(targetMethod.value())).containsExactly(ElementType.FIELD);
        Retention retention = Query.class.getAnnotation(Retention.class);
        assertThat(retention.value()).isEqualTo(RetentionPolicy.RUNTIME);
    }

    /**
     * 查询类型枚举成员应保持稳定。
     */
    @Test
    void shouldKeepQueryTypeMembers() {
        assertThat(QueryType.values()).extracting(Enum::name)
            .containsExactly("EQ", "NE", "GT", "GE", "LT", "LE", "BETWEEN", "LIKE", "LIKE_LEFT",
                "LIKE_RIGHT", "IN", "NOT_IN", "IS_NULL", "IS_NOT_NULL");
    }

    /**
     * 逻辑关系枚举成员应保持稳定。
     */
    @Test
    void shouldKeepLogicalRelationMembers() {
        assertThat(LogicalRelation.values()).extracting(Enum::name).containsExactly("AND", "OR");
    }

    /**
     * 读取指定字段上的 Query 注解。
     *
     * @param name 字段名
     * @return Query 注解实例
     */
    private Query annotationOf(String name) throws Exception {
        Field field = QueryAnnotationContractTest.class.getDeclaredField(name);
        return field.getAnnotation(Query.class);
    }

    @Query
    private String defaultUsage;

    @Query(columns = {"dept_id", "dept_name"}, type = QueryType.LIKE,
        logicalRelation = LogicalRelation.AND)
    private String explicitUsage;
}
