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

package top.continew.starter.smoke.tests;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 冒烟测试端点
 *
 * @author Charles7c
 * @since 3.1.0
 */
@RestController
class SmokeController {

    /**
     * 正常响应端点：返回普通对象，验证统一响应包装
     *
     * @return 含 value 字段的普通对象，由全局响应包装为统一结构
     */
    @GetMapping("/smoke/ok")
    public Map<String, String> ok() {
        return Map.of("value", "smoke");
    }

    /**
     * 参数校验端点：name 为空触发校验失败，验证统一异常处理
     *
     * @param name
     *            非空校验参数，传入空值时由全局异常处理返回统一异常结构
     * @return 原样返回 name
     */
    @GetMapping("/smoke/validate")
    public String validate(@RequestParam @NotBlank String name) {
        return name;
    }
}
