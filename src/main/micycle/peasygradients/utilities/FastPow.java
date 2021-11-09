package micycle.peasygradients.utilities;

import micycle.peasygradients.utilities.fastLog.DFastLog;
import micycle.peasygradients.utilities.fastLog.FastLog;
import net.jafama.FastMath;

/**
 * Java implementation of 'Fast pow() With Adjustable Accuracy' by Harrison
 * Ainsworth from
 * http://www.hxa7241.org/articles/content/fast-pow-adjustable_hxa7241_2007.html
 * <p>
 * When precision = 11 (8KB table), mean error is < 0.01%, and max error is <
 * 0.02% (proportional, ie: abs(true - approx) / true).
 * <p>
 * The essential approximation is a 'staircase' function across the fraction
 * range between successive integer powers. It has full float precision y
 * values, but at limited regular x intervals. Accuracy is proportional to
 * number of table elements.
 * <p>
 * This solution has a small weakness: for radix two it produces inexact results
 * for integer powers when it need not.
 * 
 * @author Michael Carleton
 *
 */
public final class FastPow {

	private static final float _2p23 = 8388608.0f;
	private static final float _2p23b = (127.0f * _2p23);
	private static final float ln2_INV = (float) (1 / Math.log(2));
	private static final float floatBaseE = getBaseRepresentation(Math.E);
	private static final float doubleBaseE = getBaseRepresentation(Math.E);

	private static FastLog fastLog;

	private static int[] table;
	private static int precision;

	static {
		// init statically incase used from other library
		init(13);
	}

	/**
	 * Initialize powFast lookup table. Must be called once before use.
	 * 
	 * fastLog speed factor drops off after 14 bits.
	 * 
	 * @param precision number of mantissa bits used, >= 0 and <= 18
	 */
	public static void init(int precision) {

		FastPow.precision = precision;

		table = new int[(int) Math.pow(2, precision)];

		float zeroToOne = 1.0f / ((1 << precision) * 2.0f);
		for (int i = 0; i < (1 << precision); ++i) {
			/* make y-axis value for table element */
			final float f = ((float) Math.pow(2.0f, zeroToOne) - 1.0f) * _2p23;
			table[i] = (int) (f < _2p23 ? f : (_2p23 - 1.0f));

			zeroToOne += 1.0f / (1 << precision);
		}

		fastLog = new DFastLog(precision);
	}

	/**
	 * Use {@link #getBaseRepresentation(float)}
	 * 
	 * @param baseRepresentation one over log, to required radix, of two. Use
	 *                           {@link #getBaseRepresentation(float)} to derive
	 *                           value.
	 * @param exponent           power to raise radix to
	 * @return
	 * @see #fastPow(double, double)
	 */
	public static float fastPowConstantBase(final float baseRepresentation, final float exponent) {
		final int i = (int) ((exponent * (_2p23 * baseRepresentation)) + (127.0f * _2p23));

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	public static float fastPowConstantBase(final double baseRepresentation, final double exponent) {
		final int i = (int) ((exponent * (_2p23 * baseRepresentation)) + (127.0f * _2p23));

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	public static float fastPow(final float base, final float exponent) {
		final int i = (int) ((exponent * (_2p23 * fastLog.fastLog2(base))) + _2p23b);

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	/**
	 * Includes further optimisation to calculate base representation.
	 * 
	 * @param baseRepresentation the exact base
	 * @param exponent           power to raise radix to
	 * @return
	 * @see #fastPow(float, float)
	 */
	public static float fastPow(final double base, final double exponent) {

		final int i = (int) (exponent * (_2p23 * fastLog.fastLog2(base)) + _2p23b);

		/* replace mantissa with lookup */
		final int it = (i & 0xFF800000) | table[(i & 0x7FFFFF) >> (23 - precision)];

		/* convert bits to float */
		return Float.intBitsToFloat(it); // Calls a JNI binding
	}

	/**
	 * @param exponent the exponent to raise e to
	 * @return the value e^a, where e is the base of the natural logarithms.
	 */
	public static float exp(final float exponent) {
		return fastPowConstantBase(floatBaseE, exponent);
	}

	/**
	 * @param exponent the exponent to raise e to
	 * @return the value e^a, where e is the base of the natural logarithms.
	 */
	public static float exp(final double exponent) {
		return fastPowConstantBase(doubleBaseE, exponent);
	}

	/**
	 * Calcuate representation of the given radix for use in
	 * {@link #fastPow(float, float)}.
	 * 
	 * @param radix
	 * @return
	 * @see #fastPow(float, float)
	 */
	public static float getBaseRepresentation(float radix) {
		return (float) FastMath.log(radix) * ln2_INV;
	}

	public static float getBaseRepresentation(double radix) {
		return (float) FastMath.log(radix) * ln2_INV;
	}

}
