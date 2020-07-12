package peasyGradients.utilities.fastLog;

/**
 * Implementation of the ICSILog algorithm as described in O. Vinyals, G.
 * Friedland, N. Mirghafori "Revisiting a basic function on current CPUs: A fast
 * logarithm implementation with adjustable accuracy" (2007).
 *
 * <p>
 * This class is based on the original algorithm description and a Java
 * implementation by Hanns Holger Rutz. It has been adapted for use with
 * double-precision data.
 *
 * <p>
 * Note: log(float) is not provided as it is dynamically cast up to a double and
 * float values can be represented as a double. If your computation generates a
 * float value to be logged then use {@link FFastLog}.
 *
 * @see <a href=
 *      "https://www.javatips.net/api/Eisenkraut-master/src/main/java/de/sciss/eisenkraut/math/FastLog.java">https://www.javatips.net/api/Eisenkraut-master/src/main/java/de/sciss/eisenkraut/math/FastLog.java</a>
 * @see <a href=
 *      "http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf">http://www.icsi.berkeley.edu/pubs/techreports/TR-07-002.pdf</a>
 */
public class DFastLog extends FastLog {
	/** The base. */
	private final double base;
	/** The number of bits to remove from a float mantissa. */
	// CHECKSTYLE.OFF: MemberName
	private final int q;
	// CHECKSTYLE.ON: MemberName
	/** (q-1). */
	private final int qm1;
	/**
	 * The table of the log2 value of binary number 1.0000... to 1.1111....,
	 * depending on the precision. The table has had the float bias (1023) and
	 * mantissa size (52) pre-subtracted (i.e. -1075 in total).
	 */
	private final float[] data;
	/** The scale used to convert the log2 to the logB (using the base). */
	private final float scale;

	/**
	 * Create a new natural logarithm calculation instance.
	 */
	public DFastLog() {
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
	public DFastLog(int n) {
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
	public DFastLog(double base, int n) {
		// Note: Only support the max precision we can store in a table.
		// Note: A large table would be very costly to compute!
		if (n < 0 || n > 30) {
			throw new IllegalArgumentException("N must be in the range 0<=n<=30");
		}
		scale = (float) computeScale(base);
		this.base = base;

		final int size = 1 << (n + 1);

		q = 52 - n;
		qm1 = q - 1;
		data = new float[size];

		for (int i = 0; i < size; i++) {
			data[i] = (float) (exactLog2(((long) i) << q) - 1075);
		}
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
		return 52 - q;
	}

	/**
	 * Gets the number of least signification bits to ignore from the mantissa of a
	 * double (52-bits).
	 *
	 * @return the number of least signification bits to ignore
	 */
	public int getQ() {
		return q;
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

		final int e = (int) ((bits >>> 52) & 0x7ffL);
		// raw mantissa, conversion is done with the bit shift to reduce precision
		final long m = (bits & 0xfffffffffffffL);

		if (e == 2047) {
			// All bits set is a special case
			if (m != 0) {
				return Float.NaN;
			}
			// +/- Infinity
			return ((bits >> 63) != 0L) ? Float.NaN : Float.POSITIVE_INFINITY;
		}

		if ((bits >> 63) != 0L) {
			// Only -0 is allowed
			return (e == 0 && m == 0) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}

		return (e == 0 ? data[(int) (m >>> qm1)] : e + data[(int) ((m | 0x10000000000000L) >>> q)]);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This just calls {@link #log2(double)}. Use {@link FFastLog} for a dedicated
	 * {@code float version}.
	 */
	@Override
	public float log2(float x) {
		return log2((double) x);
	}

	/**
	 * Calculate the logarithm to base 2. Requires the argument be finite and
	 * positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect.
	 * <li>If the argument is negative, then the result is incorrect (log(-x)).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * (Math.log(Double.MAX_VALUE)).
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
		return (e == 0 ? data[(int) (m >>> qm1)] : e + data[(int) ((m | 0x10000000000000L) >>> q)]);
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This just calls {@link #fastLog2(double)}. Use {@link FFastLog} for a
	 * dedicated {@code float version}.
	 */
	@Override
	public float fastLog2(float x) {
		return fastLog2((double) x);
	}

	@Override
	public float log(double x) {
		final long bits = Double.doubleToRawLongBits(x);
		final int e = (int) ((bits >>> 52) & 0x7ffL);
		final long m = (bits & 0xfffffffffffffL);
		if (e == 2047) {
			if (m != 0) {
				return Float.NaN;
			}
			return ((bits >> 63) != 0L) ? Float.NaN : Float.POSITIVE_INFINITY;
		}
		if ((bits >> 63) != 0L) {
			return (e == 0 && m == 0) ? Float.NEGATIVE_INFINITY : Float.NaN;
		}
		return (e == 0 ? data[(int) (m >>> qm1)] : e + data[(int) ((m | 0x10000000000000L) >>> q)]) * scale;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This just calls {@link #log(double)}. Use {@link FFastLog} for a dedicated
	 * {@code float version}.
	 */
	@Override
	public float log(float x) {
		return log((double) x);
	}

	/**
	 * Calculate the logarithm to the base given in the constructor. Requires the
	 * argument be finite and positive.
	 *
	 * <p>
	 * Special cases:
	 * <ul>
	 * <li>If the argument is NaN, then the result is incorrect.
	 * <li>If the argument is negative, then the result is incorrect (log(-x)).
	 * <li>If the argument is positive infinity, then the result is incorrect
	 * (Math.log(Double.MAX_VALUE)).
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
		return (e == 0 ? data[(int) (m >>> qm1)] : e + data[(int) ((m | 0x10000000000000L) >>> q)]) * scale;
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>
	 * This just calls {@link #fastLog(double)}. Use {@link FFastLog} for a
	 * dedicated {@code float version}.
	 */
	@Override
	public float fastLog(float x) {
		return fastLog((double) x);
	}
}