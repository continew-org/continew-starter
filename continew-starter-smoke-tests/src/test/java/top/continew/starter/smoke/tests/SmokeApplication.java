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

import org.springframework.boot.autoconfigure.SpringBootApplication;
import top.continew.starter.web.annotation.EnableGlobalResponse;

/**
 * 冒烟测试启动类：以消费方视角组合多个 Starter，验证可启动、可装配、可响应 HTTP
 *
 * @author Charles7c
 * @since 3.1.0
 */
@SpringBootApplication
@EnableGlobalResponse
public class SmokeApplication {
}
