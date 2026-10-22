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

package top.continew.starter.web.test.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.web.annotation.ExcludeFromGlobalResponse;
import top.continew.starter.web.test.exception.AliasedException;
import top.continew.starter.web.test.exception.AliasedStateException;
import top.continew.starter.web.test.exception.AliasedSubException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 全局响应测试控制器
 *
 * @author Charles7c
 * @since 3.0.0
 */
@Validated
@RestController
@RequestMapping("/test")
public class GlobalResponseTestController {

    /**
     * 普通 JSON 响应
     *
     * @return 响应数据
     */
    @GetMapping("/ok")
    public Map<String, String> ok() {
        return Map.of("value", "hello");
    }

    /**
     * 无返回值
     */
    @GetMapping("/void")
    public void voidMethod() {
    }

    /**
     * 方法级排除
     *
     * @return 原始响应数据
     */
    @GetMapping("/excluded")
    @ExcludeFromGlobalResponse
    public Map<String, String> excluded() {
        return Map.of("value", "raw");
    }

    /**
     * 返回类型级排除
     *
     * @return 原始响应数据
     */
    @GetMapping("/excluded-type")
    public ExcludedPayload excludedType() {
        return new ExcludedPayload("raw");
    }

    /**
     * URL 级排除
     *
     * @return 原始响应数据
     */
    @GetMapping("/excluded-url")
    public Map<String, String> excludedUrl() {
        return Map.of("value", "url-raw");
    }

    /**
     * ResponseEntity 响应
     *
     * @return 原始响应实体
     */
    @GetMapping("/entity")
    public ResponseEntity<Map<String, String>> entity() {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("value", "entity"));
    }

    /**
     * 文件响应
     *
     * @return 原始字节数据
     */
    @GetMapping("/file")
    public byte[] file() {
        return "raw-bytes".getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 未匹配的异常
     *
     * @return 不会返回
     */
    @GetMapping("/exception")
    public String exception() {
        throw new IllegalStateException("boom");
    }

    /**
     * 异常别名
     *
     * @return 不会返回
     */
    @GetMapping("/alias")
    public String alias() {
        throw new AliasedException();
    }

    /**
     * 业务方自定义处理器优先
     *
     * @return 不会返回
     */
    @GetMapping("/custom")
    public String custom() {
        throw new IllegalArgumentException("custom-boom");
    }

    /**
     * 参数校验
     *
     * @param request 请求参数
     * @return 响应数据
     */
    @PostMapping("/validation")
    public String validation(@Valid @RequestBody ValidationRequest request) {
        return request.name();
    }

    /**
     * 约束校验
     *
     * @param name 名称
     * @return 响应数据
     */
    @GetMapping("/constraint")
    public String constraint(
        @RequestParam @NotBlank(message = "param must not be blank") String name) {
        return name;
    }

    /**
     * 异常别名子类
     *
     * @return 不会返回
     */
    @GetMapping("/alias-subclass")
    public String aliasSubclass() {
        throw new AliasedSubException();
    }

    /**
     * 配置别名子类
     *
     * @return 不会返回
     */
    @GetMapping("/config-alias-subclass")
    public String configAliasSubclass() {
        throw new AliasedStateException();
    }
}
