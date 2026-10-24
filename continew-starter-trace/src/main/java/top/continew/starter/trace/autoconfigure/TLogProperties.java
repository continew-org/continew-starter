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

package top.continew.starter.trace.autoconfigure;

/**
 * 旧版 TLog 配置属性
 *
 * <p>
 * 链路追踪底层已替换为 Micrometer Tracing，TLog 配置项自 3.0.0 起不再生效。
 * 保留该配置类仅为兼容旧版 YAML 绑定，避免使用方升级后启动失败。
 * </p>
 *
 * @author Bryan.Zhang
 * @author Jasmine
 * @since 1.3.0
 */
public class TLogProperties {

    /**
     * 日志标签模板
     */
    private String pattern;

    /**
     * 自动打印调用参数和时间
     */
    private Boolean enableInvokeTimePrint;

    /**
     * 自定义 TraceId 生成器
     */
    private String idGenerator;

    /**
     * MDC 模式
     */
    private Boolean mdcEnable;

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public Boolean getEnableInvokeTimePrint() {
        return enableInvokeTimePrint;
    }

    public void setEnableInvokeTimePrint(Boolean enableInvokeTimePrint) {
        this.enableInvokeTimePrint = enableInvokeTimePrint;
    }

    public String getIdGenerator() {
        return idGenerator;
    }

    public void setIdGenerator(String idGenerator) {
        this.idGenerator = idGenerator;
    }

    public Boolean getMdcEnable() {
        return mdcEnable;
    }

    public void setMdcEnable(Boolean mdcEnable) {
        this.mdcEnable = mdcEnable;
    }

    /**
     * 是否配置了任意旧版 TLog 属性
     *
     * @return 配置了任意属性时返回 true
     */
    public boolean isConfigured() {
        return pattern != null || enableInvokeTimePrint != null || idGenerator != null
            || mdcEnable != null;
    }
}
