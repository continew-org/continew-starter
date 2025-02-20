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

package top.continew.starter.web.autoconfigure.response;

import cn.hutool.core.util.ClassUtil;
import org.apache.commons.lang3.reflect.TypeUtils;
import org.springdoc.core.parsers.ReturnTypeParser;
import org.springframework.core.MethodParameter;
import top.continew.starter.apidoc.util.DocUtils;

import java.lang.reflect.Type;

/**
 * SpringDoc 全局响应处理器
 * <p>
 * 接口文档全局添加响应格式 {@link com.feiniaojin.gracefulresponse.data.Response}
 * </p>
 *
 * @author echo
 * @since 2.5.2
 */
public class ApiDocGlobalResponseHandler implements ReturnTypeParser {

    private final GlobalResponseProperties globalResponseProperties;
    private final Class<Object> responseClass;

    public ApiDocGlobalResponseHandler(GlobalResponseProperties globalResponseProperties) {
        this.globalResponseProperties = globalResponseProperties;
        this.responseClass = ClassUtil.loadClass(globalResponseProperties.getResponseClassFullName());
    }

    /**
     * 获取返回类型
     *
     * @param methodParameter 方法参数
     * @return {@link Type }
     */
    @Override
    public Type getReturnType(MethodParameter methodParameter) {
        // 获取返回类型
        Type returnType = ReturnTypeParser.super.getReturnType(methodParameter);
        // 判断是否具有 RestController 注解
        if (!DocUtils.hasRestControllerAnnotation(methodParameter.getContainingClass())) {
            return returnType;
        }
        // 如果为响应类型，则直接返回
        if (returnType.getTypeName().contains(globalResponseProperties.getResponseClassFullName())) {
            return returnType;
        }
        // 如果是 void类型，则返回 R<Void>
        if (returnType == void.class || returnType == Void.class) {
            return TypeUtils.parameterize(responseClass, Void.class);
        }
        // 返回 R<T>
        return TypeUtils.parameterize(responseClass, returnType);
    }
}
