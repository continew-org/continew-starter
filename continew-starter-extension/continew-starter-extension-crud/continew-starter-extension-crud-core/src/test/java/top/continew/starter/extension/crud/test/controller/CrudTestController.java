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

package top.continew.starter.extension.crud.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import top.continew.starter.extension.crud.annotation.CrudRequestMapping;

/**
 * CRUD 测试控制器
 *
 * @author Charles7c
 * @since 3.0.0
 */
@RestController
@CrudRequestMapping("/test-crud")
public class CrudTestController {

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }
}
