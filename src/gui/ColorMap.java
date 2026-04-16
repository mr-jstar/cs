package gui;

import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 *
 * @author jstar
 */
public class ColorMap {

    private float min = 100;
    private float max = 1000;
    private final static float BLUE_HUE;
    private final static float RED_HUE;
    
    public static enum Menu { HORIZONTAL, VERTICAL};
    
    static {
        Color color = Color.RED;
        RED_HUE = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
        color = Color.BLUE;
        BLUE_HUE = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null)[0];
    }

    public ColorMap() {
    }

    public ColorMap(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public void setMin(float newMin) {
        this.min = newMin;
    }

    public void setMax(float newMax) {
        this.max = newMax;
    }

    public int argbColorForValue(double dvalue) {
        float value = (float)dvalue;
        if (value < min || value > max) {
            return 0;
        }
        float hue = BLUE_HUE + (RED_HUE - BLUE_HUE) * (value - min) / (max - min);
        Color hsb = Color.getHSBColor(hue, 1.0f, 1.0f);
        int alpha = hsb.getAlpha();
        int red = hsb.getRed();
        int green = hsb.getGreen();
        int blue = hsb.getBlue();
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }
    
    public Color getColorForValue(double dvalue) {
        float value = (float)dvalue;
        if (value < min || value > max) {
            return Color.BLACK;
        }
        float hue = BLUE_HUE + (RED_HUE - BLUE_HUE) * (value - min) / (max - min);
        return Color.getHSBColor(hue, 1.0f, 1.0f);
    }

    public Image createColorScaleImage(int width, int height, ColorMap.Menu orientation) {
        BufferedImage image = new BufferedImage(width, height,BufferedImage.TYPE_INT_ARGB);
        if (orientation == Menu.HORIZONTAL) {
            for (int x = 0; x < width; x++) {
                double value = min + (max - min) * x / width;
                int color = argbColorForValue((float)value);
                for (int y = 0; y < height; y++) {
                    image.setRGB(x, y, color);
                }
            }
        } else {
            for (int y = 0; y < height; y++) {
                double value = max - (max - min) * y / height;
                int color = argbColorForValue((float)value);
                for (int x = 0; x < width; x++) {
                    image.setRGB(x, y, color);
                }
            }
        }
        return image;
    }
}
