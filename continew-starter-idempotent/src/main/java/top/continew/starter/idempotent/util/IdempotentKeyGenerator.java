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

package top.continew.starter.idempotent.util;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.springframework.util.DigestUtils;

import java.util.Arrays;

/**
 * 定义幂等key工具类
 *
 * @version 1.0
 * @Author loach
 * @Date 2025-03-07 20:13
 * @Package top.continew.starter.idempotent.util.IdempotentKeyGenerator
 */
public class IdempotentKeyGenerator {

    /**
     * 创建key
     *
     * @param prefix    幂等key前缀
     * @param joinPoint 切面参数
     * @param request   请求头
     * @return
     */
    public static String generateKey(String prefix, JoinPoint joinPoint, HttpServletRequest request) {
        // 可选使用header中的token
        String token = request.getHeader("Authorization");
        if (token == null || token.isEmpty()) {
            token = request.getParameter("Authorization");
        }

        String methodSignature = joinPoint.getSignature().toString();
        String argsStr = Arrays.toString(joinPoint.getArgs());

        // 如果没有token，只使用方法签名和参数生成key
        String rawKey = prefix + methodSignature + argsStr + (token != null ? token : "");
        return DigestUtils.md5DigestAsHex(rawKey.getBytes());
    }
}