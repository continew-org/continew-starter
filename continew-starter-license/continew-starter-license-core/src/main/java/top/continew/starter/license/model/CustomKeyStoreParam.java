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

package top.continew.starter.license.model;

import de.schlichtherle.license.AbstractKeyStoreParam;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 自定义密钥存储参数
 *
 * @author loach
 * @author echo
 * @since 2.12.0
 */
public class CustomKeyStoreParam extends AbstractKeyStoreParam {

    /**
     * 密钥路径，可为磁盘路径，也可为项目资源文件里的路径,如果为磁盘路径需重写getStream()方法
     */
    private String storePath;

    /**
     * 公钥或私钥的别名
     */
    private String alias;

    /**
     * 访问公钥/私钥库的密码
     */
    private String storePass;

    /**
     * 公钥/私钥的密码
     */
    private String keyPass;

    public CustomKeyStoreParam(Class clazz, String s) {
        super(clazz, s);
    }

    public CustomKeyStoreParam(Class clazz, String resource, String alias, String storePass,
        String keyPass) {
        super(clazz, resource);
        this.storePath = resource;
        this.alias = alias;
        this.storePass = storePass;
        this.keyPass = keyPass;
    }

    @Override
    public String getAlias() {
        return alias;
    }

    @Override
    public String getStorePwd() {
        return storePass;
    }

    @Override
    public String getKeyPwd() {
        return keyPass;
    }

    @Override
    public InputStream getStream() throws IOException {
        return new FileInputStream(storePath);
    }
}
