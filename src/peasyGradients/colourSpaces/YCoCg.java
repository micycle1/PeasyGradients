package peasyGradients.colourSpaces;

/**
 * https://github.com/accord-net/java/blob/master/Catalano
 */
public class YCoCg {

	/**
	 * RGB -> YCoCg.
	 * 
	 * @param red   Red coefficient. Values in the range [0...1].
	 * @param green Green coefficient. Values in the range [0...1].
	 * @param blue  Blue coefficient. Values in the range [0...1].
	 * @return YCoCg color space.
	 */
	public static double[] rgb2YCoCg(double[] rgb) {

		double y = rgb[0] / 4f + rgb[1] / 2f + rgb[2] / 4f;
		double co = rgb[0] / 2f - rgb[2] / 2f;
		double cg = -rgb[0] / 4f + rgb[1] / 2f - rgb[2] / 4f;

		return new double[] { y, co, cg };
	}

	/**
	 * YCoCg -> RGB.
	 * 
	 * @param y  Pseudo luminance, or intensity.
	 * @param co Orange chrominance.
	 * @param cg Green chrominance.
	 * @return RGB color space. 0...1
	 */
	public static double[] YCoCg2rgb(double[] ycocg) {
		return new double[] { ycocg[0] + ycocg[1] - ycocg[2], ycocg[0] + ycocg[2], ycocg[0] - ycocg[1] - ycocg[2] };
	}

}
