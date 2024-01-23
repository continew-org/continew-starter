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

import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import top.charles7c.continew.starter.core.exception.BadRequestException;
import top.charles7c.continew.starter.core.util.ReflectUtils;
import top.charles7c.continew.starter.core.util.validate.ValidationUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 查询助手
 *
 * @author Charles7c
 * @author Jasmine
 * @author Zheng Jie（<a href="https://gitee.com/elunez/eladmin">ELADMIN</a>）
 * @since 1.0.0
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class QueryHelper {

    /**
     * 根据查询条件构建查询条件封装对象
     *
     * @param query 查询条件
     * @param <Q>   查询条件数据类型
     * @param <R>   查询数据类型
     * @return 查询条件封装对象
     */
    public static <Q, R> QueryWrapper<R> build(Q query) {
        QueryWrapper<R> queryWrapper = new QueryWrapper<>();
        // 没有查询条件，直接返回
        if (null == query) {
            return queryWrapper;
        }
        // 解析并拼接查询条件
        List<Field> fieldList = ReflectUtils.getNonStaticFields(query.getClass());
        fieldList.forEach(field -> buildWrapper(query, field, queryWrapper));
        return queryWrapper;
    }

    /**
     * 构建查询条件封装对象
     *
     * @param query        查询条件
     * @param field        字段
     * @param queryWrapper 查询条件封装对象
     * @param <Q>          查询条件数据类型
     * @param <R>          查询数据类型
     */
    public static <Q, R> void buildWrapper(Q query, Field field, QueryWrapper<R> queryWrapper) {
        boolean accessible = field.canAccess(query);
        try {
            field.setAccessible(true);
            // 如果字段值为空，直接返回
            Object fieldValue = field.get(query);
            if (ObjectUtil.isEmpty(fieldValue)) {
                return;
            }
            // 建议：数据库规范中列建议采用下划线连接法命名，程序规范中变量建议采用驼峰法命名
            String fieldName = field.getName();
            String columnName = StrUtil.toUnderlineCase(fieldName);
            // 没有 @Query 注解，默认等值查询
            Query queryAnnotation = field.getAnnotation(Query.class);
            if (null == queryAnnotation) {
                queryWrapper.eq(columnName, fieldValue);
                return;
            }
            // 解析单列查询
            String[] columns = queryAnnotation.columns();
            final int columnLength = ArrayUtil.length(columns);
            if (columnLength == 0 || columnLength == 1) {
                columnName = columnLength == 1 ? columns[0] : columnName;
                parse(queryAnnotation.type(), columnName, fieldValue, queryWrapper);
                return;
            }
            // 解析多列查询
            QueryType queryType = queryAnnotation.type();
            queryWrapper.nested(wrapper -> {
                for (String column : columns) {
                    switch (queryType) {
                        case EQ -> queryWrapper.or().eq(column, fieldValue);
                        case NE -> queryWrapper.or().ne(column, fieldValue);
                        case GT -> queryWrapper.or().gt(column, fieldValue);
                        case GE -> queryWrapper.or().ge(column, fieldValue);
                        case LT -> queryWrapper.or().lt(column, fieldValue);
                        case LE -> queryWrapper.or().le(column, fieldValue);
                        case BETWEEN -> {
                            List<Object> between = new ArrayList<>((List<Object>)fieldValue);
                            ValidationUtils.throwIf(between.size() != 2, "[{}] 必须是一个范围", fieldName);
                            queryWrapper.or().between(column, between.get(0), between.get(1));
                        }
                        case LIKE -> queryWrapper.or().like(column, fieldValue);
                        case LIKE_LEFT -> queryWrapper.or().likeLeft(column, fieldValue);
                        case LIKE_RIGHT -> queryWrapper.or().likeRight(column, fieldValue);
                        case IN -> {
                            ValidationUtils.throwIfEmpty(fieldValue, "[{}] 不能为空", fieldName);
                            queryWrapper.or().in(column, (List<Object>)fieldValue);
                        }
                        case NOT_IN -> {
                            ValidationUtils.throwIfEmpty(fieldValue, "[{}] 不能为空", fieldName);
                            queryWrapper.or().notIn(column, (List<Object>)fieldValue);
                        }
                        case IS_NULL -> queryWrapper.or().isNull(column);
                        case IS_NOT_NULL -> queryWrapper.or().isNotNull(column);
                        default -> throw new IllegalArgumentException(String.format("暂不支持 [%s] 查询类型", queryType));
                    }
                }
            });
        } catch (BadRequestException e) {
            log.error("Build query occurred an validation error: {}. Query: {}, Field: {}.", e
                .getMessage(), query, field, e);
            throw e;
        } catch (Exception e) {
            log.error("Build query occurred an error: {}. Query: {}, Field: {}.", e.getMessage(), query, field, e);
        } finally {
            field.setAccessible(accessible);
        }
    }

    /**
     * 解析查询条件
     *
     * @param queryType    查询类型
     * @param columnName   驼峰字段名
     * @param fieldValue   字段值
     * @param queryWrapper 查询条件封装对象
     * @param <R>          查询数据类型
     */
    private static <R> void parse(QueryType queryType,
                                  String columnName,
                                  Object fieldValue,
                                  QueryWrapper<R> queryWrapper) {
        switch (queryType) {
            case EQ -> queryWrapper.eq(columnName, fieldValue);
            case NE -> queryWrapper.ne(columnName, fieldValue);
            case GT -> queryWrapper.gt(columnName, fieldValue);
            case GE -> queryWrapper.ge(columnName, fieldValue);
            case LT -> queryWrapper.lt(columnName, fieldValue);
            case LE -> queryWrapper.le(columnName, fieldValue);
            case BETWEEN -> {
                List<Object> between = new ArrayList<>((List<Object>)fieldValue);
                ValidationUtils.throwIf(between.size() != 2, "[{}] 必须是一个范围", columnName);
                queryWrapper.between(columnName, between.get(0), between.get(1));
            }
            case LIKE -> queryWrapper.like(columnName, fieldValue);
            case LIKE_LEFT -> queryWrapper.likeLeft(columnName, fieldValue);
            case LIKE_RIGHT -> queryWrapper.likeRight(columnName, fieldValue);
            case IN -> {
                ValidationUtils.throwIfEmpty(fieldValue, "[{}] 不能为空", columnName);
                queryWrapper.in(columnName, (List<Object>)fieldValue);
            }
            case NOT_IN -> {
                ValidationUtils.throwIfEmpty(fieldValue, "[{}] 不能为空", columnName);
                queryWrapper.notIn(columnName, (List<Object>)fieldValue);
            }
            case IS_NULL -> queryWrapper.isNull(columnName);
            case IS_NOT_NULL -> queryWrapper.isNotNull(columnName);
            default -> throw new IllegalArgumentException(String.format("暂不支持 [%s] 查询类型", queryType));
        }
    }
}
