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

import cn.hutool.core.map.MapUtil;

import java.util.*;

/**
 * Map 工具类
 *
 * @author Charles7c
 * @since 2.1.0
 */
public class MapUtils {

    private MapUtils() {
    }

    /**
     * 转换为 Properties 对象
     *
     * @param source 数据源
     * @return Properties 对象
     */
    public static Properties toProperties(Map<String, String> source) {
        Properties properties = new Properties();
        properties.putAll(source);
        return properties;
    }

    /**
     * 深度合并两个map
     * 需要用新的map接收
     *
     * @param to   需要合并的map
     * @param from 需要被合并的map
     * @return Map<String, Object> 必须重新使用的map
     * @author luoqiz
     * @since 2.14.0
     */
    public static Map<String, Object> mergeMap(Map<String, Object> to, Map<String, Object> from) {
        if (MapUtil.isEmpty(to)) {
            return from;
        }
        if (MapUtil.isEmpty(from)) {
            return to;
        }
        if (MapUtil.isEmpty(to) && MapUtil.isEmpty(from)) {
            return new HashMap<>();
        }
        Set<Map.Entry<String, Object>> entries = to.entrySet();
        Iterator<Map.Entry<String, Object>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Object> kv = iterator.next();
            String toKey = kv.getKey();
            Object toValue = kv.getValue();
            Object fromValue = from.get(toKey);
            if (fromValue != null) {
                if (toValue instanceof Map) {
                    Map<String, Object> childTo = (Map<String, Object>)toValue;
                    mergeMap(childTo, (Map<String, Object>)fromValue);
                } else {
                    to.put(toKey, fromValue);
                }
            }
        }

        Set<String> keys = from.keySet();
        for (String key : keys) {
            if (!to.containsKey(key)) {
                to.put(key, from.get(key));
            }
        }
        return to;
    }
}
