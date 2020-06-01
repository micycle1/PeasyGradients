package peasyGradients.colourSpaces;

/**
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
	
	public static void hsl2rgb() {
		
	}

}
