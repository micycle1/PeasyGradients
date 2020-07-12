package peasyGradients.utilities.fastLog;

/**
 * Implementation of the ICSILog algorithm as described in O. Vinyals, G.
 * Friedland, N. Mirghafori "Revisiting a basic function on current CPUs: A fast
 * logarithm implementation with adjustable accuracy" (2007).
 *
 * <p>
 * This class is based on the original algorithm description.
 *
 * <p>
 * The algorithm has been changed to detects when the unbiased exponent is zero
 * and maintains the precision.
 *
 * <p>
 * Using look-up table the relative error
 * {@code ((fastLog(x)-Math.log(x))/Math.log(x))} is large ({@code e>>1}) when
 * the input value {@code x} is close to 1. So the algorithm detects values
 * close to 1 and uses {@code Math.log(double)} instead.
 *
 * @see <a href=
 *      "http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf">http://www.icsi.berkeley.edu/pubs/techreports/TR-
 *      07-002.pdf</a>
 */
public class TurboLog extends FastLog {
	/**
	 * The table of the log value of the unbiased float exponent (an integer from
	 * -127 to 128).
	 */
	private static final float[] logExpF;
	/**
	 * The table of the log value of the unbiased double exponent (an integer from
	 * -1023 to 1024).
	 */
	private static final float[] logExpD;

	/**
	 * The bounds below 1 where the function switches to use Math.log. This results
	 * in a maximum relative error of 7.95e-4 for x below 1.
	 */
	public static final double LOWER_ONE_BOUND = 0.92;
	/**
	 * The bounds above 1 where the function switches to use Math.log. This results
	 * in a maximum relative error of 7.09e-4 for x above 1.
	 */
	public static final double UPPER_ONE_BOUND = 1.16;

	/**
	 * The lower bound mantissa (for a double) below 1 where the function switches
	 * to use Math.log
	 */
	protected static final long LOWER_BOUND_MANTISSA;
	/**
	 * The upper bound mantissa (for a double) above 1 where the function switches
	 * to use Math.log
	 */
	protected static final long UPPER_BOUND_MANTISSA;
	/**
	 * The lower bound mantissa (for a float) below 1 where the function switches to
	 * use Math.log
	 */
	protected static final int LOWER_BOUND_MANTISSA_F;
	/**
	 * The upper bound mantissa (for a float) above 1 where the function switches to
	 * use Math.log
	 */
	protected static final int UPPER_BOUND_MANTISSA_F;

	static {
		// Note: the exponent is already in base 2. Just multiply by ln(2) to convert to
		// base E
		logExpF = new float[256]; // 8-bit exponent
		for (int i = 0; i < logExpF.length; i++) {
			logExpF[i] = (float) ((i - 127) * LN2);
		}
		logExpD = new float[2048]; // 11-bit exponent
		for (int i = 0; i < logExpD.length; i++) {
			logExpD[i] = (float) ((i - 1023) * LN2);
		}

		// Get the mantissa bounds
		assert ((Double.doubleToRawLongBits(LOWER_ONE_BOUND) >> 52) - 1023) == -1 : "lower bound exponent not -1";
		assert ((Double.doubleToRawLongBits(UPPER_ONE_BOUND) >> 52) - 1023) == 0 : "upper bound exponent not 0";
		LOWER_BOUND_MANTISSA = Double.doubleToRawLongBits(LOWER_ONE_BOUND) & 0xfffffffffffffL;
		UPPER_BOUND_MANTISSA = Double.doubleToRawLongBits(UPPER_ONE_BOUND) & 0xfffffffffffffL;

		assert ((Float.floatToIntBits((float) LOWER_ONE_BOUND) >> 23) - 127) == -1 : "lower bound exponent not -1";
		assert ((Float.floatToIntBits((float) UPPER_ONE_BOUND) >> 23) - 127) == 0 : "upper bound exponent not 0";
		LOWER_BOUND_MANTISSA_F = Float.floatToIntBits((float) LOWER_ONE_BOUND) & 0x7fffff;
		UPPER_BOUND_MANTISSA_F = Float.floatToIntBits((float) UPPER_ONE_BOUND) & 0x7fffff;
	}

