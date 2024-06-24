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

package top.continew.starter.core.exception;

/**
 * 接口返回码与消息 所有业务异常都要继承该接口
 *
 * @author Jasmine
 * @since 2.2.0
 */
public interface ResultInfoInterface {

    /**
     * 获取编码
     *
     * @return String
     */
    int getCode();

    /**
     * 国际化消息key
     *
     * @return
     */
    default String getMessageKey() {
        return "";
    }

    /**
     * 获取默认消息 若从国际化文件里没有获取到值，就取默认值
     *
     * @return String
     */
    String getDefaultMessage();
}
