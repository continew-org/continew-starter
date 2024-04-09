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

package top.continew.starter.core.util;

import cn.hutool.extra.template.Template;
import cn.hutool.extra.template.TemplateConfig;
import cn.hutool.extra.template.TemplateEngine;
import cn.hutool.extra.template.TemplateUtil;

import java.util.Map;

/**
 * 模板工具类
 *
 * @author Charles7c
 * @since 1.0.0
 */
public class TemplateUtils {

    private static final String DEFAULT_TEMPLATE_PARENT_PATH = "templates";

    private TemplateUtils() {
    }

    /**
     * 渲染模板
     *
     * @param templatePath 模板路径
     * @param bindingMap   绑定参数（此 Map 中的参数会替换模板中的变量）
     * @return 渲染后的内容
     */
    public static String render(String templatePath, Map<?, ?> bindingMap) {
        return render(DEFAULT_TEMPLATE_PARENT_PATH, templatePath, bindingMap);
    }

    /**
     * 渲染模板
     *
     * @param parentPath   模板父目录
     * @param templatePath 模板路径
     * @param bindingMap   绑定参数（此 Map 中的参数会替换模板中的变量）
     * @return 渲染后的内容
     */
    public static String render(String parentPath, String templatePath, Map<?, ?> bindingMap) {
        TemplateEngine engine = TemplateUtil
            .createEngine(new TemplateConfig(parentPath, TemplateConfig.ResourceMode.CLASSPATH));
        Template template = engine.getTemplate(templatePath);
        return template.render(bindingMap);
    }
}
