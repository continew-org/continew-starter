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

package top.continew.starter.security.sensitivewords.validation;

import org.springframework.util.StringUtils;
import top.continew.starter.security.sensitivewords.service.SensitiveWordsService;

import jakarta.annotation.Resource;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * 敏感词校验器
 *
 * @author luoqiz
 * @author Charles7c
 * @since 3.0.0
 */
public class SensitiveWordsValidator implements ConstraintValidator<SensitiveWords, String> {

    @Resource
    private SensitiveWordsService sensitiveWordsService;

    private boolean failFast;

    @Override
    public void initialize(SensitiveWords annotation) {
        this.failFast = annotation.failFast();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (!StringUtils.hasText(value)) {
            return true;
        }
        List<String> hits;
        if (failFast) {
            String first = sensitiveWordsService.findFirst(value);
            hits = first == null ? List.of() : List.of(first);
        } else {
            hits = sensitiveWordsService.findAll(value);
        }
        if (hits.isEmpty()) {
            return true;
        }
        // 禁用默认消息，改为拼接命中词的动态消息
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
            context.getDefaultConstraintMessageTemplate() + ": " + String
                .join(",", hits))
            .addConstraintViolation();
        return false;
    }
}