	/** The number of bits to remove from a float mantissa. */
	// CHECKSTYLE.OFF: MemberName
	private final int q;
	// CHECKSTYLE.ON: MemberName
	/** The number of bits to remove from a double mantissa. */
	private final int qd;
	/**
	 * The table of the log value of the floating point mantissa (a binary number
	 * 1.0000... to 1.1111....), depending on the precision.
	 */
	private final float[] logMantissa;

	/**
	 * Gets the log value of the unbiased float exponent (an integer from -127 to
	 * 128) using the biased exponent.
	 *
	 * <pre>
	 * {@code (float) ((exponent - 127) * Math.log(2.0))}
	 * </pre>
	 *
	 * @param exponent the biased exponent
	 * @return the log value
	 */
	static final float getLogExpF(int exponent) {
		return logExpF[exponent];
	}

	/**
	 * Gets the log value of the unbiased float exponent (an integer from -1023 to
	 * 1024) using the biased exponent.
	 *
	 * <pre>
	 * {@code (float) ((exponent - 1023) * Math.log(2.0))}
	 * </pre>
	 *
	 * @param exponent the biased exponent
	 * @return the log value
	 */
	static final float getLogExpD(int exponent) {
		return logExpD[exponent];
	}

	/**
	 * Create a new natural logarithm calculation instance. This will hold the
	 * pre-calculated log values for base E and the default table size.
	 */
	public TurboLog() {
		this(N);
	}

	/**
	 * Create a new natural logarithm calculation instance. This will hold the
	 * pre-calculated log values for base E and a table size depending on a given
	 * mantissa precision.
	 *
	 * @param n The number of bits to keep from the mantissa. Table storage = 2^n *
	 *          4 bytes, e.g. 32Kb for n=13.
	 */
	public TurboLog(int n) {
		// Store log value of a range of floating point numbers using a limited
		// precision mantissa (m). The purpose of this code is to enumerate all
		// possible mantissas of a float with limited precision (23-q). Note the
		// mantissa represents the digits of a binary number after the binary-point:
		// .10101010101.
		// It is assumed that the digit before the point is a 1 if the exponent
		// is non-zero. Otherwise the binary point is moved to the right of the first
		// digit (i.e. a bit shift left).

		// See Float.intBitsToFloat(int):
		// int s = ((bits >> 31) == 0) ? 1 : -1;
		// int e = ((bits >> 23) & 0xff); // Unsigned exponent
		// int m = (e == 0) ?
		// (bits & 0x7fffff) << 1 :
		// (bits & 0x7fffff) | 0x800000;
		//
		// Then the floating-point result equals the value of the mathematical
		// expression s x m x 2^(e-150):
		// e-127 is the unbiased exponent. 23 is the mantissa precision
		// = s x m x 2^(e-127-23)

		// E.g. For a precision of n=(23-q)=6
		// We enumerate:
		// (1.000000 to 1.111111)

		// The mantissa is incremented using an integer representation to allow
		// exact enumeration. This is then converted to a float for the call to
		// log(double).

		q = 23 - n;
		qd = 52 - n;
		int x = 0x3F800000; // Set the exponent to 0 so the float value=1.0
		// assert Float.intBitsToFloat(x) == 1.0f : "value is not 1.0f";
		final int inc = 1 << q; // Amount to increase the mantissa

		final int size = 1 << n;
		logMantissa = new float[size];
		for (int i = 0; i < size; i++) {
			final float value = Float.intBitsToFloat(x);
			final float logv = (float) Math.log(value);
			logMantissa[i] = logv;
			x += inc;

			// assert logv == fastLog(value) : String.format("[%d] data[i](%g) %g != %g %g",
			// i, value,
			// logv,
			// fastLog2(value),
			// uk.ac.sussex.gdsc.core.utils.FloatEquality.relativeError(logv,
			// fastLog2(value)));
		}
	}

