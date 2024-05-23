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

package top.continew.starter.data.mybatis.flex.datapermission;

import org.aspectj.lang.annotation.*;

@Aspect
public class DataPermissionAspect {

    // ThreadLocal用于存储注解信息
    private static final ThreadLocal<DataPermission> THREAD_LOCAL = new ThreadLocal<>();

    @Pointcut("@annotation(dataPermission)")
    public void dataPermissionPointcut(DataPermission dataPermission) {
    }

    @Before("dataPermissionPointcut(dataPermission)")
    public void beforeMethod(DataPermission dataPermission) {
        THREAD_LOCAL.set(dataPermission);
    }

    @AfterThrowing(pointcut = "dataPermissionPointcut(dataPermission)")
    public void afterThrowingMethod(DataPermission dataPermission) {
        THREAD_LOCAL.remove();
    }

    @After("dataPermissionPointcut(dataPermission)")
    public void afterMethod(DataPermission dataPermission) {
        THREAD_LOCAL.remove();
    }

    public static DataPermission currentDataPermission() {
        return THREAD_LOCAL.get();
    }

}
