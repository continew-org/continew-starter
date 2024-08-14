package top.continew.starter.web.handler;



import org.apache.commons.lang3.reflect.TypeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.parsers.ReturnTypeParser;
import org.springframework.core.MethodParameter;
import top.continew.starter.apidoc.util.DocUtils;
import top.continew.starter.web.model.R;

import java.lang.reflect.Type;

/**
 * SpringDoc 通用响应处理程序 --仅处理 doc 文档响应格式
 * <p>
 全局添加响应格式 {@link R}
 *
 * @Author echo
 * @date 2024/08/12
 */
public class DocGenericResponseHandler implements ReturnTypeParser {
  private static final Logger log = LoggerFactory.getLogger(DocGenericResponseHandler.class);

  private static final Class<R> R_TYPE = R.class;

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

    // 判断是否具有RestController 注解
    if (!DocUtils.hasRestControllerAnnotation(methodParameter.getContainingClass())) {
      return returnType;
    }
    // 如果为R<T> 则直接返回
    if (returnType.getTypeName().contains("top.continew.starter.web.model.R")) {
      return returnType;
    }
    // 如果是void类型，则返回R<Void>
    if (returnType == void.class || returnType == Void.class) {
      return TypeUtils.parameterize(R_TYPE, Void.class);
    }
    // 返回R<T>
    return TypeUtils.parameterize(R_TYPE, returnType);
  }
}
