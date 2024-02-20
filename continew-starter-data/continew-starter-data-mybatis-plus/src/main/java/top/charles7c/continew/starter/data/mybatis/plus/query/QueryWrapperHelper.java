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

package top.charles7c.continew.starter.data.mybatis.plus.query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import top.charles7c.continew.starter.core.exception.BadRequestException;
import top.charles7c.continew.starter.core.util.ReflectUtils;
import top.charles7c.continew.starter.core.util.validate.ValidationUtils;
import top.charles7c.continew.starter.data.core.annotation.Query;
import top.charles7c.continew.starter.data.core.annotation.QueryIgnore;
import top.charles7c.continew.starter.data.core.enums.QueryType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * QueryWrapper 助手
 *
 * @author Charles7c
 * @author Jasmine
 * @since 1.0.0
 */
public class QueryWrapperHelper {

    private static final Logger log = LoggerFactory.getLogger(QueryWrapperHelper.class);

    private QueryWrapperHelper() {
    }

    /**
     * 构建 QueryWrapper
     *
     * @param query 查询条件
     * @param <Q>   查询条件数据类型
     * @param <R>   查询数据类型
     * @return QueryWrapper
     */
    public static <Q, R> QueryWrapper<R> build(Q query) {
        QueryWrapper<R> queryWrapper = new QueryWrapper<>();
        // 没有查询条件，直接返回
        if (null == query) {
            return queryWrapper;
        }
        // 获取查询条件中所有的字段
        List<Field> fieldList = ReflectUtils.getNonStaticFields(query.getClass());
        return build(query, fieldList, queryWrapper);
    }

    /**
     * 构建 QueryWrapper
     *
     * @param query        查询条件
     * @param fields       查询条件字段列表
     * @param queryWrapper QueryWrapper
     * @param <Q>          查询条件数据类型
     * @param <R>          查询数据类型
     * @return QueryWrapper
     */
    public static <Q, R> QueryWrapper<R> build(Q query, List<Field> fields, QueryWrapper<R> queryWrapper) {
        // 没有查询条件，直接返回
        if (null == query) {
            return queryWrapper;
        }
        // 解析并拼接查询条件
        for (Field field : fields) {
            List<Consumer<QueryWrapper<R>>> consumers = buildWrapperConsumer(query, field);
            queryWrapper.and(CollUtil.isNotEmpty(consumers), q -> consumers.forEach(q::or));
        }
        return queryWrapper;
    }

    /**
     * 构建 QueryWrapper Consumer
     *
     * @param query 查询条件
     * @param field 查询条件字段
     * @param <Q>   查询条件数据类型
     * @param <R>   查询数据类型
     * @return QueryWrapper Consumer
     */
    private static <Q, R> List<Consumer<QueryWrapper<R>>> buildWrapperConsumer(Q query, Field field) {
        boolean accessible = field.canAccess(query);
        try {
            field.setAccessible(true);
            // 如果字段值为空，直接返回
            Object fieldValue = field.get(query);
            if (ObjectUtil.isEmpty(fieldValue)) {
                return Collections.emptyList();
            }
            // 设置了 @QueryIgnore 注解，直接忽略
            QueryIgnore queryIgnoreAnnotation = field.getAnnotation(QueryIgnore.class);
            if (null != queryIgnoreAnnotation && queryIgnoreAnnotation.value()) {
                return Collections.emptyList();
            }
            // 建议：数据库表列建议采用下划线连接法命名，程序变量建议采用驼峰法命名
            String fieldName = field.getName();
            // 没有 @Query 注解，默认等值查询
            Query queryAnnotation = field.getAnnotation(Query.class);
            if (null == queryAnnotation) {
                return Collections.singletonList(q -> q.eq(CharSequenceUtil.toUnderlineCase(fieldName), fieldValue));
            }
            // 解析单列查询
            QueryType queryType = queryAnnotation.type();
            String[] columns = queryAnnotation.columns();
            final int columnLength = ArrayUtil.length(columns);
            List<Consumer<QueryWrapper<R>>> consumers = new ArrayList<>(columnLength);
            if (columnLength <= 1) {
                String columnName = columnLength == 1 ? columns[0] : CharSequenceUtil.toUnderlineCase(fieldName);
                parse(queryType, columnName, fieldValue, consumers);
                return consumers;
            }
            // 解析多列查询
            for (String column : columns) {
                parse(queryType, column, fieldValue, consumers);
            }
            return consumers;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Build query wrapper occurred an error: {}. Query: {}, Field: {}.", e
                .getMessage(), query, field, e);
        } finally {
            field.setAccessible(accessible);
        }
        return Collections.emptyList();
    }

    /**
     * 解析查询条件
     *
     * @param queryType  查询类型
     * @param columnName 列名
     * @param fieldValue 字段值
     * @param <R>        查询数据类型
     */
    private static <R> void parse(QueryType queryType,
                                  String columnName,
                                  Object fieldValue,
                                  List<Consumer<QueryWrapper<R>>> consumers) {
        switch (queryType) {
            case EQ -> consumers.add(q -> q.eq(columnName, fieldValue));
            case NE -> consumers.add(q -> q.ne(columnName, fieldValue));
            case GT -> consumers.add(q -> q.gt(columnName, fieldValue));
            case GE -> consumers.add(q -> q.ge(columnName, fieldValue));
            case LT -> consumers.add(q -> q.lt(columnName, fieldValue));
            case LE -> consumers.add(q -> q.le(columnName, fieldValue));
            case BETWEEN -> {
                List<Object> between = new ArrayList<>((List<Object>)fieldValue);
                ValidationUtils.throwIf(between.size() != 2, "[{}] 必须是一个范围", columnName);
                consumers.add(q -> q.between(columnName, between.get(0), between.get(1)));
            }
            case LIKE -> consumers.add(q -> q.like(columnName, fieldValue));
            case LIKE_LEFT -> consumers.add(q -> q.likeLeft(columnName, fieldValue));
            case LIKE_RIGHT -> consumers.add(q -> q.likeRight(columnName, fieldValue));
            case IN -> {
                ValidationUtils.throwIfEmpty(fieldValue, "[{}] 不能为空", columnName);
                consumers.add(q -> q.in(columnName, (List<Object>)fieldValue));
            }
            case NOT_IN -> {
                ValidationUtils.throwIfEmpty(fieldValue, "[{}] 不能为空", columnName);
                consumers.add(q -> q.notIn(columnName, (List<Object>)fieldValue));
            }
            case IS_NULL -> consumers.add(q -> q.isNull(columnName));
            case IS_NOT_NULL -> consumers.add(q -> q.isNotNull(columnName));
            default -> throw new IllegalArgumentException("暂不支持 [%s] 查询类型".formatted(queryType));
        }
    }
}
