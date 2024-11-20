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

package top.continew.starter.extension.crud.handler;

import top.continew.starter.extension.crud.annotation.CrudApi;
import top.continew.starter.extension.crud.controller.BaseController;

import java.lang.reflect.Method;

/**
 * CRUD API 处理器（默认）
 *
 * @author Charles7c
 * @since 2.7.5
 */
public class DefaultCrudApiHandler implements CrudApiHandler<BaseController> {

    @Override
    public Class getHandlerControllerClass() {
        return BaseController.class;
    }

    @Override
    public void preHandle(CrudApi crudApi, Method targetMethod, Class<?> targetClass) {
        // do nothing
    }
}
