package micycle.peasygradients.colorspace;

/**
 * Interface for defining a color space that provides conversion to and from
 * RGB.
 * <p>
 * It is more important the {@link #toRGB(double[])} method is optimised.
 * 
 * @author Michael Carleton
 *
 */
public interface ColorSpace {

	/**
	 * Converts a color space representation of the color into RGB.
	 * 
	 * @param color the 3 channel color as represented in the implementing color
	 *              space
	 * @return RGB normalised to [0, 1]. Implementations of this class do not
	 *         necessarily need to clamp the output between 0 and 1.
	 */
	public double[] toRGB(double[] color); // convert from color space to RGB

	/**
	 * Converts an RGB color into the target color space.
	 * 
	 * @param RGB [R,G,B] where each component is normalised to [0, 1]
	 * @return
	 */
	public double[] fromRGB(double[] RGB); // convert from RGB to color space

	/**
	 * Default linear interpolation method -- colorspace independent. Most color
	 * spaces use this; a few (like HSB) may need to override.
	 * 
	 * @param a    colorA
	 * @param b    colorB
	 * @param step
	 * @param out  interpolated color
	 * @return
	 */
	public default double[] interpolateLinear(double[] a, double[] b, double step, double[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}
}
