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

import top.continew.starter.extension.crud.controller.BaseController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * CRUD API 策略
 *
 * @author Charles7c
 * @since 2.7.5
 */
public final class CrudApiStrategy {

    /**
     * 全局单例引用
     */
    public static final CrudApiStrategy INSTANCE = new CrudApiStrategy();

    /**
     * 处理器集合
     */
    public Map<Class<?>, CrudApiHandler<?>> handlerMap = new LinkedHashMap<>();

    private CrudApiStrategy() {
        registerDefaultHandler();
    }

    /**
     * 注册所有默认的处理器
     */
    public void registerDefaultHandler() {
        handlerMap.put(BaseController.class, new DefaultCrudApiHandler());
    }

    /**
     * 注册一个处理器
     *
     * @param handler 处理器
     */
    public void registerHandler(CrudApiHandler<?> handler) {
        handlerMap.put(handler.getHandlerControllerClass(), handler);
    }

    /**
     * 移除一个注解处理器
     *
     * @param controllerClass 控制器类
     */
    public void removeAnnotationHandler(Class<?> controllerClass) {
        handlerMap.remove(controllerClass);
    }
}
