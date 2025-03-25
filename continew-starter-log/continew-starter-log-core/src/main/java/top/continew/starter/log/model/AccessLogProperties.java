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

package top.continew.starter.log.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 访问日志配置属性
 *
 * @author echo
 * @author Charles7c
 * @since 2.10.0
 */
public class AccessLogProperties {

    /**
     * 是否打印访问日志（类似于 Nginx access log）
     * <p>
     * 不记录请求日志也支持开启打印访问日志
     * </p>
     */
    private boolean isPrint = false;

    /**
     * 是否打印请求参数（body/query/form）
     * <p>开启后，访问日志会打印请求参数</p>
     */
    private boolean isPrintRequestParam = false;

    /**
     * 是否自动截断超长参数值（如 base64、大文本）
     * <p>开启后，超过指定长度的参数值将会自动截断处理</p>
     */
    private boolean longParamTruncate = false;

    /**
     * 超长参数检测阈值（单位：字符）
     * <p>当参数值长度超过此值时，触发截断规则</p>
     * <p>默认：2000，仅在 {@link #longParamTruncate} 启用时生效</p>
     */
    private int longParamThreshold = 2000;

    /**
     * 超长参数最大保留长度（单位：字符）
     * <p>当参数超过 {@link #longParamThreshold} 时，强制截断到此长度</p>
     * <p>默认：50，仅在 {@link #longParamTruncate} 启用时生效</p>
     */
    private int longParamMaxLength = 50;

    /**
     * 截断后追加的后缀符号（如配置 "..." 会让截断内容更直观）
     * <p>建议配置 3-5 个非占宽字符，默认为 ...</p>
     * <p>仅在 {@link #longParamTruncate} 启用时生效</p>
     */
    private String longParamSuffix = "...";

    /**
     * 是否过滤敏感参数
     * <p>开启后会对敏感参数进行过滤，默认不过滤</p>
     */
    private boolean isParamSensitive = false;

    /**
     * 敏感参数字段列表（如：password,token,idCard）
     * <p>支持精确匹配（区分大小写）</p>
     * <p>示例值：password,oldPassword</p>
     */
    private List<String> sensitiveParams = new ArrayList<>();

    public boolean isPrint() {
        return isPrint;
    }

    public void setPrint(boolean print) {
        isPrint = print;
    }

    public boolean isPrintRequestParam() {
        return isPrintRequestParam;
    }

    public void setPrintRequestParam(boolean printRequestParam) {
        isPrintRequestParam = printRequestParam;
    }

    public boolean isLongParamTruncate() {
        return longParamTruncate;
    }

    public void setLongParamTruncate(boolean longParamTruncate) {
        this.longParamTruncate = longParamTruncate;
    }

    public int getLongParamThreshold() {
        return longParamThreshold;
    }

    public void setLongParamThreshold(int longParamThreshold) {
        this.longParamThreshold = longParamThreshold;
    }

    public int getLongParamMaxLength() {
        return longParamMaxLength;
    }

    public void setLongParamMaxLength(int longParamMaxLength) {
        this.longParamMaxLength = longParamMaxLength;
    }

    public String getLongParamSuffix() {
        return longParamSuffix;
    }

    public void setLongParamSuffix(String longParamSuffix) {
        this.longParamSuffix = longParamSuffix;
    }

    public boolean isParamSensitive() {
        return isParamSensitive;
    }

    public void setParamSensitive(boolean paramSensitive) {
        isParamSensitive = paramSensitive;
    }

    public List<String> getSensitiveParams() {
        return sensitiveParams;
    }

    public void setSensitiveParams(List<String> sensitiveParams) {
        this.sensitiveParams = sensitiveParams;
    }
}
