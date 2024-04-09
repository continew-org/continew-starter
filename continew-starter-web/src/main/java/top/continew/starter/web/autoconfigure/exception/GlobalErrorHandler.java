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

package top.continew.starter.web.autoconfigure.exception;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorViewResolver;
import org.springframework.boot.web.servlet.error.ErrorAttributes;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import top.continew.starter.web.model.R;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 全局错误处理器
 *
 * @author Charles7c
 * @since 1.0.0
 */
@RestController
public class GlobalErrorHandler extends BasicErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorHandler.class);

    @Resource
    private ObjectMapper objectMapper;

    public GlobalErrorHandler(ErrorAttributes errorAttributes,
                              ServerProperties serverProperties,
                              List<ErrorViewResolver> errorViewResolvers) {
        super(errorAttributes, serverProperties.getError(), errorViewResolvers);
    }

    @Override
    public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
        Map<String, Object> errorAttributeMap = super.getErrorAttributes(request, super.getErrorAttributeOptions(request, MediaType.TEXT_HTML));
        String path = (String)errorAttributeMap.get("path");
        HttpStatus status = super.getStatus(request);
        R<Object> result = R.fail(status.value(), (String)errorAttributeMap.get("error"));
        result.setData(path);
        try {
            response.setStatus(HttpStatus.OK.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), result);
        } catch (IOException e) {
            log.error("请求地址 [{}]，默认错误处理时发生 IO 异常。", path, e);
        }
        if (log.isErrorEnabled()) {
            log.error("请求地址 [{}]，发生错误，错误信息：{}。", path, JSONUtil.toJsonStr(errorAttributeMap));
        }
        return null;
    }

    @Override
    public ResponseEntity<Map<String, Object>> error(HttpServletRequest request) {
        Map<String, Object> errorAttributeMap = super.getErrorAttributes(request, super.getErrorAttributeOptions(request, MediaType.ALL));
        String path = (String)errorAttributeMap.get("path");
        HttpStatus status = super.getStatus(request);
        R<Object> result = R.fail(status.value(), (String)errorAttributeMap.get("error"));
        result.setData(path);
        if (log.isErrorEnabled()) {
            log.error("请求地址 [{}]，发生错误，错误信息：{}。", path, JSONUtil.toJsonStr(errorAttributeMap));
        }
        return new ResponseEntity<>(BeanUtil.beanToMap(result), HttpStatus.OK);
    }
}
