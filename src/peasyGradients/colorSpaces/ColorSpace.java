package peasyGradients.colorSpaces;

/**
 * Colorspaces should implement this class.
 * 
 * @author micycle1
 *
 */
public interface ColorSpace {

	/**
	 * 
	 * @param color the 3 channel color as represented in the implementing color
	 *              space
	 * @return RGB normalised to [0, 1]
	 */
	public double[] toRGB(double[] color); // convert from color space to RGB

	/**
	 * 
	 * @param RGB RGB normalised to [0, 1]
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
