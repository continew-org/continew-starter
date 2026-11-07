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

package top.continew.starter.encrypt.field.interceptor;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import org.apache.ibatis.executor.resultset.ResultSetHandler;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.type.SimpleTypeRegistry;
import top.continew.starter.encrypt.field.annotation.FieldEncrypt;
import top.continew.starter.encrypt.field.util.EncryptHelper;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

/**
 * 字段解密拦截器
 *
 * @author Charles7c
 * @author lishuyan
 * @since 1.4.0
 */
@Intercepts({@Signature(type = ResultSetHandler.class, method = "handleResultSets",
    args = {Statement.class})})
public class MyBatisDecryptInterceptor extends AbstractMyBatisInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 获取执行结果
        Object obj = invocation.proceed();
        if (ObjectUtil.isNull(obj)) {
            return null;
        }
        // 确保目标是 ResultSetHandler
        if (!(invocation.getTarget() instanceof ResultSetHandler)) {
            return obj;
        }
        // 处理查询结果
        if (obj instanceof List<?> resultList) {
            // 处理列表结果
            this.decryptList(resultList);
        } else if (obj instanceof Map<?, ?> resultMap) {
            // 处理 map 结果（resultType=map 的单行结果）
            this.decryptMapRow(this.toMutableRow(resultMap));
        } else {
            // 处理单个对象结果
            this.decryptObject(obj);
        }
        return obj;
    }

    /**
     * 解密列表结果
     *
     * @param resultList 结果列表
     */
    private void decryptList(List<?> resultList) {
        if (CollUtil.isEmpty(resultList)) {
            return;
        }
        for (Object result : resultList) {
            decryptObject(result);
        }
    }

    /**
     * 解密单个对象结果
     *
     * @param result 结果对象
     */
    private void decryptObject(Object result) {
        if (ObjectUtil.isNull(result)) {
            return;
        }
        // String、Integer、Long 等简单类型对象无需处理
        if (SimpleTypeRegistry.isSimpleType(result.getClass())) {
            return;
        }
        // resultType=map 的结果行为 Map，按实体声明的加密列按键解密
        if (result instanceof Map<?, ?> resultMap) {
            this.decryptMapRow(this.toMutableRow(resultMap));
            return;
        }
        // 获取所有字符串类型、需要解密的、有值字段
        List<Field> fieldList = super.getEncryptFields(result);
        if (fieldList.isEmpty()) {
            return;
        }
        // 解密处理
        for (Field field : fieldList) {
            Object fieldValue = ReflectUtil.getFieldValue(result, field);
            if (fieldValue != null) {
                String strValue = String.valueOf(fieldValue);
                if (CharSequenceUtil.isNotBlank(strValue)) {
                    ReflectUtil.setFieldValue(result, field, EncryptHelper.decrypt(strValue, field
                        .getAnnotation(FieldEncrypt.class), field.getName()));
                }
            }
        }
    }

    /**
     * 按 MyBatis-Plus 已注册实体声明的加密列解密 map 结果行，列名命中加密列且值为字符串时
     * 解密替换，其余列保持原值
     *
     * @param row 结果行
     */
    @SuppressWarnings("unchecked")
    private void decryptMapRow(Map<Object, Object> row) {
        Map<String, FieldEncrypt> columnMap = super.getGlobalColumnEncryptAnnotations();
        if (columnMap.isEmpty()) {
            return;
        }
        for (Map.Entry<Object, Object> entry : row.entrySet()) {
            Object value = entry.getValue();
            if (!(value instanceof String strValue) || CharSequenceUtil.isBlank(strValue)) {
                continue;
            }
            String columnName = String.valueOf(entry.getKey()).toLowerCase();
            FieldEncrypt fieldEncrypt = columnMap.get(columnName);
            if (fieldEncrypt == null) {
                continue;
            }
            entry.setValue(EncryptHelper.decrypt(strValue, fieldEncrypt, columnName));
        }
    }

    /**
     * 转换为键值可替换的结果行视图
     *
     * @param resultMap 结果 Map
     * @return 可替换键值的 Map
     */
    @SuppressWarnings("unchecked")
    private Map<Object, Object> toMutableRow(Map<?, ?> resultMap) {
        return (Map<Object, Object>) resultMap;
    }
}
