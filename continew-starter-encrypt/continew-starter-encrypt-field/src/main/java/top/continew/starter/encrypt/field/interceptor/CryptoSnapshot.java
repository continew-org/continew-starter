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

package top.continew.starter.encrypt.field.interceptor;

import cn.hutool.core.util.ReflectUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 写入参数快照
 *
 * <p>记录参数被就地加密前的原值，写入完成后恢复，保证调用方实体在写入结束后保持明文，
 * 同一实体重复写入不会二次加密。</p>
 *
 * @author Charles7c
 * @since 3.1.0
 */
class CryptoSnapshot {

    private final List<Runnable> restoreActions = new ArrayList<>();

    /**
     * 记录实体字段的加密前原值
     *
     * @param owner    实体
     * @param field    字段
     * @param oldValue 原值
     */
    void recordField(Object owner, Field field, Object oldValue) {
        this.restoreActions.add(() -> ReflectUtil.setFieldValue(owner, field, oldValue));
    }

    /**
     * 记录 Map 条目的加密前原值（Wrapper 的 paramNameValuePairs）
     *
     * @param map      目标 Map
     * @param key      键
     * @param oldValue 原值
     */
    void recordMapEntry(Map<String, Object> map, String key, Object oldValue) {
        this.restoreActions.add(() -> map.put(key, oldValue));
    }

    /**
     * 恢复所有已记录的原值
     */
    void restore() {
        for (Runnable restoreAction : this.restoreActions) {
            restoreAction.run();
        }
        this.restoreActions.clear();
    }
}
