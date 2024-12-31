package micycle.peasygradients.utilities;

/**
 * Provides static utility methods for color processing, including color
 * composition and decomposition, as well as linear interpolation between
 * colors.
 * <P>
 * "RGB1" is used in a number of conversion method names. It means the RGB
 * components' ranges are expected in [0...1].
 * 
 * @author Michael Carleton
 */
public final class ColorUtils {

	private ColorUtils() {
	}

	private static final int OPAQUE = 255 << 24; // fully opaque
	private static final float INV_255 = 1f / 255f; // used to normalise RGB values // TODO use double?

	/**
	 * Linearly interpolates between two colors represented as double arrays.
	 * 
	 * @param a    The first color as a double array [R,G,B].
	 * @param b    The second color as a double array [R,G,B].
	 * @param step The interpolation factor, where 0.0 is the start color and 1.0 is
	 *             the end color.
	 * @param out  The array to store the interpolated color [R,G,B].
	 * @return The interpolated color as a double array.
	 */
	public static double[] interpolateLinear(double[] a, double[] b, float step, double[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}

	/**
	 * Linearly interpolates between two colors with alpha values.
	 * 
	 * @param col1 The first color as a float array [R,G,B,A] with each component in
	 *             the range 0...1.
	 * @param col2 The second color as a float array [R,G,B,A] with each component
	 *             in the range 0...1.
	 * @param step The interpolation factor, where 0.0 is the start color and 1.0 is
	 *             the end color.
	 * @param out  The array to store the interpolated color [R,G,B,A].
	 * @return The interpolated color as a float array.
	 */
	public static float[] interpolateLinear(float[] col1, float[] col2, float step, float[] out) {
		out[0] = col1[0] + step * (col2[0] - col1[0]);
		out[1] = col1[1] + step * (col2[1] - col1[1]);
		out[2] = col1[2] + step * (col2[2] - col1[2]);
		out[3] = col1[3] + step * (col2[3] - col1[3]);
		return out;
	}

	/**
	 * Composes a 32-bit ARGB color from RGB components represented as a float
	 * array.
	 * 
	 * @param RGB The RGB components as a float array [R,G,B] with each value in
	 *             the range 0...1.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGB1ToRGBA255(float[] RGB) { // floatRGBToRGBInt
		return OPAQUE | (int) (RGB[0] * 255) << 16 | (int) (RGB[1] * 255) << 8 | (int) (RGB[2] * 255);
	}

	/**
	 * Composes a 32-bit ARGB color from RGBA components represented as a float
	 * array.
	 * 
	 * @param RGBA The RGB components as a float array [R,G,B] with each value in
	 *             the range 0...1.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGBA1ToRGBA255(float[] RGBA) { // floatRGBToRGBInt
		return (int) (RGBA[3] * 255) << 24 | (int) (RGBA[0] * 255) << 16 | (int) (RGBA[1] * 255) << 8 | (int) (RGBA[2] * 255);
	}

	/**
	 * Composes a 32-bit ARGB color from RGBA components represented as a float
	 * array.
	 * 
	 * @param RGBA The RGBA components as a float array [R,G,B,A] with each value in
	 *             the range 0...255.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGBA255ToRGBA255(float[] RGBA) {
		return (int) (RGBA[3]) << 24 | (int) (RGBA[0]) << 16 | (int) (RGBA[1]) << 8 | (int) (RGBA[2]);
	}

	/**
	 * Composes a 32-bit ARGB color from individual RGBA components.
	 * 
	 * @param red   The red component in the range 0...1.
	 * @param green The green component in the range 0...1.
	 * @param blue  The blue component in the range 0...1.
	 * @param alpha The alpha component in the range 0...1.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGBA1ToRGBA255(float red, float green, float blue, float alpha) {
		return (int) (alpha * 255) << 24 | (int) (red * 255) << 16 | (int) (green * 255) << 8 | (int) (blue * 255);
	}

	/**
	 * Composes a 32-bit ARGB color from individual RGB components, defaulting to
	 * full opacity.
	 * 
	 * @param red   The red component as a float.
	 * @param green The green component as a float.
	 * @param blue  The blue component as a float.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGB255ToRGB255(float red, float green, float blue) {
		return 255 << 24 | (int) red << 16 | (int) green << 8 | (int) blue;
	}

	/**
	 * Composes a 32-bit ARGB color from RGB components represented as a double
	 * array. Assumes full opacity.
	 * 
	 * @param in The RGB components as a double array [R,G,B] with each value in the
	 *           range 0...1.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGB1ToRGB255(double[] in) {
		return OPAQUE | (int) (in[0] * 255 + 0.5) << 16 | (int) (in[1] * 255 + 0.5) << 8 | (int) (in[2] * 255 + 0.5);
	}

	/**
	 * Composes a 32-bit ARGB color from RGB components represented as a double
	 * array and an alpha component as an integer.
	 * 
	 * @param in    The RGB components as a double array [R,G,B] with each value in
	 *              the range 0...1.
	 * @param alpha The alpha component as an integer in the range 0...255.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGB1ToRGBA255(double[] in, int alpha) {
		return alpha << 24 | (int) (in[0] * 255 + 0.5) << 16 | (int) (in[1] * 255 + 0.5) << 8 | (int) (in[2] * 255 + 0.5);
	}

	/**
	 * Composes a 32-bit ARGB color from RGB components represented as a double
	 * array, clamping each channel output between 0 and 255.
	 * 
	 * @param in    The RGB components [0...1] as a double array [R,G,B] with
	 *              possible over/underflow.
	 * @param alpha The alpha component as an integer in the range 0...255.
	 * @return The composed ARGB color as an integer.
	 */
	public static int RGB1ToRGBA255Clamp(double[] in, int alpha) {
		// https://stackoverflow.com/a/70420549/9808792
		int r = (int) (in[0] * 255.0 + 0.5);
		r = (r & ~(r >> 31) | 255 - r >> 31) & 255;
		int g = (int) (in[1] * 255.0 + 0.5);
		g = (g & ~(g >> 31) | 255 - g >> 31) & 255;
		int b = (int) (in[2] * 255.0 + 0.5);
		b = (b & ~(b >> 31) | 255 - b >> 31) & 255;
		return alpha << 24 | r << 16 | g << 8 | b;
	}

	private static int composeclrClampSimple(double[] in, int alpha) {
		int r = (int) Math.min(Math.max(in[0] * 255, 0), 255);
		int g = (int) Math.min(Math.max(in[1] * 255, 0), 255);
		int b = (int) Math.min(Math.max(in[2] * 255, 0), 255);
		return alpha << 24 | r << 16 | g << 8 | b;
	}

	/**
	 * Converts RGB components from a double array to an integer array with values
	 * scaled to range 0...255.
	 * 
	 * @param in The RGB components as a double array [R,G,B] with each value in the
	 *           range 0...1.
	 * @return The RGB components as an integer array [R,G,B] scaled to range
	 *         0...255.
	 */
	public static int[] RGB1ToInt255(double[] in) {
		return new int[] { (int) Math.round(in[0] * 255), (int) Math.round(in[1] * 255), (int) Math.round(in[2] * 255) };
	}

	/**
	 * Decomposes a 32-bit ARGB color into its components and scales them to range
	 * 0...1.
	 * 
	 * @param clr The ARGB color as an integer.
	 * @return The color components as a float array [R,G,B,A] scaled to range
	 *         0...1.
	 */
	public static float[] decomposeclr(int clr) {
		// 1.0 / 255.0 = 0.003921569
		return new float[] { (clr >> 16 & 0xff) * INV_255, (clr >> 8 & 0xff) * INV_255, (clr & 0xff) * INV_255, (clr >> 24 & 0xff) * INV_255 };
	}

	/**
	 * Decomposes a 32-bit ARGB color into its components with values in the range
	 * 0...255.
	 * 
	 * @param clr The ARGB color as an integer.
	 * @return The color components as a float array [R,G,B,A] with values in the
	 *         range 0...255.
	 */
	public static float[] decomposeclrRGB(int clr) {
		float[] out = new float[4];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		out[3] = (clr >> 24 & 0xff);
		return out;
	}

	/**
	 * Decomposes a 32-bit RGB color into its components, returning them as a double
	 * array.
	 * 
	 * @param clr The RGB color as an integer.
	 * @return The color components as a double array [R,G,B] with values in the
	 *         range 0...255.
	 */
	public static double[] decomposeclrRGBDouble(int clr) {
		double[] out = new double[3];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		return out;
	}

	/**
	 * Decomposes a 32-bit RGB color into its components, including an alpha value
	 * as an additional parameter.
	 * 
	 * @param clr   The RGB color as an integer.
	 * @param alpha The alpha component as an integer in the range 0...255.
	 * @return The color components as a double array [R,G,B,A] with RGB values in
	 *         the range 0...255 and the given alpha.
	 */
	public static double[] decomposeclrRGBDouble(int clr, int alpha) {
		double[] out = new double[4];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		out[3] = alpha;
		return out;
	}

	/**
	 * Decomposes a 32-bit ARGB color into its components and normalizes them to
	 * range 0...1.
	 * 
	 * @param clr The ARGB color as an integer.
	 * @return The color components as a float array [R,G,B,A] normalized to range
	 *         0...1.
	 */
	public static float[] decomposeclrRGBA(int clr) {
		float[] out = new float[4];
		out[3] = (clr >> 24 & 0xff) * INV_255;
		out[0] = (clr >> 16 & 0xff) * INV_255;
		out[1] = (clr >> 8 & 0xff) * INV_255;
		out[2] = (clr & 0xff) * INV_255;
		return out;
	}

	/**
	 * Decomposes a 32-bit RGB color into its components and normalizes them to
	 * range 0...1.
	 * 
	 * @param clr The RGB color as an integer.
	 * @return The color components as a double array [R,G,B] normalized to range
	 *         0...1.
	 */
	public static double[] RGB255ToRGB1(int clr) {
		double[] out = new double[3];
		out[0] = (clr >> 16 & 0xff) * INV_255;
		out[1] = (clr >> 8 & 0xff) * INV_255;
		out[2] = (clr & 0xff) * INV_255;
		return out;
	}

}