	@Override
	public int getN() {
		return 23 - q;
	}

	@Override
	public double getScale() {
		return LN2;
	}

	@Override
	public double getBase() {
		return Math.E;
	}

	@Override
	public float log(float x) {
		final int bits = Float.floatToRawIntBits(x);
		final int e = (bits >>> 23) & 0xff;
		final int m = (bits & 0x7fffff);

		// Edge case for NaN and +/- Infinity
		if (e == 255) {
			if (m != 0) {
				return Float.NaN;
			}
			return ((bits & 0x80000000) != 0) ? Float.NaN : Float.POSITIVE_INFINITY;
		}

		// Edge case for negatives
		if ((bits & 0x80000000) != 0) {
			// Only allow -0
			return (e == 0 && m == 0) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}

		// Note the documentation from Float.intBitsToFloat(int):
		// int s = ((bits >> 31) == 0) ? 1 : -1;
		// int e = ((bits >> 23) & 0xff); // Unsigned exponent
		// int m = (e == 0) ?
		// (bits & 0x7fffff) << 1 :
		// (bits & 0x7fffff) | 0x800000;
		//
		// Then the floating-point result equals the value of the mathematical
		// expression s x m x 2^(e-150):
		// e-127 is the unbiased exponent. 23 is the mantissa precision
		// = s x m x 2^(e-127-23)
		//
		// Here we have m as an index to the log of the mantissa including
		// the binary point. So we just need to compute
		// log(m x 2^(e-127))
		// = log(m) + log(2^(e-127))
		// = log(m) + (e-127) * log(2)

		// Check the exponent
		if (e == 0) {
			return (m == 0) ? Float.NEGATIVE_INFINITY : computeSubnormal(m << 1);
		}

		// When the value is close to 1 then the relative error can be very large
		if ((e == 126 && m >= LOWER_BOUND_MANTISSA_F) || (e == 127 && m <= UPPER_BOUND_MANTISSA_F)) {
			return (float) Math.log(x);
		}

		return logMantissa[m >>> q] + logExpF[e];
	}

	@Override
	public float log(double x) {
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);

		// Edge case for NaN and +/- Infinity
		if (e == 2047) {
			if (m != 0L) {
				return Float.NaN;
			}
			return ((bits & 0x8000000000000000L) != 0L) ? Float.NaN : Float.POSITIVE_INFINITY;
		}

		// Edge case for negatives
		if ((bits & 0x8000000000000000L) != 0L) {
			// Only allow -0
			return (e == 0 && m == 0L) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}

		// Note the documentation from Double.longBitsToDouble(int):
		// int s = ((bits >> 63) == 0) ? 1 : -1;
		// int e = (int)((bits >>> 52) & 0x7ffL);
		// long m = (e == 0) ?
		// (bits & 0xfffffffffffffL) << 1 :
		// (bits & 0xfffffffffffffL) | 0x10000000000000L;
		// Then the floating-point result equals the value of the mathematical
		// expression s x m x 2^(e-1075):
		// e-1023 is the unbiased exponent. 52 is the mantissa precision
		// = s x m x 2^(e-1023-52)
		//
		// Here we have m as an index to the log of the mantissa including
		// the binary point. So we just need to compute
		// log(m x 2^(e-1023))
		// = log(m) + log(2^(e-1023))
		// = log(m) + (e-1023) * log(2)

		// Check the exponent
		if (e == 0) {
			return (m == 0L) ? Float.NEGATIVE_INFINITY : computeSubnormalF(m << 1);
		}

		// When the value is close to 1 then the relative error can be very large
		if ((e == 1022 && m >= LOWER_BOUND_MANTISSA) || (e == 1023 && m <= UPPER_BOUND_MANTISSA)) {
			return (float) Math.log(x);
		}

