package peasyGradients.utilities;

/**
 * Contains static functions mostly related to color processing (mostly
 * (de)composition).
 * 
 * @author micycle1
 *
 */
public final class ColorUtils {

	private static final int fullAlpha = 255 << 24; // fully opaque
	private static final float INV_255 = 1f / 255f; // used to normalise RGB values

	/**
	 * Linearly interpolate between 2 colors (color-space independent) using the
	 * given step.
	 * 
	 * @param a    double[3] (col 1)
	 * @param b    double[3] (col 2)
	 * @param step
	 * @param out  double[]
	 * @return
	 */
	public static double[] interpolateLinear(double[] a, double[] b, float step, double[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}

	/**
	 * Returns a color by interpolating between two given colors. An alternative to
	 * Processing's native lerpcolor() method (which is linear).
	 * 
	 * @param col1 First color, represented as [R,G,B,A] array; each value between
	 *             0...1.
	 * @param col2 Second color, represented as [R,G,B,A] array; each value between
	 *             0...1.
	 * @param st   step: percentage between the two colors.
	 * @param out  The new interpolated color, represented by a [R,G,B,A] array.
	 * @return
	 */
	public static float[] interpolateLinear(float[] col1, float[] col2, float step, float[] out) {
		out[0] = col1[0] + step * (col2[0] - col1[0]);
		out[1] = col1[1] + step * (col2[1] - col1[1]);
		out[2] = col1[2] + step * (col2[2] - col1[2]);
		out[3] = col1[3] + step * (col2[3] - col1[3]);
		return out;
	}

	/**
	 * Compose a 32 bit sARGB int from float[] 0...1
	 * 
	 * @param in
	 * @return
	 */
	public static int composeclr(float[] RGBA) {
		return (int) (RGBA[3] * 255) << 24 | (int) (RGBA[0] * 255) << 16 | (int) (RGBA[1] * 255) << 8 | (int) (RGBA[2] * 255);
	}

	/**
	 * Compose a 32 bit sARGB int from float[] 0...255
	 * 
	 * @param in
	 * @return
	 */
	public static int composeclr255(float[] RGBA) {
		return (int) (RGBA[3]) << 24 | (int) (RGBA[0]) << 16 | (int) (RGBA[1]) << 8 | (int) (RGBA[2]);
	}

	/**
	 * Compose an RGBA color using a float[] of values in range 0...1
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 * @return integer representation of RGBA
	 */
	public static int composeclr(float red, float green, float blue, float alpha) {
		return (int) (alpha * 255) << 24 | (int) (red * 255) << 16 | (int) (green * 255) << 8 | (int) (red * 255);
	}

	public static int composeclr(float red, float green, float blue) {
		return 255 << 24 | (int) red << 16 | (int) green << 8 | (int) blue;
	}

	/**
	 * sRGB (0...1) in. Assumes full alpha.
	 * 
	 * @param in
	 * @return
	 */
	public static int composeclr(double[] in) {
		return fullAlpha | (int) (in[0] * 255 + 0.5) << 16 | (int) (in[1] * 255 + 0.5) << 8 | (int) (in[2] * 255 + 0.5);
	}

	/**
	 * 
	 * @param in    double[3] each is 0...1
	 * @param alpha 0...255
	 * @return
	 */
	public static int composeclr(double[] in, int alpha) {
		return alpha << 24 | (int) (in[0] * 255 + 0.5) << 16 | (int) (in[1] * 255 + 0.5) << 8 | (int) (in[2] * 255 + 0.5);
	}

	public static int[] composeclrTo255(double[] in) {
		return new int[] { (int) Math.round(in[0] * 255), (int) Math.round(in[1] * 255), (int) Math.round(in[2] * 255) };
	}

	/**
	 * Decompose color integer (ARGB) into 4 separate components and scale between
	 * 0...1
	 * 
	 * @param clr
	 * @param out
	 * @return [R,G,B,A] 0...1
	 */
	public static float[] decomposeclr(int clr) {
		// 1.0 / 255.0 = 0.003921569
		return new float[] { (clr >> 16 & 0xff) * INV_255, (clr >> 8 & 0xff) * INV_255, (clr & 0xff) * INV_255,
				(clr >> 24 & 0xff) * INV_255 };
	}

	/**
	 * Decompose color integer (RGBA) into 4 separate components (0...255)
	 * 
	 * @param clr
	 * @param out
	 * @return [R,G,B] 0...255
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
	 * out 255
	 * 
	 * @param clr
	 * @return
	 */
	public static double[] decomposeclrRGBDouble(int clr) {
		double[] out = new double[3];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		return out;
	}

	public static double[] decomposeclrRGBDouble(int clr, int alpha) {
		double[] out = new double[4];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		out[3] = alpha;
		return out;
	}

	/**
	 * Unpack a 32-Bit ARGB int, normalising to 0..1
	 * 
	 * @param clr
	 * @return sRGB representing the argb color
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
	 * out 0...1
	 * 
	 * @param clr
	 * @return
	 */
	public static double[] decomposeclrDouble(int clr) {
		double[] out = new double[3];
		out[0] = (clr >> 16 & 0xff) * INV_255;
		out[1] = (clr >> 8 & 0xff) * INV_255;
		out[2] = (clr & 0xff) * INV_255;
		return out;
	}

}
