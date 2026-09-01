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

package top.continew.starter.excel.model;

import java.util.Map;

/**
 * Excel 字段信息
 *
 * @author jiang4yu
 * @since 2.13.0
 */
public class ExcelClassField {

    /**
     * 字段名称
     */
    private String fieldName;

    /**
     * 表头名称
     */
    private String name;

    /**
     * 映射关系
     */
    private Map<String, String> kvMap;

    /**
     * 示例值
     */
    private Object example;

    /**
     * 排序
     */
    private int sort;

    /**
     * 是否为注解字段：0-否，1-是
     */
    private int hasAnnotation;

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getKvMap() {
        return kvMap;
    }

    public void setKvMap(Map<String, String> kvMap) {
        this.kvMap = kvMap;
    }

    public Object getExample() {
        return example;
    }

    public void setExample(Object example) {
        this.example = example;
    }

    public int getSort() {
        return sort;
    }

    public void setSort(int sort) {
        this.sort = sort;
    }

    public int getHasAnnotation() {
        return hasAnnotation;
    }

    public void setHasAnnotation(int hasAnnotation) {
        this.hasAnnotation = hasAnnotation;
    }

}
