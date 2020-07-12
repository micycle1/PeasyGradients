package peasyGradients.utilities.fastLog;

/**
 * Implementation of the ICSILog algorithm as described in O. Vinyals, G.
 * Friedland, N. Mirghafori "Revisiting a basic function on current CPUs: A fast
 * logarithm implementation with adjustable accuracy" (2007).
 *
 * <p>
 * This class is based on the original algorithm description and a Java
 * implementation by Hanns Holger Rutz.
 *
 * @see <a href=
 *      "https://www.javatips.net/api/Eisenkraut-master/src/main/java/de/sciss/eisenkraut/math/FastLog.java">https://www.javatips.net/api/Eisenkraut-master/src/main/java/de/sciss/eisenkraut/math/FastLog.java</a>
 * @see <a href=
 *      "http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf">http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf</a>
 */
public class FFastLog extends FastLog {
	/** The base. */
	private final double base;
	/** The number of bits to remove from a float mantissa. */
	// CHECKSTYLE.OFF: MemberName
	private final int q;
	// CHECKSTYLE.ON: MemberName
	/** (q-1). */
	private final int qm1;
	/** The number of bits to remove from a double mantissa. */
	private final int qd;
	/** (qd-1). */
	private final int qdm1;
	/**
	 * The table of the log2 value of binary number 1.0000... to 1.1111....,
	 * depending on the precision. The table has had the float bias (127) and
	 * mantissa size (23) pre-subtracted (i.e. -150 in total).
	 */
	private final float[] data;
	/** The scale used to convert the log2 to the logB (using the base). */
	private final float scale;

	/**
	 * Create a new natural logarithm calculation instance.
	 */
	public FFastLog() {
		this(Math.E, N);
	}

	/**
	 * Create a new logarithm calculation instance. This will hold the
	 * pre-calculated log values for base E and a table size depending on a given
	 * mantissa quantization.
	 *
	 * @param n The number of bits to keep from the mantissa. Table storage =
	 *          2^(n+1) * 4 bytes, e.g. 64Kb for n=13
	 */
	public FFastLog(int n) {
		this(Math.E, n);
	}

	/**
	 * Create a new logarithm calculation instance. This will hold the
	 * pre-calculated log values for a given base and a table size depending on a
	 * given mantissa quantization.
	 *
	 * @param base the logarithm base (e.g. 2 for log duals, 10 for decibels
	 *             calculations, Math.E for natural log)
	 * @param n    The number of bits to keep from the mantissa. Table storage =
	 *             2^(n+1) * 4 bytes, e.g. 64Kb for n=13
	 */
	public FFastLog(double base, int n) {
		if (n < 0 || n > 23) {
			throw new IllegalArgumentException("N must be in the range 0<=n<=23");
		}
		scale = (float) computeScale(base);
		this.base = base;

		final int size = 1 << (n + 1);

		q = 23 - n;
		qm1 = q - 1;
		qd = 52 - n;
		qdm1 = qd - 1;
		data = new float[size];

		for (int i = 0; i < size; i++) {
			data[i] = (float) (exactLog2(i << q) - 150);
		}

		// We need the complete table to do this
		// Comment out for production code since the tolerance is variable.
		// for (int i = 1; i < size; i++)
		// {
		// float value = i << q;
		// float log2 = data[i] + 150f;
		// assert Math.abs((log2 - fastLog2(value)) / log2) < 1e-6f :
		// String.format("[%d] log2(%g) %g !=
		// %g %g", i,
		// value, log2, fastLog2(value),
		// uk.ac.sussex.gdsc.core.utils.FloatEquality.relativeError(log2,
		// fastLog2(value)));
		// }
	}

	@Override
	public double getBase() {
		return base;
	}

	@Override
	public double getScale() {
		return scale;
	}

	@Override
	public int getN() {
		return 23 - q;
	}

	@Override
	public float log2(float x) {
		final int bits = Float.floatToRawIntBits(x);

		// Note the documentation from Float.intBitsToFloat(int):
		// int s = ((bits >> 31) == 0) ? 1 : -1;
		// int e = ((bits >> 23) & 0xff);
		// int m = (e == 0) ?
		// (bits & 0x7fffff) << 1 :
		// (bits & 0x7fffff) | 0x800000;
		// Then the floating-point result equals the value of the mathematical
		// expression s x m x 2^(e-150):
		// e-127 is the unbiased exponent. 23 is the mantissa precision
		// = s x m x 2^(e-127-23)

		final int e = (bits >> 23) & 0xff;
		// raw mantissa, conversion is done with the bit shift to reduce precision
		final int m = (bits & 0x7fffff);

		if (e == 255) {
			// All bits set is a special case
			if (m != 0) {
				return Float.NaN;
			}
			// +/- Infinity
			return ((bits >> 31) != 0) ? Float.NaN : Float.POSITIVE_INFINITY;
		}

		if ((bits >> 31) != 0) {
			// Only -0 is allowed
			return (e == 0 && m == 0) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}

		return (e == 0 ? data[m >>> qm1] : e + data[((m | 0x00800000) >>> q)]);
	}

