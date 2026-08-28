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

package top.continew.starter.license.util;

import cn.hutool.core.util.ArrayUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;

/**
 * 运行命令行工具类
 *
 * @author loach
 * @since 2.12.0
 */
public class ExecCmdUtil {

    private static final String CREATE_3RDSESSION_SHELL_SCRIPT =
        "head -n 80 /dev/urandom | tr -dc A-Za-z0-9 | head -c 168";

    private ExecCmdUtil() {
    }

    /**
     * 执行cmd命令(shell脚本)
     *
     * @param cmd linux sh/windows bat命令
     * @return String 返回打印信息
     */
    public static String exec(String... cmd) throws IOException {
        Process process;
        if (System.getProperty("os.name").contains("Windows")) {
            if (cmd != null && cmd.length == 1) {
                process = Runtime.getRuntime().exec(cmd[0]);
            } else {
                process = Runtime.getRuntime().exec(cmd);
            }
        } else {
            cmd = ArrayUtil.addAll(new String[] {"/bin/sh", "-c"}, cmd);
            process = Runtime.getRuntime().exec(cmd);
        }

        String print = readProcess(process.getInputStream());
        String err = readProcess(process.getErrorStream());
        return print + " " + err;
    }

    /**
     * 读取 InputStream 内容为字符串（使用 GBK 编码）。
     *
     * @param in 输入流
     * @return 拼接后的字符串，读取失败返回空字符串
     */
    private static String readProcess(InputStream in) {
        try (LineNumberReader print = new LineNumberReader(new InputStreamReader(in, "GBK"))) {
            StringBuffer sb = new StringBuffer();
            String line;
            while ((line = print.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

    /**
     * 执行linux命令(shell脚本)生成3rd_session随机数
     */
    public static String create3rdSessionToken() throws IOException {
        return exec(CREATE_3RDSESSION_SHELL_SCRIPT);
    }
}
