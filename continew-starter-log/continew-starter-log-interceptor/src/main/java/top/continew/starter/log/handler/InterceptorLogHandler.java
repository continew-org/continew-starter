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

import cn.hutool.core.annotation.AnnotationUtil;
import cn.hutool.core.text.CharSequenceUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import top.continew.starter.log.AbstractLogHandler;
import top.continew.starter.log.model.LogRecord;

import java.lang.reflect.Method;

/**
 * 日志处理器-拦截器版实现
 *
 * @author Charles7c
 * @since 2.8.0
 */
public class InterceptorLogHandler extends AbstractLogHandler {

    @Override
    public void logDescription(LogRecord logRecord, Method targetMethod) {
        super.logDescription(logRecord, targetMethod);
        if (CharSequenceUtil.isNotBlank(logRecord.getDescription())) {
            return;
        }
        // 例如：@Operation(summary="新增部门") -> 新增部门
        Operation methodOperation = AnnotationUtil.getAnnotation(targetMethod, Operation.class);
        if (null != methodOperation) {
            logRecord.setDescription(CharSequenceUtil.blankToDefault(methodOperation.summary(), "请在该接口方法上指定日志描述"));
        }
    }

    @Override
    public void logModule(LogRecord logRecord, Method targetMethod, Class<?> targetClass) {
        super.logModule(logRecord, targetMethod, targetClass);
        if (CharSequenceUtil.isNotBlank(logRecord.getModule())) {
            return;
        }
        // 例如：@Tag(name = "部门管理") -> 部门管理
        Tag classTag = AnnotationUtil.getAnnotation(targetClass, Tag.class);
        if (null != classTag) {
            String name = classTag.name();
            logRecord.setModule(CharSequenceUtil.blankToDefault(name, "请在该接口类上指定所属模块"));
        }
    }
}
