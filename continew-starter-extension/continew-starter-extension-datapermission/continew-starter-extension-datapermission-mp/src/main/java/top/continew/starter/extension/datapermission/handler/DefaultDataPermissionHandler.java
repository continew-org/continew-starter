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

package top.continew.starter.extension.datapermission.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.data.enums.DatabaseType;
import top.continew.starter.data.util.MetaUtils;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import top.continew.starter.extension.datapermission.constant.DataPermissionConstants;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.exception.DataPermissionException;
import top.continew.starter.extension.datapermission.model.RoleData;
import top.continew.starter.extension.datapermission.model.UserData;
import top.continew.starter.extension.datapermission.provider.DataPermissionUserDataProvider;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认数据权限处理器
 *
 * @author <a href="https://gitee.com/baomidou/mybatis-plus/issues/I37I90">DataPermissionInterceptor 如何使用？</a>
 * @author Charles7c
 * @since 1.1.0
 */
public class DefaultDataPermissionHandler implements DataPermissionHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataPermissionHandler.class);
    private final DataPermissionUserDataProvider dataPermissionUserDataProvider;
    /**
     * Mapper类中所有方法数据权限注解缓存
     */
    private final Map<String, Map<String, DataPermission>> annotationCache =
        new ConcurrentHashMap<>();

    public DefaultDataPermissionHandler(
        DataPermissionUserDataProvider dataPermissionUserDataProvider) {
        this.dataPermissionUserDataProvider = dataPermissionUserDataProvider;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        try {
            DataPermission dataPermission = findDataPermissionAnnotation(mappedStatementId);
            if (dataPermission != null && dataPermissionUserDataProvider.isFilter()) {
                return buildDataScopeFilter(dataPermission, where);
            }
        } catch (Exception e) {
            log.error("Data permission handler build data scope filter occurred an error: {}.",
                e.getMessage(), e);
        }
        return where;
    }

    /**
     * 查找数据权限注解
     *
     * @param mappedStatementId Mapper 方法 ID
     * @return 数据权限注解
     */
    private DataPermission findDataPermissionAnnotation(String mappedStatementId) {
        try {
            int lastDotIndex = mappedStatementId.lastIndexOf(StringConstants.DOT);
            if (lastDotIndex == -1) {
                return null;
            }

            String className = mappedStatementId.substring(0, lastDotIndex);
            String methodName = mappedStatementId.substring(lastDotIndex + 1);

            // 先根据类名从缓存获取，如果methodAnnotations不为空，则说明该类中的所有方法都已缓存， 只是值为null。
            Map<String, DataPermission> methodAnnotations = annotationCache.get(className);
            if (methodAnnotations != null) {
                // methodName 可能是 ** 或者 **_COUNT
                return methodAnnotations.getOrDefault(methodName, methodAnnotations
                    .get(methodName + DataPermissionConstants.COUNT_METHOD_SUFFIX));
            }

            // 缓存未命中，执行反射操作
            Class<?> clazz = Class.forName(className);
            Method[] methods = clazz.getMethods();

            // 创建新的缓存映射
            Map<String, DataPermission> newMethodAnnotations = new ConcurrentHashMap<>();

            // 缓存所有带@DataPermission注解的方法
            for (Method method : methods) {
                String name = method.getName();
                DataPermission annotation = method.getAnnotation(DataPermission.class);
                if (annotation != null) {
                    newMethodAnnotations.put(name, annotation);
                }
            }
            // 存入缓存
            annotationCache.put(className, newMethodAnnotations);

            return newMethodAnnotations.get(methodName);
        } catch (ClassNotFoundException e) {
            throw DataPermissionException.methodNotFound(mappedStatementId);
        }
    }

    /**
     * 构建数据范围过滤条件
     *
     * @param dataPermission 数据权限
     * @param where          当前查询条件
     * @return 构建后查询条件
     */
    private Expression buildDataScopeFilter(DataPermission dataPermission, Expression where) {
        UserData userData = dataPermissionUserDataProvider.getUserData();
        if (userData == null || !userData.isValid()) {
            throw DataPermissionException.invalidUserData("User data is null or invalid");
        }

        Expression expression = null;
        Set<RoleData> roles = userData.getRoles();

        for (RoleData roleData : roles) {
            DataScope dataScope = roleData.getDataScope();
            if (DataScope.ALL.equals(dataScope)) {
                return where;
            }

            expression = switch (dataScope) {
                case DEPT_AND_CHILD ->
                    buildDeptAndChildExpression(dataPermission, userData, expression);
                case DEPT -> buildDeptExpression(dataPermission, userData, expression);
                case SELF -> buildSelfExpression(dataPermission, userData, expression);
                case CUSTOM -> buildCustomExpression(dataPermission, roleData, expression);
                default -> throw DataPermissionException.unsupportedDataScope(dataScope.toString());
            };
        }

        return where != null ? new AndExpression(where, new ParenthesedExpressionList<>(expression))
            : expression;
    }

    /**
     * 构建本部门及以下数据权限表达式
     *
     * <p>
     * 处理完后的 SQL 示例：<br /> select t1.* from table as t1 where t1.dept_id in (select id from sys_dept where id = xxx or
     * find_in_set(xxx, ancestors));
     * </p>
     *
     * @param dataPermission 数据权限
     * @param userData       用户数据
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildDeptAndChildExpression(DataPermission dataPermission,
        UserData userData,
        Expression expression) {
        ParenthesedSelect subSelect = new ParenthesedSelect();
        PlainSelect select = new PlainSelect();
        select.setSelectItems(
            Collections.singletonList(new SelectItem<>(new Column(dataPermission.id()))));
        select.setFromItem(new Table(dataPermission.deptTableAlias()));

        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(dataPermission.id()));
        equalsTo.setRightExpression(new LongValue(userData.getDeptId()));

        DatabaseType databaseType = MetaUtils.getDatabaseType(SpringUtil.getBean(DataSource.class));
        Expression inSetExpression;
        if (DatabaseType.MYSQL.getDatabase().equalsIgnoreCase(databaseType.getDatabase())) {
            Function findInSetFunction = new Function();
            findInSetFunction.setName("find_in_set");
            findInSetFunction.setParameters(new ExpressionList(new LongValue(userData
                .getDeptId()), new Column(DataPermissionConstants.ANCESTORS_COLUMN)));
            inSetExpression = findInSetFunction;
        } else if (DatabaseType.POSTGRE_SQL.getDatabase()
            .equalsIgnoreCase(databaseType.getDatabase())) {
            // 构建 concat 函数
            Function concatFunction = new Function("concat");
            concatFunction
                .setParameters(
                    new ExpressionList<>(new Column(DataPermissionConstants.ANCESTORS_COLUMN),
                        new StringValue(",")));

            // 创建 LIKE 函数
            LikeExpression likeExpression = new LikeExpression();
            likeExpression.setLeftExpression(concatFunction);
            likeExpression.setRightExpression(new StringValue("%," + userData.getDeptId() + ",%"));
            inSetExpression = likeExpression;
        } else {
            throw DataPermissionException.unsupportedDatabase(databaseType.getDatabase());
        }

        select.setWhere(new OrExpression(equalsTo, inSetExpression));
        subSelect.setSelect(select);
        // 构建父查询
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(
            this.buildColumn(dataPermission.tableAlias(), dataPermission.deptId()));
        inExpression.setRightExpression(subSelect);
        return expression != null ? new OrExpression(expression, inExpression) : inExpression;
    }

    /**
     * 构建本部门数据权限表达式
     *
     * <p>
     * 处理完后的 SQL 示例：<br /> select t1.* from table as t1 where t1.dept_id = xxx;
     * </p>
     *
     * @param dataPermission 数据权限
     * @param userData       用户数据
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildDeptExpression(DataPermission dataPermission, UserData userData,
        Expression expression) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(
            this.buildColumn(dataPermission.tableAlias(), dataPermission.deptId()));
        equalsTo.setRightExpression(new LongValue(userData.getDeptId()));
        return expression != null ? new OrExpression(expression, equalsTo) : equalsTo;
    }

    /**
     * 构建仅本人数据权限表达式
     *
     * <p>
     * 处理完后的 SQL 示例：<br /> select t1.* from table as t1 where t1.create_user = xxx;
     * </p>
     *
     * @param dataPermission 数据权限
     * @param userData       用户数据
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildSelfExpression(DataPermission dataPermission, UserData userData,
        Expression expression) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(
            this.buildColumn(dataPermission.tableAlias(), dataPermission.userId()));
        equalsTo.setRightExpression(new LongValue(userData.getUserId()));
        return expression != null ? new OrExpression(expression, equalsTo) : equalsTo;
    }

    /**
     * 构建自定义数据权限表达式
     *
     * <p>
     * 处理完后的 SQL 示例：<br /> select t1.* from table as t1 where t1.dept_id in (select dept_id from sys_role_dept where
     * role_id = xxx);
     * </p>
     *
     * @param dataPermission 数据权限
     * @param roleData       角色上下文
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildCustomExpression(DataPermission dataPermission, RoleData roleData,
        Expression expression) {
        ParenthesedSelect subSelect = new ParenthesedSelect();
        PlainSelect select = new PlainSelect();
        select.setSelectItems(
            Collections.singletonList(new SelectItem<>(new Column(dataPermission.deptId()))));
        select.setFromItem(new Table(dataPermission.roleDeptTableAlias()));
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(dataPermission.roleId()));
        equalsTo.setRightExpression(new LongValue(roleData.getRoleId()));
        select.setWhere(equalsTo);
        subSelect.setSelect(select);
        // 构建父查询
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(
            this.buildColumn(dataPermission.tableAlias(), dataPermission.deptId()));
        inExpression.setRightExpression(subSelect);
        return expression != null ? new OrExpression(expression, inExpression) : inExpression;
    }

    /**
     * 构建 Column
     *
     * @param tableAlias 表别名
     * @param columnName 字段名称
     * @return 带表别名字段
     */
    private Column buildColumn(String tableAlias, String columnName) {
        if (StringUtils.isNotEmpty(tableAlias)) {
            return new Column("%s.%s".formatted(tableAlias, columnName));
        }
        return new Column(columnName);
    }
}
