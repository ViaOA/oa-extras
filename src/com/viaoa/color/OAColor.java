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
package com.viaoa.color;

import java.awt.Color;

/**
 * Utility methods for selecting a readable foreground color (black or white)
 * based on the luminance of a background color. The methods compute the
 * perceived brightness of the background using Rec. 709 luminance weights and
 * return white for dark backgrounds and black for light backgrounds. <p>
 *
 * The utility accepts either a {@link Color} instance or raw RGB component
 * values and is useful when rendering text or icons over arbitrary background
 * colors to maintain sufficient contrast.
 */
public class OAColor {

	/**
	 * Returns a foreground color based on the specified background color.
	 *
	 * If the background color is {@code null}, this method returns {@link Color#white}.
	 * Otherwise, the RGB components of the background color are used to determine
	 * the foreground color.
	 *
	 * @param backgroundColor the background {@link Color}
	 * @return the selected foreground {@link Color}
	 */
    public static Color getForeground(Color backgroundColor) {
        if (backgroundColor == null) return Color.white;
        return getForegroundColor(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue());
    }
    
    /**
     * Returns a foreground color based on the specified background color.
     *
     * If the background color is {@code null}, this method returns {@link Color#white}.
     * Otherwise, the RGB components of the background color are used to determine
     * the foreground color.
     *
     * @param backgroundColor the background {@link Color}
     * @return the selected foreground {@link Color}
     */
    public static Color getForegroundColor(Color backgroundColor) {
        if (backgroundColor == null) return Color.white;
        return getForegroundColor(backgroundColor.getRed(), backgroundColor.getGreen(), backgroundColor.getBlue());
    }

    /**
     * Returns a foreground color based on the specified RGB background values.
     *
     * This method computes a luminance value using the supplied red, green,
     * and blue components and returns either black or white based on the result.
     *
     * @param r the red component value
     * @param g the green component value
     * @param b the blue component value
     * @return the selected foreground {@link Color}
     */
    public static Color getForegroundColor(int r, int g, int b) {
        float f = (0.2126f*r) + (0.7152f*g) + (0.0722f*b);
        int x = (f < 140) ? (0 | 0xFFFFFF) : 0;  // pick black or white color
        return new Color(x);
    }
    
}
