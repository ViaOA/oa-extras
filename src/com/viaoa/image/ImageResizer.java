/*
 * Copyright 1999–2025 ViaOA (info@viaoa.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.viaoa.image;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.imageio.ImageIO;

/**
 * Utility for resizing an image file and writing the scaled result as a JPEG.
 * The class loads an image using AWT, blocks until it is fully loaded,
 * generates a proportionally scaled instance using
 * {@link Image#getScaledInstance(int, int, int)}, renders it into a
 * {@link BufferedImage}, and writes the result using {@link ImageIO}. <p>
 *
 * Scaling is controlled by a resize factor: the original width is multiplied
 * by the supplied factor and the height is adjusted automatically to preserve
 * the aspect ratio. <p>
 *
 * Image loading is performed using a {@link MediaTracker} to ensure the image
 * and its scaled instance are fully realized before drawing. This class is
 * intended for standalone use through {@link #doResize(String, String, double)}
 * or the command-line interface provided by {@link #main(String[])}.
 */
public class ImageResizer extends Panel {

	/**
	 * Resizes an image file and writes the scaled result as a JPEG image.
	 * <p>
	 * The original image is loaded using the AWT {@link Toolkit}, fully realized
	 * using a {@link MediaTracker}, and then scaled proportionally based on the
	 * supplied resize factor. The resulting image is rendered into a
	 * {@link BufferedImage} and written to disk using {@link ImageIO}.
	 * <p>
	 * The new image width is computed as {@code originalWidth * factor}, and
	 * the height is automatically adjusted to preserve the original aspect ratio.
	 *
	 * @param originalImage the file name of the source image to resize
	 * @param newImage the file name for the resized output image
	 * @param factor the scaling factor applied to the original width
	 */
    public void doResize(String originalImage, String newImage, double factor) {
        Image img = getToolkit().getImage(originalImage);
        if (img == null) return;
        loadImage(img);
        int iw = img.getWidth(this);
        int ih = img.getHeight(this);

        //Reduce the image
        int w = (int)(iw * factor);
        Image i2 = img.getScaledInstance(w, -1, 0);
        loadImage(i2);

        //Load it into a BufferedImage
        int i2w = i2.getWidth(this);
        int i2h = i2.getHeight(this);
        BufferedImage bi = new BufferedImage(i2w, i2h, BufferedImage.TYPE_INT_RGB);
        Graphics2D big = bi.createGraphics();
        big.drawImage(i2,0,0,this);

        // Use JPEGImageEncoder to write the BufferedImage to a file
        try{
            OutputStream os = new FileOutputStream(newImage);
            // 20151215
            ImageIO.write(bi, "jpeg", os);
            /*was:
            JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(os);
            encoder.encode(bi);
            os.flush();
            os.close();
            img.flush();
            i2.flush();
            bi.flush();
            */
        }
        catch(IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Forces an {@link Image} to be fully loaded and realized.
     * <p>
     * This method uses a {@link MediaTracker} to block until the image data
     * has been completely loaded, ensuring that width and height information
     * is available before further processing.
     *
     * @param img the image to load and realize
     */
    private void loadImage(Image img){
        try {
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(img, 0);
            tracker.waitForID(0);
        }
        catch (Exception e) {
        }
    }
}
