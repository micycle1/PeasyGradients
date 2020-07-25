package peasyGradients.colourSpaces;

/**
 * Colourspaces should implement this class.
 * 
 * @author micycle1
 *
 */
public interface ColourSpace {

	public double[] toRGB(double[] colour); // convert from colour space to RGB

	public double[] fromRGB(double[] RGB); // convert from RGB to colour space
	
	/**
	 * Default linear interpolation method. Most classes use this; a few (like HSB)
	 * may need to override.
	 * 
	 * @param a
	 * @param b
	 * @param step
	 * @param out
	 * @return
	 */
	public default double[] interpolateLinear(double[] a, double[] b, float step, double[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}
}
