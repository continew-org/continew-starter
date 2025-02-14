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

package top.continew.starter.sensitive.words.validate;

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import top.continew.starter.sensitive.words.service.SensitiveWordsService;

import java.util.List;

public class SensitiveWordValidator implements ConstraintValidator<SensitiveWord, String> {

    @Resource
    private SensitiveWordsService sensitiveWordsService;

    /**
     * 初始化方法，可以用自定义注解中获取值进行初始化
     *
     * @param {@link SensitiveWord } constraintAnnotation 注解值内容
     */
    @Override
    public void initialize(SensitiveWord constraintAnnotation) {

    }

    /**
     * 实际校验自定义注解 value 值
     *
     * @param {@link String} value 待检测字符串
     * @param {@link ConstraintValidatorContext } constraintValidatorContext 检测的上下文
     * @return boolean 是否通过检测
     */
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        List<String> res = sensitiveWordsService.check(value);
        if (!res.isEmpty()) {
            // 动态设置错误消息
            context.disableDefaultConstraintViolation(); // 禁用默认消息
            context.buildConstraintViolationWithTemplate("包含敏感词: " + String.join(",", res))
                    .addConstraintViolation();
            return false;
        }
        return true;
    }
}