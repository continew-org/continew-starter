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

package top.continew.starter.sensitive.words;

import cn.hutool.dfa.FoundWord;
import cn.hutool.dfa.WordTree;

import java.util.List;

public class SensitiveWordsTest {
    public static void main(String[] args) {
        WordTree tree = new WordTree();
        tree.addWord("大");
        tree.addWord("大土豆");
        tree.addWord("土豆");
        tree.addWord("刚出锅");
        tree.addWord("出锅");
        //正文
        String text = "我有一颗大土豆，刚出锅的";

        // 匹配到【大】，由于非密集匹配，因此从下一个字符开始查找，匹配到【土豆】接着被匹配
        // 由于【刚出锅】被匹配，由于非密集匹配，【出锅】被跳过
        List<String> matchAll = tree.matchAll(text, -1, false, true);
        for (String s : matchAll) {
            System.out.println(s);
        }
        System.out.println("-------------------");
        String match = tree.match(text);
        System.out.println(match);

        System.out.println("-------------------");
        FoundWord matchText = tree.matchWord(text);
        System.out.println(matchText.getFoundWord());
    }
}