		return logMantissa[(int) (m >>> qd)] + logExpD[e];
	}

	/**
	 * Compute the log for a subnormal float-point number, i.e. where the exponent
	 * is 0 then there is no assumed leading 1.
	 *
	 * <p>
	 * Note that if the mantissa is zero this will fail!
	 *
	 * <p>
	 * No rounding to be done on sub-normal as the mantissa is shifted {@code << 1}
	 * so the least significant digit is always 0.
	 *
	 * @param mantissa the mantissa (already bit shifted by 1)
	 * @return the log(x)
	 */
	protected float computeSubnormal(int mantissa) {
		// Normalize the subnormal number.
		// The unbiased exponent starts at -127.
		// Shift the mantissa until it is a binary number
		// with a leading 1: 1.10101010...

		int exp = -127;
		while ((mantissa & 0x800000) == 0) {
			--exp;
			mantissa <<= 1;
		}

		// Remove the leading 1
		return logMantissa[(mantissa & 0x7fffff) >>> q] + exp * LN2F;
	}

	/**
	 * Compute the log for a subnormal float-point number, i.e. where the exponent
	 * is 0 then there is no assumed leading 1.
	 *
	 * <p>
	 * Note that if the mantissa is zero this will fail!
	 *
	 * <p>
	 * No rounding to be done on sub-normal as the mantissa is shifted {@code << 1}
	 * so the least significant digit is always 0.
	 *
	 * @param mantissa the mantissa (already bit shifted by 1)
	 * @return the log(x)
	 */
	protected double computeSubnormal(long mantissa) {
		// Normalize the subnormal number.
		// The unbiased exponent starts at -1023.
		// Shift the mantissa until it is a binary number
		// with a leading 1: 1.10101010...

		int exp = -1023;
		while ((mantissa & 0x0010000000000000L) == 0) {
			--exp;
			mantissa <<= 1;
		}

		// Remove the leading 1
		return logMantissa[(int) ((mantissa & 0xfffffffffffffL) >>> qd)] + exp * LN2;
	}

	/**
	 * Compute the log for a subnormal float-point number, i.e. where the exponent
	 * is 0 then there is no assumed leading 1.
	 *
	 * <p>
	 * Note that if the mantissa is zero this will fail!
	 *
	 * <p>
	 * No rounding to be done on sub-normal as the mantissa is shifted {@code << 1}
	 * so the least significant digit is always 0.
	 *
	 * @param mantissa the mantissa (already bit shifted by 1)
	 * @return the log(x)
	 */
	protected float computeSubnormalF(long mantissa) {
		// Normalize the subnormal number.
		// The unbiased exponent starts at -1023.
		// Shift the mantissa until it is a binary number
		// with a leading 1: 1.10101010...

		int exp = -1023;
		while ((mantissa & 0x0010000000000000L) == 0) {
			--exp;
			mantissa <<= 1;
		}

		// Remove the leading 1
		return logMantissa[(int) ((mantissa & 0xfffffffffffffL) >>> qd)] + exp * LN2F;
	}

	/**
	 * Calculate the natural logarithm. Requires the argument be finite and
	 * positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog(Float.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code >fastLog(Float.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log(x)
	 */
	@Override
	public float fastLog(float x) {
		// As above but no checks for NaN or infinity
		final int bits = Float.floatToRawIntBits(x);
		final int e = ((bits >>> 23) & 0xff);
		final int m = (bits & 0x7fffff);
		if (e == 0) {
			return (m == 0) ? Float.NEGATIVE_INFINITY : computeSubnormal(m << 1);
		}
		if ((e == 126 && m >= LOWER_BOUND_MANTISSA_F) || (e == 127 && m <= UPPER_BOUND_MANTISSA_F)) {
			return (float) Math.log(x);
		}
		return logMantissa[m >>> q] + logExpF[e];
	}

	/**
	 * Calculate the natural logarithm. Requires the argument be finite and
	 * positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog(Double.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code >fastLog(Double.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log(x)
	 */
	@Override
	public float fastLog(double x) {
		// As above but no checks for NaN or infinity
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);
		if (e == 0) {
			return (m == 0L) ? Float.NEGATIVE_INFINITY : computeSubnormalF(m << 1);
		}
		if ((e == 1022 && m >= LOWER_BOUND_MANTISSA) || (e == 1023 && m <= UPPER_BOUND_MANTISSA)) {
			return (float) Math.log(x);
		}
		return logMantissa[(int) (m >>> qd)] + logExpD[e];
	}

	@Override
	public double logD(double x) {
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);

		// Edge case for NaN and +/- Infinity
		if (e == 2047) {
			if (m != 0L) {
				return Double.NaN;
			}
			return ((bits & 0x8000000000000000L) != 0L) ? Double.NaN : Double.POSITIVE_INFINITY;
		}

		// Edge case for negatives
		if ((bits & 0x8000000000000000L) != 0L) {
			// Only allow -0
			return (e == 0 && m == 0L) ? Double.NEGATIVE_INFINITY : Double.NaN;
		}

		// Note the documentation from Double.longBitsToDouble(int):
		// int s = ((bits >> 63) == 0) ? 1 : -1;
		// int e = (int)((bits >>> 52) & 0x7ffL);
		// long m = (e == 0) ?
		// (bits & 0xfffffffffffffL) << 1 :
		// (bits & 0xfffffffffffffL) | 0x10000000000000L;
		// Then the floating-point result equals the value of the mathematical
		// expression s x m x 2^(e-1075):
		// e-1023 is the unbiased exponent. 52 is the mantissa precision
		// = s x m x 2^(e-1023-52)
		//
		// Here we have m as an index to the log of the mantissa including
		// the binary point. So we just need to compute
		// log(m x 2^(e-1023))
		// = log(m) + log(2^(e-1023))
		// = log(m) + (e-1023) * log(2)

		// Check the exponent
		if (e == 0) {
			return (m == 0L) ? Double.NEGATIVE_INFINITY : computeSubnormal(m << 1);
		}

		// When the value is close to 1 then the relative error can be very large
		if ((e == 1022 && m >= LOWER_BOUND_MANTISSA) || (e == 1023 && m <= UPPER_BOUND_MANTISSA)) {
			return Math.log(x);
		}

		// return logMantissa[(int) (m >>> qd)] + logExpD[e];
		return logMantissa[(int) (m >>> qd)] + (e - 1023) * LN2;
	}

	/**
	 * Calculate the natural logarithm. Requires the argument be finite and
	 * positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog(Double.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code >fastLog(Double.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log(x)
	 */
	@Override
	public double fastLogD(double x) {
		// As above but no checks for NaN or infinity
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);
		if (e == 0) {
			return (m == 0L) ? Double.NEGATIVE_INFINITY : computeSubnormal(m << 1);
		}
		if ((e == 1022 && m >= LOWER_BOUND_MANTISSA) || (e == 1023 && m <= UPPER_BOUND_MANTISSA)) {
			return Math.log(x);
		}
		// return logMantissa[(int) (m >>> qd)] + logExpD[e];
		return logMantissa[(int) (m >>> qd)] + (e - 1023) * LN2;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Non-specialised version which calls the corresponding log function and
	 * divides by {@code Math.log(2)}.
	 */
	@Override
	public float log2(float x) {
		return log(x) / LN2F;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Non-specialised version which calls the corresponding log function and
	 * divides by {@code Math.log(2)}.
	 */
	@Override
	public float log2(double x) {
		return log(x) / LN2F;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Non-specialised version which calls the corresponding log function and
	 * divides by {@code Math.log(2)}.
	 */
	@Override
	public float fastLog2(float x) {
		return fastLog(x) / LN2F;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Non-specialised version which calls the corresponding log function and
	 * divides by {@code Math.log(2)}.
	 */
	@Override
	public float fastLog2(double x) {
		return fastLog(x) / LN2F;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Non-specialised version which calls the corresponding log function and
	 * divides by {@code Math.log(2)}.
	 */
	@Override
	public double log2D(double x) {
		return log(x) / LN2;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * Non-specialised version which calls the corresponding log function and
	 * divides by {@code Math.log(2)}.
	 */
	@Override
	public double fastLog2D(double x) {
		return fastLog(x) / LN2;
	}
}