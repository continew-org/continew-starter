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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

/**
 * {@link ExecCmdUtil} 命令执行测试，仅使用无写入、无网络行为的只读命令。
 *
 * @author Charles7c
 * @since 3.1.0
 */
class ExecCmdUtilTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Windows：单参数命令走单字符串执行分支并返回标准输出")
    void shouldExecSingleArgumentCommandOnWindows() throws Exception {
        // 锁定当前行为：stdout 与 stderr 以单个空格拼接，stderr 为空时保留尾随空格
        assertThat(ExecCmdUtil.exec("cmd.exe /c echo hello")).isEqualTo("hello ");
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    @DisplayName("Windows：多参数命令走数组执行分支并返回标准输出")
    void shouldExecMultipleArgumentsCommandOnWindows() throws Exception {
        assertThat(ExecCmdUtil.exec("cmd.exe", "/c", "echo", "hello")).isEqualTo("hello ");
    }

    @Test
    @EnabledOnOs({
        OS.LINUX, OS.MAC
    })
    @DisplayName("Unix：命令经 /bin/sh -c 包装执行")
    void shouldExecCommandViaShellOnUnix() throws Exception {
        // 锁定当前行为：varargs 整体拼接在 /bin/sh -c 之后，须以单个元素传递完整脚本
        assertThat(ExecCmdUtil.exec("echo hello")).isEqualTo("hello ");
    }

    @Test
    @EnabledOnOs({
        OS.LINUX, OS.MAC
    })
    @DisplayName("Unix：脚本内管道可用")
    void shouldSupportPipeInScriptOnUnix() throws Exception {
        assertThat(ExecCmdUtil.exec("echo hi | tr a-z A-Z")).isEqualTo("HI ");
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    @DisplayName("create3rdSessionToken 返回 168 位字母数字随机串")
    void shouldCreate3rdSessionTokenWithFixedLengthOnLinux() throws Exception {
        String token = ExecCmdUtil.create3rdSessionToken();
        assertThat(token.trim()).hasSize(168).matches("[A-Za-z0-9]+");
    }
}
