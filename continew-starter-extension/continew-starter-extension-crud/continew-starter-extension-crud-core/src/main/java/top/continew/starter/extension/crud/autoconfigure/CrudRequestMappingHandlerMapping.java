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

package top.continew.starter.extension.crud.autoconfigure;

import cn.hutool.core.util.ArrayUtil;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.condition.RequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.util.pattern.PathPatternParser;
import top.continew.starter.core.util.ExceptionUtils;
import top.continew.starter.extension.crud.annotation.CrudRequestMapping;
import top.continew.starter.extension.crud.enums.Api;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

/**
 * CRUD 请求映射器处理器映射器
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class CrudRequestMappingHandlerMapping extends RequestMappingHandlerMapping {

    @Override
    protected RequestMappingInfo getMappingForMethod(@NonNull Method method, @NonNull Class<?> handlerType) {
        RequestMappingInfo requestMappingInfo = super.getMappingForMethod(method, handlerType);
        if (null == requestMappingInfo) {
            return null;
        }
        // 如果没有声明 @CrudRequestMapping 注解，直接返回
        if (!handlerType.isAnnotationPresent(CrudRequestMapping.class)) {
            return requestMappingInfo;
        }
        CrudRequestMapping crudRequestMapping = handlerType.getDeclaredAnnotation(CrudRequestMapping.class);
        // 过滤 API，如果非本类中定义，且 API 列表中不包含，则忽略
        Api[] apiArr = crudRequestMapping.api();
        Api api = ExceptionUtils.exToNull(() -> Api.valueOf(method.getName().toUpperCase()));
        if (method.getDeclaringClass() != handlerType && !ArrayUtil.contains(apiArr, api)) {
            return null;
        }
        // 拼接路径（合并了 @RequestMapping 的部分能力）
        return this.getMappingForMethodWrapper(method, handlerType, crudRequestMapping);
    }

    private RequestMappingInfo getMappingForMethodWrapper(@NonNull Method method,
                                                          @NonNull Class<?> handlerType,
                                                          CrudRequestMapping crudRequestMapping) {
        RequestMappingInfo info = this.buildRequestMappingInfo(method);
        if (null != info) {
            RequestMappingInfo typeInfo = this.buildRequestMappingInfo(handlerType);
            if (null != typeInfo) {
                info = typeInfo.combine(info);
            }
            String prefix = crudRequestMapping.value();
            if (null != prefix) {
                RequestMappingInfo.BuilderConfiguration options = new RequestMappingInfo.BuilderConfiguration();
                options.setPatternParser(PathPatternParser.defaultInstance);
                info = RequestMappingInfo.paths(prefix).options(options).build().combine(info);
            }
        }
        return info;
    }

    private RequestMappingInfo buildRequestMappingInfo(AnnotatedElement element) {
        RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(element, RequestMapping.class);
        RequestCondition<?> condition = (element instanceof Class<?> clazz
            ? getCustomTypeCondition(clazz)
            : getCustomMethodCondition((Method)element));
        return (requestMapping != null ? super.createRequestMappingInfo(requestMapping, condition) : null);
    }
}
