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

package top.continew.starter.log.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.model.AccessLogContext;
import top.continew.starter.log.model.LogRecord;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Set;

/**
 * 日志处理器
 *
 * @author Charles7c
 * @since 2.8.0
 */
public interface LogHandler {

    /**
     * 开始日志记录
     *
     * @param startTime 开始时间
     * @param request   请求对象
     * @return 日志记录器
     */
    LogRecord.Started start(Instant startTime, HttpServletRequest request);

    /**
     * 结束日志记录
     *
     * @param started  开始日志记录器
     * @param endTime  结束时间
     * @param response 响应对象
     * @param includes 包含信息
     * @return 日志记录
     */
    LogRecord finish(LogRecord.Started started, Instant endTime, HttpServletResponse response, Set<Include> includes);

    /**
     * 结束日志记录
     *
     * @param started      开始日志记录器-
     * @param endTime      结束时间
     * @param response     响应对象
     * @param includes     包含信息
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     * @return 日志记录
     */
    LogRecord finish(LogRecord.Started started,
                     Instant endTime,
                     HttpServletResponse response,
                     Set<Include> includes,
                     Method targetMethod,
                     Class<?> targetClass);

    /**
     * 记录日志描述
     *
     * @param logRecord    日志记录
     * @param targetMethod 目标方法
     */
    void logDescription(LogRecord logRecord, Method targetMethod);

    /**
     * 记录所属模块
     *
     * @param logRecord    日志记录
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     */
    void logModule(LogRecord logRecord, Method targetMethod, Class<?> targetClass);

    /**
     * 获取日志包含信息
     *
     * @param includes     默认包含信息
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     * @return 日志包含信息
     */
    Set<Include> getIncludes(Set<Include> includes, Method targetMethod, Class<?> targetClass);

    /**
     * 处理访问日志开始请求
     *
     * @param accessLogContext 访问日志上下文
     */
    void processAccessLogStartReq(AccessLogContext accessLogContext);

    /**
     * 处理访问日志 结束请求
     *
     * @param accessLogContext 访问日志上下文
     */
    void processAccessLogEndReq(AccessLogContext accessLogContext);
}
