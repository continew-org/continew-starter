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

package top.continew.starter.storage.enums;

import cn.hutool.core.util.StrUtil;
import top.continew.starter.core.enums.BaseEnum;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 文件类型枚举
 *
 * @author Charles7c
 * @since 2.9.0
 */
public enum FileType implements BaseEnum<Integer> {

    /**
     * 其他
     */
    UNKNOWN(1, "其他", Collections.emptyList()),

    /**
     * 图片
     */
    IMAGE(2, "图片", List
        .of("jpg", "jpeg", "png", "gif", "bmp", "webp", "ico", "psd", "tiff", "dwg", "jxr", "apng", "xcf")),

    /**
     * 文档
     */
    DOC(3, "文档", List.of("txt", "pdf", "doc", "xls", "ppt", "docx", "xlsx", "pptx")),

    /**
     * 视频
     */
    VIDEO(4, "视频", List.of("mp4", "avi", "mkv", "flv", "webm", "wmv", "m4v", "mov", "mpg", "rmvb", "3gp")),

    /**
     * 音频
     */
    AUDIO(5, "音频", List.of("mp3", "flac", "wav", "ogg", "midi", "m4a", "aac", "amr", "ac3", "aiff")),;

    private final Integer value;
    private final String description;
    private final List<String> extensions;

    /**
     * 根据扩展名查询
     *
     * @param extension 扩展名
     * @return 文件类型
     */
    public static FileType getByExtension(String extension) {
        return Arrays.stream(FileType.values())
            .filter(t -> t.getExtensions().contains(StrUtil.emptyIfNull(extension).toLowerCase()))
            .findFirst()
            .orElse(FileType.UNKNOWN);
    }

    FileType(Integer value, String description, List<String> extensions) {
        this.value = value;
        this.description = description;
        this.extensions = extensions;
    }

    public List<String> getExtensions() {
        return this.extensions;
    }

    @Override
    public Integer getValue() {
        return this.value;
    }

    @Override
    public String getDescription() {
        return this.description;
    }

    @Override
    public String getColor() {
        return BaseEnum.super.getColor();
    }
}