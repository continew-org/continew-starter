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

import java.lang.reflect.Method;

/**
 * CRUD API 处理器
 *
 * @author Charles7c
 * @since 2.7.5
 */
public interface CrudApiHandler {

    /**
     * 前置处理
     *
     * @param crudApi      CRUD API 注解
     * @param args         方法参数
     * @param targetMethod 目标方法
     * @param targetClass  目标类
     * @throws Exception 处理异常
     */
    void preHandle(CrudApi crudApi, Object[] args, Method targetMethod, Class<?> targetClass) throws Exception;
}
