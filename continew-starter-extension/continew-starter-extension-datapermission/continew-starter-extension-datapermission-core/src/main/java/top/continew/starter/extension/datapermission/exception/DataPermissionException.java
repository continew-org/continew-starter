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

package top.continew.starter.extension.datapermission.exception;

import top.continew.starter.core.exception.BaseException;

/**
 * 数据权限异常
 *
 * @author Charles7c
 * @since 2.13.2
 */
public class DataPermissionException extends BaseException {

    public DataPermissionException(String message) {
        super(message);
    }

    public DataPermissionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static DataPermissionException unsupportedDataScope(String dataScope) {
        return new DataPermissionException("Unsupported data scope: " + dataScope);
    }

    public static DataPermissionException unsupportedDatabase(String database) {
        return new DataPermissionException("Unsupported database for data permission: " + database);
    }

    public static DataPermissionException invalidUserData(String message) {
        return new DataPermissionException("Invalid user data: " + message);
    }

    public static DataPermissionException methodNotFound(String mappedStatementId) {
        return new DataPermissionException(
            "Method not found for data permission: " + mappedStatementId);
    }
}
