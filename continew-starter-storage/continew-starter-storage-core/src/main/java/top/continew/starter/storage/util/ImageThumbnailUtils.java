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

package top.continew.starter.storage.util;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * 图像缩略图工具
 *
 * @author echo
 * @date 2024/12/20 16:49
 */
public class ImageThumbnailUtils {

    // 默认缩略图尺寸：100x100
    private static final int DEFAULT_WIDTH = 100;
    private static final int DEFAULT_HEIGHT = 100;

    /**
     * 根据输入流生成默认大小（100x100）的缩略图并写入输出流
     *
     * @param inputStream  原始图片的输入流
     * @param outputStream 缩略图输出流
     * @param suffix       后缀
     * @throws IOException IOException
     */
    public static void generateThumbnail(InputStream inputStream,
                                         OutputStream outputStream,
                                         String suffix) throws IOException {
        generateThumbnail(inputStream, outputStream, DEFAULT_WIDTH, DEFAULT_HEIGHT, suffix);
    }

    /**
     * 根据输入流和自定义尺寸生成缩略图并写入输出流
     *
     * @param inputStream  原始图片的输入流
     * @param outputStream 缩略图输出流
     * @param width        缩略图宽度
     * @param height       缩略图高度
     * @param suffix       后缀
     * @throws IOException IOException
     */
    public static void generateThumbnail(InputStream inputStream,
                                         OutputStream outputStream,
                                         int width,
                                         int height,
                                         String suffix) throws IOException {
        // 读取原始图片
        BufferedImage originalImage = ImageIO.read(inputStream);

        // 调整图片大小
        Image tmp = originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage thumbnail = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        // 画出缩略图
        Graphics2D g2d = thumbnail.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        // 写入输出流
        ImageIO.write(thumbnail, suffix, outputStream);
    }
}
