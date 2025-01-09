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

package top.continew.starter.log;

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import top.continew.starter.core.validation.ValidationUtils;
import top.continew.starter.log.annotation.Log;
import top.continew.starter.log.enums.Include;
import top.continew.starter.log.http.servlet.RecordableServletHttpRequest;
import top.continew.starter.log.http.servlet.RecordableServletHttpResponse;
import top.continew.starter.log.model.LogRecord;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * 日志处理器基类
 *
 * @author Charles7c
 * @since 2.8.0
 */
public abstract class AbstractLogHandler implements LogHandler {

    @Override
    public LogRecord.Started start(Instant startTime, HttpServletRequest request) {
        return LogRecord.start(startTime, new RecordableServletHttpRequest(request));
    }

    @Override
    public LogRecord finish(LogRecord.Started started,
                            Instant endTime,
                            HttpServletResponse response,
                            Set<Include> includes,
                            Method targetMethod,
                            Class<?> targetClass) {
        Set<Include> includeSet = this.getIncludes(includes, targetMethod, targetClass);
        LogRecord logRecord = this.finish(started, endTime, response, includeSet);
        // 记录日志描述
        if (includeSet.contains(Include.DESCRIPTION)) {
            this.logDescription(logRecord, targetMethod);
        }
        // 记录所属模块
        if (includeSet.contains(Include.MODULE)) {
            this.logModule(logRecord, targetMethod, targetClass);
        }
        return logRecord;
    }

    @Override
    public LogRecord finish(LogRecord.Started started,
                            Instant endTime,
                            HttpServletResponse response,
                            Set<Include> includes) {
        return started.finish(endTime, new RecordableServletHttpResponse(response, response.getStatus()), includes);
    }

    /**
     * 记录日志描述
     *
     * @param logRecord    日志记录
     * @param targetMethod 目标方法
     */
    @Override
    public void logDescription(LogRecord logRecord, Method targetMethod) {
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
        // 例如：@Log("新增部门") -> 新增部门
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.value())) {
            logRecord.setDescription(methodLog.value());
            return;
        }
        // 例如：@Operation(summary="新增部门") -> 新增部门
        Operation methodOperation = AnnotationUtil.getAnnotation(targetMethod, Operation.class);
        if (null != methodOperation) {
            logRecord.setDescription(CharSequenceUtil.blankToDefault(methodOperation
                .summary(), "请在该接口方法的 @Operation 上添加 summary 来指定日志描述"));
        }
        ValidationUtils.throwIfBlank(logRecord.getDescription(), "请在该接口方法上添加 @Log 来指定日志描述");
    }

    /**
     * 记录所属模块
     *
     * @param logRecord    日志记录
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     */
    @Override
    public void logModule(LogRecord logRecord, Method targetMethod, Class<?> targetClass) {
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
        // 例如：@Log(module = "部门管理") -> 部门管理
        // 方法级注解优先级高于类级注解
        if (null != methodLog && CharSequenceUtil.isNotBlank(methodLog.module())) {
            logRecord.setModule(methodLog.module());
            return;
        }
        Log classLog = AnnotationUtil.getAnnotation(targetClass, Log.class);
        if (null != classLog && CharSequenceUtil.isNotBlank(classLog.module())) {
            logRecord.setModule(classLog.module());
            return;
        }
        // 例如：@Tag(name = "部门管理") -> 部门管理
        Tag classTag = AnnotationUtil.getAnnotation(targetClass, Tag.class);
        if (null != classTag) {
            String name = classTag.name();
            logRecord.setModule(CharSequenceUtil.blankToDefault(name, "请在该接口类的 @Tag 上添加 name 来指定所属模块"));
        }
        ValidationUtils.throwIfBlank(logRecord.getModule(), "请在该接口方法或接口类上添加 @Log 来指定所属模块");
    }

    @Override
    public Set<Include> getIncludes(Set<Include> includes, Method targetMethod, Class<?> targetClass) {
        Log classLog = AnnotationUtil.getAnnotation(targetClass, Log.class);
        Set<Include> includeSet = new HashSet<>(includes);
        if (null != classLog) {
            this.processInclude(includeSet, classLog);
        }
        // 方法级注解优先级高于类级注解
        Log methodLog = AnnotationUtil.getAnnotation(targetMethod, Log.class);
        if (null != methodLog) {
            this.processInclude(includeSet, methodLog);
        }
        return includeSet;
    }

    /**
     * 处理日志包含信息
     *
     * @param includes      日志包含信息
     * @param logAnnotation Log 注解
     */
    private void processInclude(Set<Include> includes, Log logAnnotation) {
        Include[] includeArr = logAnnotation.includes();
        if (includeArr.length > 0) {
            includes.addAll(Set.of(includeArr));
        }
        Include[] excludeArr = logAnnotation.excludes();
        if (excludeArr.length > 0) {
            includes.removeAll(Set.of(excludeArr));
        }
    }
}