	@Override
	public float log2(double x) {
		final long bits = Double.doubleToRawLongBits(x);

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

		// Get the biased exponent
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		// Mantissa
		final long m = (bits & 0xfffffffffffffL);

		if (e == 2047) {
			// All bits set is a special case
			if (m != 0) {
				return Float.NaN;
			}
			// +/- Infinity
			return ((bits >> 63) != 0L) ? Float.NaN : Float.POSITIVE_INFINITY;
		}

		// Check for negatives
		if ((bits >> 63) != 0L) {
			// Only -0 is allowed
			return (e == 0 && m == 0L) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}

		// We must subtract -1075 from the exponent.
		// However -150 has been pre-subtracted in the table.
		// and the mantissa has 29 more digits of significance.
		// So take away 1075-150-29 = 896.
		return (e == 0 ? data[(int) (m >>> qdm1)] - 896 : e - 896 + data[(int) ((m | 0x10000000000000L) >>> qd)]);
	}

	/**
	 * Calculate the logarithm using base 2. Requires the argument be finite and
	 * positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog2(Float.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog2(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code fastLog2(Float.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log2( x )
	 */
	@Override
	public float fastLog2(float x) {
		final int bits = Float.floatToRawIntBits(x);
		final int e = (bits >> 23) & 0xff;
		final int m = (bits & 0x7fffff);
		return (e == 0 ? data[m >>> qm1] : e + data[((m | 0x00800000) >>> q)]);
	}

	/**
	 * Calculate the logarithm using base 2. Requires the argument be finite and
	 * positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog2(Float.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog2(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code fastLog2(Float.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log( x )
	 */
	@Override
	public float fastLog2(double x) {
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);
		return (e == 0 ? data[(int) (m >>> qdm1)] - 896 : e - 896 + data[(int) ((m | 0x10000000000000L) >>> qd)]);
	}

	@Override
	public float log(float x) {
		// Re-implement to avoid float comparisons (which will be slower than int
		// comparisons)
		final int bits = Float.floatToRawIntBits(x);
		final int e = (bits >> 23) & 0xff;
		final int m = (bits & 0x7fffff);
		if (e == 255) {
			if (m != 0) {
				return Float.NaN;
			}
			return ((bits >> 31) != 0) ? Float.NaN : Float.POSITIVE_INFINITY;
		}
		if ((bits >> 31) != 0) {
			return (e == 0 && m == 0) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}
		return (e == 0 ? data[m >>> qm1] : e + data[((m | 0x00800000) >>> q)]) * scale;
	}

	@Override
	public float log(double x) {
		final long bits = Double.doubleToRawLongBits(x);

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

		// Get the biased exponent
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		// Mantissa
		final long m = (bits & 0xfffffffffffffL);

		if (e == 2047) {
			// All bits set is a special case
			if (m != 0) {
				return Float.NaN;
			}
			// +/- Infinity
			return ((bits >> 63) != 0L) ? Float.NaN : Float.POSITIVE_INFINITY;
		}

		// Check for negatives
		if ((bits >> 63) != 0L) {
			// Only -0 is allowed
			return (e == 0 && m == 0L) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}

		// We must subtract -1075 from the exponent.
		// However -150 has been pre-subtracted in the table.
		// and the mantissa has 29 more digits of significance.
		// So take away 1075-150-29 = 896.
		return (e == 0 ? data[(int) (m >>> qdm1)] - 896 : e - 896 + data[(int) ((m | 0x10000000000000L) >>> qd)]) * scale;
	}

	/**
	 * Calculate the logarithm to the base given in the constructor. Requires the
	 * argument be finite and positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog(Float.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code fastLog(Float.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log( x )
	 */
	@Override
	public float fastLog(float x) {
		final int bits = Float.floatToRawIntBits(x);
		final int e = (bits >> 23) & 0xff;
		final int m = (bits & 0x7fffff);
		return (e == 0 ? data[m >>> qm1] : e + data[((m | 0x00800000) >>> q)]) * scale;
	}

	/**
	 * Calculate the logarithm to the base given in the constructor. Requires the
	 * argument be finite and positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect
	 * ({@code >fastLog(Float.MAX_VALUE)}).
	 * <li>If the argument is negative, then the result is incorrect
	 * ({@code fastLog(-x)}).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * ({@code fastLog(Float.MAX_VALUE)}).
	 * <li>If the argument is positive zero or negative zero, then the result is
	 * negative infinity.
	 * </ul>
	 *
	 * @param x the argument (must be strictly positive)
	 * @return log( x )
	 */
	@Override
	public float fastLog(double x) {
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);
		return (e == 0 ? data[(int) (m >>> qdm1)] - 896 : e - 896 + data[(int) ((m | 0x10000000000000L) >>> qd)]) * scale;
	}
}