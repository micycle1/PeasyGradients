package peasyGradients.utilities;

import net.jafama.FastMath;

/**
 * Implementation of Fast pow() With Adjustable Accuracy at
 * http://www.hxa7241.org/articles/content/fast-pow-adjustable_hxa7241_2007.html
 * 
 * The essential approximation is a ‘staircase’ function across the fraction
 * range between successive integer powers. It has full float precision y
 * values, but at limited regular x intervals. Accuracy is proportional to
 * number of table elements.
 * 
 * This solution has a small weakness: for radix two it produces inexact results
 * for integer powers when it need not.
 * 
 * @author micycle1
 *
 */
public final class FastPow {

	private static final float _2p23 = 8388608.0f;
	private static final float log2 = (float) Math.log(2);

	private static int[] table;
	private static int precision;

	/**
	 * Initialize powFast lookup table.Initialize powFast lookup table.
	 * 
	 * @param precision number of mantissa bits used, >= 0 and <= 18
	 */
	public static void init(int precision) {

		table = new int[(int) Math.pow(2, precision)];
		FastPow.precision = precision;

		float zeroToOne = 1.0f / ((float) (1 << precision) * 2.0f);
		int i;
		for (i = 0; i < (1 << precision); ++i) {
			/* make y-axis value for table element */
			final float f = ((float) Math.pow(2.0f, zeroToOne) - 1.0f) * _2p23;
			table[i] = (int) (f < _2p23 ? f : (_2p23 - 1.0f));

			zeroToOne += 1.0f / (float) (1 << precision);
		}
	}

	/**
	 * Use {@link #getbaseRepresentation(float)}
	 * @param baseRepresentation one over log, to required radix, of two
	 * @param exponent           power to raise radix to
	 * @return
	 */
	public static float fastPow(final float baseRepresentation, final float exponent) {
		final int i = (int) ((exponent * (_2p23 * baseRepresentation)) + (127.0f * _2p23));

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}
	
	public static float getbaseRepresentation(float base) {
		return (float) Math.log(base) / log2;
	}

	/**
	 * Includes further optimisation to calculate base representation.
	 * @param baseRepresentation the exact base
	 * @param exponent           power to raise radix to
	 * @return
	 */
	public static float fastPow(final double base, final double exponent) {

		final int i = (int) ((exponent * (_2p23 * (log(base) / log2))) + (127.0f * _2p23));

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	private static double log(double x) {
		return 6 * (x - 1) / (x + 1 + 4 * (FastMath.sqrt(x)));
	}

}
