package micycle.peasygradients.colorspace;

/**
 * Defines the interface for color space transformations, enabling conversion to
 * and from the RGB color space. Implementations should particularly focus on
 * optimizing the {@link #toRGB(double[])} method for efficient conversion.
 * 
 * @author Michael Carleton
 */
public interface ColorSpaceTransform {

	/**
	 * Converts a color from the implementing color space to RGB. Implementations
	 * should focus on optimizing this conversion process for efficiency.
	 * 
	 * @param color an array representing the color in the implementing color space.
	 *              The specific meaning and length of this array depend on the
	 *              color space.
	 * @return an array of three doubles representing the color in RGB, with each
	 *         component normalized to the range [0, 1]. The output may exceed this
	 *         range before display calibration or clipping is applied.
	 */
	public double[] toRGB(double[] color);

	/**
	 * Converts an RGB color into the corresponding color in the target color space.
	 * 
	 * @param RGB an array of three doubles representing the red, green, and blue
	 *            components of the color, each normalized to the range [0, 1]
	 * @return an array of doubles representing the color in the target color space.
	 *         The length and interpretation of this array depend on the specific
	 *         color space.
	 */
	public double[] fromRGB(double[] RGB);

	/**
	 * Performs linear interpolation between two colors in an
	 * implementation-independent manner. This method is designed to work across
	 * different color spaces, providing a generic approach to color blending.
	 * Specific color space implementations (like HSB) may override this method if a
	 * appropriate (non-linear) interpolation technique is required to mix colors
	 * within the space.
	 * 
	 * @param a    an array of doubles representing the starting color in the color
	 *             space
	 * @param b    an array of doubles representing the ending color in the color
	 *             space
	 * @param step a double value between 0.0 and 1.0 representing the interpolation
	 *             factor, where 0.0 corresponds to the starting color, 1.0 to the
	 *             ending color, and values in between to intermediate colors
	 * @param out  an array of doubles where the interpolated color will be stored.
	 *             This array must be pre-allocated and of appropriate length for
	 *             the color space
	 * @return the {@code out} array containing the interpolated color
	 */
	public default double[] interpolateLinear(double[] a, double[] b, double step) {
		double[] out = new double[3];
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}
}
