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

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.plugins.handler.DataPermissionHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.continew.starter.core.constant.StringConstants;
import top.continew.starter.extension.datapermission.annotation.DataPermission;
import top.continew.starter.extension.datapermission.enums.DataScope;
import top.continew.starter.extension.datapermission.filter.DataPermissionUserContextProvider;
import top.continew.starter.extension.datapermission.model.RoleContext;
import top.continew.starter.extension.datapermission.model.UserContext;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

/**
 * 默认数据权限处理器
 *
 * @author <a href="https://gitee.com/baomidou/mybatis-plus/issues/I37I90">DataPermissionInterceptor 如何使用？</a>
 * @author Charles7c
 * @since 1.1.0
 */
public class DefaultDataPermissionHandler implements DataPermissionHandler {

    private static final Logger log = LoggerFactory.getLogger(DefaultDataPermissionHandler.class);
    private final DataPermissionUserContextProvider dataPermissionUserContextProvider;

    public DefaultDataPermissionHandler(DataPermissionUserContextProvider dataPermissionUserContextProvider) {
        this.dataPermissionUserContextProvider = dataPermissionUserContextProvider;
    }

    @Override
    public Expression getSqlSegment(Expression where, String mappedStatementId) {
        try {
            Class<?> clazz = Class.forName(mappedStatementId.substring(0, mappedStatementId
                .lastIndexOf(StringConstants.DOT)));
            String methodName = mappedStatementId.substring(mappedStatementId.lastIndexOf(StringConstants.DOT) + 1);
            Method[] methodArr = clazz.getMethods();
            for (Method method : methodArr) {
                DataPermission dataPermission = method.getAnnotation(DataPermission.class);
                String name = method.getName();
                if (null == dataPermission || !CharSequenceUtil.equalsAny(methodName, name, name + "_COUNT")) {
                    continue;
                }
                if (dataPermissionUserContextProvider.isFilter()) {
                    return buildDataScopeFilter(dataPermission, where);
                }
            }
        } catch (ClassNotFoundException e) {
            log.error("Data permission handler build data scope filter occurred an error: {}.", e.getMessage(), e);
        }
        return where;
    }

    /**
     * 构建数据范围过滤条件
     *
     * @param dataPermission 数据权限
     * @param where          当前查询条件
     * @return 构建后查询条件
     */
    private Expression buildDataScopeFilter(DataPermission dataPermission, Expression where) {
        Expression expression = null;
        UserContext userContext = dataPermissionUserContextProvider.getUserContext();
        Set<RoleContext> roles = userContext.getRoles();
        for (RoleContext roleContext : roles) {
            DataScope dataScope = roleContext.getDataScope();
            if (DataScope.ALL.equals(dataScope)) {
                return where;
            }
            switch (dataScope) {
                case DEPT_AND_CHILD -> expression = this
                    .buildDeptAndChildExpression(dataPermission, userContext, expression);
                case DEPT -> expression = this.buildDeptExpression(dataPermission, userContext, expression);
                case SELF -> expression = this.buildSelfExpression(dataPermission, userContext, expression);
                case CUSTOM -> expression = this.buildCustomExpression(dataPermission, roleContext, expression);
                default -> throw new IllegalArgumentException("暂不支持 [%s] 数据权限".formatted(dataScope));
            }
        }
        return null != where ? new AndExpression(where, new ParenthesedExpressionList<>(expression)) : expression;
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
     * @param userContext    用户上下文
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildDeptAndChildExpression(DataPermission dataPermission,
                                                   UserContext userContext,
                                                   Expression expression) {
        ParenthesedSelect subSelect = new ParenthesedSelect();
        PlainSelect select = new PlainSelect();
        select.setSelectItems(Collections.singletonList(new SelectItem<>(new Column(dataPermission.id()))));
        select.setFromItem(new Table(dataPermission.deptTableAlias()));
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(dataPermission.id()));
        equalsTo.setRightExpression(new LongValue(userContext.getDeptId()));
        Function function = new Function();
        function.setName("find_in_set");
        function.setParameters(new ExpressionList<>(new LongValue(userContext.getDeptId()), new Column("ancestors")));
        select.setWhere(new OrExpression(equalsTo, function));
        subSelect.setSelect(select);
        // 构建父查询
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(this.buildColumn(dataPermission.tableAlias(), dataPermission.deptId()));
        inExpression.setRightExpression(subSelect);
        return null != expression ? new OrExpression(expression, inExpression) : inExpression;
    }

    /**
     * 构建本部门数据权限表达式
     *
     * <p>
     * 处理完后的 SQL 示例：<br /> select t1.* from table as t1 where t1.dept_id = xxx;
     * </p>
     *
     * @param dataPermission 数据权限
     * @param userContext    用户上下文
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildDeptExpression(DataPermission dataPermission,
                                           UserContext userContext,
                                           Expression expression) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(this.buildColumn(dataPermission.tableAlias(), dataPermission.deptId()));
        equalsTo.setRightExpression(new LongValue(userContext.getDeptId()));
        return null != expression ? new OrExpression(expression, equalsTo) : equalsTo;
    }

    /**
     * 构建仅本人数据权限表达式
     *
     * <p>
     * 处理完后的 SQL 示例：<br /> select t1.* from table as t1 where t1.create_user = xxx;
     * </p>
     *
     * @param dataPermission 数据权限
     * @param userContext    用户上下文
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildSelfExpression(DataPermission dataPermission,
                                           UserContext userContext,
                                           Expression expression) {
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(this.buildColumn(dataPermission.tableAlias(), dataPermission.userId()));
        equalsTo.setRightExpression(new LongValue(userContext.getUserId()));
        return null != expression ? new OrExpression(expression, equalsTo) : equalsTo;
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
     * @param roleContext    角色上下文
     * @param expression     处理前的表达式
     * @return 处理完后的表达式
     */
    private Expression buildCustomExpression(DataPermission dataPermission,
                                             RoleContext roleContext,
                                             Expression expression) {
        ParenthesedSelect subSelect = new ParenthesedSelect();
        PlainSelect select = new PlainSelect();
        select.setSelectItems(Collections.singletonList(new SelectItem<>(new Column(dataPermission.deptId()))));
        select.setFromItem(new Table(dataPermission.roleDeptTableAlias()));
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(dataPermission.roleId()));
        equalsTo.setRightExpression(new LongValue(roleContext.getRoleId()));
        select.setWhere(equalsTo);
        subSelect.setSelect(select);
        // 构建父查询
        InExpression inExpression = new InExpression();
        inExpression.setLeftExpression(this.buildColumn(dataPermission.tableAlias(), dataPermission.deptId()));
        inExpression.setRightExpression(subSelect);
        return null != expression ? new OrExpression(expression, inExpression) : inExpression;
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
