package peasyGradients.colourSpaces;

/**
 * Hue, Saturation, Lightness
 * https://github.com/avp/chroma/blob/master/src/main/java/com/chroma/RgbColor.java
 * 
 * @author micycle1
 *
 */
public final class HSL {

	public static float[] rgb2hsl(int rgba) {
		float[] hsb = RGB.rgbToHsb(rgba);
		float s, l;

		l = (2 - hsb[1]) * hsb[2];
		s = hsb[1] * hsb[2];
		s /= (l <= 1) ? (l) : (2 - l);
		l /= 2;
		return new float[] { hsb[0], s, l, hsb[3] };
	}
	
	public static void hsl2rgb(double[] HSL) {
		
	}
	
	private static void getHSV() {
//	    double h = ChromaUtil.clamp(hue, 0, 360);
//	    double s = ChromaUtil.clamp(saturation, 0, 1);
//	    double l = ChromaUtil.clamp(lightness, 0, 1);
//
//	    s *= ((l < 0.5) ? l : 1 - l);
//	    double saturationV = (2 * s) / (l + s);
//	    double value = l + s;
//	    return new HsvColor(h, saturationV, value, this.alpha);
	}
	
//	  public RgbColor getRgb() {
//		    double red = 0;
//		    double green = 0;
//		    double blue = 0;
//
//		    double h = ChromaUtil.clamp(hue, 0, 360);
//		    double s = ChromaUtil.clamp(saturationV, 0, 1);
//		    double v = ChromaUtil.clamp(value, 0, 1);
//		    double c = s * v;
//		    double sector = h / 60; // Sector of the color wheel.
//		    double x = c * (1 - Math.abs((sector % 2) - 1));
//
//		    if (sector < 1) {
//		      red = c;
//		      green = x;
//		    } else if (sector < 2) {
//		      red = x;
//		      green = c;
//		    } else if (sector < 3) {
//		      green = c;
//		      blue = x;
//		    } else if (sector < 4) {
//		      green = x;
//		      blue = c;
//		    } else if (sector < 5) {
//		      red = x;
//		      blue = c;
//		    } else if (sector <= 6) {
//		      red = c;
//		      blue = x;
//		    }
//
//		    double min = v - c;
//
//		    red += min;
//		    green += min;
//		    blue += min;
//
//		    red *= 255;
//		    green *= 255;
//		    blue *= 255;
//
//		    return new RgbColor(red, green, blue, alpha);
//		  }

}
