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

import top.continew.starter.web.util.SpringWebUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 访问日志输出配置
 *
 * @author echo
 * @since 2.8.3
 */
public class AccessLogProperties {

    /**
     * 是否打印日志，开启后可打印访问日志（类似于 Nginx access log）
     * <p>
     * 不记录日志也支持开启打印访问日志
     * </p>
     */
    private boolean isPrint = false;

    /**
     * 放行路由
     */
    private List<String> excludePatterns = new ArrayList<>();

    /**
     * 是否记录请求参数（body/query/form）
     * <p>开启后会在日志中输出请求参数</p>
     */
    private boolean isReqParams = false;

    /**
     * 是否自动截断超长参数值（如 base64、大文本）
     * <p>开启后会对超过指定长度的参数值进行截断处理</p>
     */
    private boolean truncateLongParams = false;

    /**
     * 超长参数检测阈值（单位：字符）
     * <p>当参数值长度超过此值时，触发截断规则</p>
     * <p>默认：2000</p>
     */
    private int ultraLongParamThreshold = 2000;

    /**
     * 超长参数最大展示长度（单位：字符）
     * <p>当参数超过ultraLongParamThreshold时，强制截断到此长度</p>
     * <p>默认：50</p>
     */
    private int ultraLongParamMaxLength = 50;

    /**
     * 截断后追加的后缀符号（如配置 "..." 会让截断内容更直观）
     * <p>建议配置 3-5 个非占宽字符，默认为空不追加</p>
     */
    private String truncateSuffix = "...";

    /**
     * 是否过滤敏感参数
     * <p>开启后会对敏感参数进行过滤，默认不过滤</p>
     */
    private boolean isSensitiveParams = false;

    /**
     * 敏感参数字段列表（如：password,token,idCard）
     * <p>支持精确匹配（区分大小写）</p>
     * <p>示例值：password,oldPassword</p>
     */
    private List<String> sensitiveParamList = new ArrayList<>();

    public boolean isPrint() {
        return isPrint;
    }

    public void setPrint(boolean print) {
        isPrint = print;
    }

    public List<String> getExcludePatterns() {
        return excludePatterns;
    }

    public void setExcludePatterns(List<String> excludePatterns) {
        this.excludePatterns = excludePatterns;
    }

    public boolean isReqParams() {
        return isReqParams;
    }

    public void setReqParams(boolean reqParams) {
        isReqParams = reqParams;
    }

    public boolean isTruncateLongParams() {
        return truncateLongParams;
    }

    public void setTruncateLongParams(boolean truncateLongParams) {
        this.truncateLongParams = truncateLongParams;
    }

    public int getUltraLongParamThreshold() {
        return ultraLongParamThreshold;
    }

    public void setUltraLongParamThreshold(int ultraLongParamThreshold) {
        this.ultraLongParamThreshold = ultraLongParamThreshold;
    }

    public int getUltraLongParamMaxLength() {
        return ultraLongParamMaxLength;
    }

    public void setUltraLongParamMaxLength(int ultraLongParamMaxLength) {
        this.ultraLongParamMaxLength = ultraLongParamMaxLength;
    }

    public String getTruncateSuffix() {
        return truncateSuffix;
    }

    public void setTruncateSuffix(String truncateSuffix) {
        this.truncateSuffix = truncateSuffix;
    }

    public boolean isSensitiveParams() {
        return isSensitiveParams;
    }

    public void setSensitiveParams(boolean sensitiveParams) {
        isSensitiveParams = sensitiveParams;
    }

    public List<String> getSensitiveParamList() {
        return sensitiveParamList;
    }

    public void setSensitiveParamList(List<String> sensitiveParamList) {
        this.sensitiveParamList = sensitiveParamList;
    }

    /**
     * 是否匹配放行路由
     *
     * @param uri 请求 URI
     * @return 是否匹配
     */
    public boolean isMatch(String uri) {
        return this.getExcludePatterns().stream().anyMatch(pattern -> SpringWebUtils.isMatch(uri, pattern));
    }
}
