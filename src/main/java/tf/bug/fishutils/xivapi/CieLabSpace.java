package tf.bug.fishutils.xivapi;

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;

public final class CieLabSpace extends ColorSpace {
    public static final CieLabSpace INSTANCE = new CieLabSpace();

    private CieLabSpace() {
        super(ColorSpace.TYPE_Lab, 3);
    }

    private static final double d50xw = 0.34773d;
    private static final double d50yw = 0.35952d;

    private static final double d50yn = 1.0d;
    private static final double d50xn = (d50yn / d50yw) * d50xw;
    private static final double d50zn = (d50yn / d50yw) * (1 - d50xw - d50yw);

    private static final double delta = 6.0d / 29.0d;

    private static double f(double t) {
        if(t > delta * delta * delta) {
            return Math.cbrt(t);
        } else {
            return (t / (3.0d * delta * delta)) + (4.0d / 29.0d);
        }
    }

    private static double inversef(double t) {
        if(t > delta) {
            return t * t * t;
        } else {
            return 3.0 * (delta * delta) * (t - (4.0d / 29.0d));
        }
    }

    @Override
    public float[] toRGB(float[] colorvalue) {
        return ICC_ColorSpace.getInstance(ICC_ColorSpace.CS_CIEXYZ).toRGB(this.toCIEXYZ(colorvalue));
    }

    @Override
    public float[] fromRGB(float[] rgbvalue) {
        return this.fromCIEXYZ(ICC_ColorSpace.getInstance(ICC_ColorSpace.CS_CIEXYZ).fromRGB(rgbvalue));
    }

    @Override
    public float[] toCIEXYZ(float[] colorvalue) {
        float l = colorvalue[0];
        float a = colorvalue[1];
        float b = colorvalue[2];

        colorvalue[0] = (float) (d50xn * inversef(((l + 16.0d) / 116.0d) + (a / 500.0d)));
        colorvalue[1] = (float) (d50yn * inversef((l + 16.0d) / 116.0d));
        colorvalue[2] = (float) (d50zn * inversef(((l + 16.0d) / 116.0d) - (b / 200.0d)));

        return colorvalue;
    }

    @Override
    public float[] fromCIEXYZ(float[] colorvalue) {
        float x = colorvalue[0];
        float y = colorvalue[1];
        float z = colorvalue[2];

        colorvalue[0] = (float) ((116.0d * f(y / d50yn)) - 16.0d);
        colorvalue[1] = (float) (500.0d * (f(x / d50xn) - f(y / d50yn)));
        colorvalue[2] = (float) (200.0d * (f(y / d50yn) - f(z / d50zn)));

        return colorvalue;
    }
}
