package micycle.peasygradients.utilities;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import micycle.peasygradients.utilities.fastLog.DFastLog;
import micycle.peasygradients.utilities.fastLog.FastLog;
import processing.core.PVector;

/**
 * A class containing static functions mostly related to color/gradient
 * processing.
 * 
 * @author Michael Carleton
 */
public final class Functions {

	// TODO DELTA E 2000 COLOR DIFF
	// https://github.com/wuchubuzai/OpenIMAJ/blob/master/image/image-processing/src/main/java/org/openimaj/image/analysis/colour/CIEDE2000.java
	// OR http://www.easyrgb.com/en/math.php

	private static final FastLog fastLog = new DFastLog(10); // Used in veryFastPow(), 10 => 2KB table

	private static final float PI = (float) Math.PI;
	private static final float TWO_PI = (float) (2 * Math.PI);
	private static final float HALF_PI = (float) (0.5f * Math.PI);
	private static final float QRTR_PI = (float) (0.25f * Math.PI);
	private static final float THREE_QRTR_PI = (float) (0.75f * Math.PI);

	private static final Random random = ThreadLocalRandom.current();

	/**
	 * Project a given 2D pixel coordinate (x, y) onto a position (0...1) of a
	 * imaginary 1D spine of a linear gradient given by its start and end points.
	 * 
	 * @param originX x coord of gradient spline origin
	 * @param originY y coord of gradient spline origin
	 * @param destX   x coord of gradient spline destination
	 * @param destY   y coord of gradient spline destination
	 * @param pointX  x coord of point to project onto spline
	 * @param pointY  y coord of point to project onto spline
	 * @return position (0...1) that the point occurs on in a gradient [0...1]
	 */
	public static float linearProject(float originX, float originY, float destX, float destY, int pointX, int pointY) {
		// Rise and run of line.
		float odX = destX - originX;
		float odY = destY - originY;

		// Distance-squared of line.
		float odSq = odX * odX + odY * odY;

		// Rise and run of projection.
		float opX = pointX - originX;
		float opY = pointY - originY;
		float opXod = opX * odX + opY * odY;

		// Normalize and clamp range.
		float div = opXod / odSq;
		return (div < 0) ? 0 : (div > 1 ? 1 : div);
	}

	/**
	 * East = 0; North = -1/2PI; West = -PI; South = -3/2PI | 1/2PI
	 * 
	 * @param tail PVector Coordinate 1.
	 * @param head PVector Coordinate 2.
	 * @return float θ in radians.
	 */
	public static float angleBetween(PVector tail, PVector head) {
		float a = fastAtan2b(tail.y - head.y, tail.x - head.x);
		if (a < 0) {
			a += TWO_PI;
		}
		return a;
	}

	public static double angleBetween(PVector head, float tailX, float tailY) {
		double a = fastAtan2b(tailY - head.y, tailX - head.x);
		if (a < 0) {
			a += TWO_PI;
		}
		return a;
	}

	/**
	 * Min of 3 floats
	 */
	public static float min(float a, float b, float c) {
		return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c);
	}

	/**
	 * Min of 3 doubles
	 */
	public static double min(double a, double b, double c) {
		return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c);
	}

	/**
	 * Max of 3 floats
	 */
	public static float max(float a, float b, float c) {
		return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
	}

	/*
	 * Max of 3 doubles
	 */
	public static double max(double a, double b, double c) {
		return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
	}

	/**
	 * Floor modulo for doubles.
	 * 
	 * @param t
	 * @param b
	 * @return
	 */
	public static double floorMod(double t, double b) {
		return (t - b * Math.floor(t / b));
	}

	/**
	 * Returns a pseudorandom, uniformly distributed float value between the given
	 * range.
	 * 
	 * @param min range min
	 * @param max range max
	 * @return
	 */
	public static float random(float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed float value between 0.0
	 * (inclusive) and 1.0 (exclusive).
	 * 
	 * @return
	 */
	public static float randomFloat() {
		return random.nextFloat();
	}

	/**
	 * Returns a pseudorandom, uniformly distributed int value between min
	 * (inclusive) and max (inclusive),
	 * 
	 * @param min
	 * @param max
	 * @return
	 */
	public static int randomInt(int min, int max) {
		return random.nextInt(max + 1 - min) + min; // + 1 for inclusive
	}

	/**
	 * Returns of string representation of the input array; array values are
	 * formatted to n decimal places, and padded with zeros (optional).
	 * 
	 * @param array
	 * @param decimalPlaces number of decimal places to retain
	 * @param padding       default is 3 when decimal places = 0
	 * @param open          character to open the array. e.g. '['
	 * @param close         character to close the array. e.g. ']'
	 * @return something like "[1, 2, 3, 4]"
	 */
	public static String formatArray(double[] array, int decimalPlaces, int padding, char open, char close) {
		double[] out = array.clone();

		int i = 0;
		if (decimalPlaces == 0) { // assuming array values > 1
			String outString = open + String.format("%03d", Math.round(out[0])) + ", ";

			for (int j = 1; j < out.length - 1; j++) {
				outString += String.format("%03d", Math.round(out[j])) + ", ";
			}
			outString += String.format("%03d", Math.round(out[out.length - 1])) + close;
			return outString;
		} else {
			DecimalFormat df = new DecimalFormat(
					"0" + new String(new char[padding]).replace("\0", "0") + "." + new String(new char[decimalPlaces]).replace("\0", "0"));
			for (double d : out) {
				out[i] = Double.valueOf(df.format(d));
				i++;
			}
		}
		return Arrays.toString(out);
	}

	/**
	 * Format array, opening and closing with '[....]'
	 * 
	 * @param array
	 * @param decimalPlaces
	 * @param padding
	 * @return
	 */
	public static String formatArray(double[] array, int decimalPlaces, int padding) {
		return formatArray(array, decimalPlaces, padding, '[', ']');
	}

	/**
	 * Very fast and fairly accurate (for its speed).
	 * 
	 * @param f1 base
	 * @param f2 exponent
	 * @return approximate value
	 */
	public static double veryFastPow(double f1, double f2) {
		double rv, ln, am1;

		ln = fastLog.log(f1);
		am1 = f2 - 1.0;
		rv = f1 * ln * am1;

		ln *= ln;
		am1 *= am1;

		rv += .5 * f1 * ln * am1;

		rv += f1;

		return rv;
	}

	/**
	 * pow approximation with exponentiation by squaring. Very fast, but with
	 * appreciable inaccuracy. https://pastebin.com/ZW95gEyr
	 * 
	 * @param a base
	 * @param b exponent
	 * @return a^b (roughly)
	 */
	public static double veryVeryFastPow(final double a, final double b) {
		// exponentiation by squaring
		double r = 1.0;
		int exp = (int) b;
		double base = a;
		while (exp != 0) {
			if ((exp & 1) != 0) {
				r *= base;
			}
			base *= base;
			exp >>= 1;
		}

		// use the IEEE 754 trick for the fraction of the exponent
		final double b_faction = b - (int) b;
		final long tmp = Double.doubleToLongBits(a);
		final long tmp2 = (long) (b_faction * (tmp - 4606921280493453312L)) + 4606921280493453312L;
		return r * Double.longBitsToDouble(tmp2);
	}

	public static double fastSqrt(double d) {
		return Double.longBitsToDouble(((Double.doubleToLongBits(d) - (1L << 52)) >> 1) + (1L << 61));
	}

	/**
	 * rel. error of 0.000892
	 */
	public static float fastInvSqrt(float x) {
		final float xhalf = 0.5f * x;
		int i = Float.floatToIntBits(x);
		i = 0x5F376908 - (i >> 1);
		x = Float.intBitsToFloat(i);
		return x *= (1.5008909f - xhalf * x * x);
	}

	/**
	 * rel. error of 0.035
	 */
	public static float veryFastInvSqrt(float x) {
		return Float.intBitsToFloat(0x5F37624F - (Float.floatToIntBits(x) >> 1));
	}

	/**
	 * Fast round from float to int. This is faster than Math.round() though it may
	 * return slightly different results. It does not try to handle NaN or
	 * infinities.
	 */
	public static int fastRound(float value) {
		long lx = (long) (value * (65536 * 256f));
		return (int) ((lx + 0x800000) >> 24);
	}

	/**
	 * More accurate for smaller values of z
	 * 
	 * @param z
	 * @return
	 */
	public static double fastSin(double z) {
		return z - 0.166666667f * z + 0.008833333333f * (z * z * z * z * z);
	}

	/**
	 * Polynomial approximating arctangenet on the range -1, 1. Implementation of
	 * function in 'Efficient Approximations for the Arctangent Function' by Rajan
	 * et al. Maximum absolute error of 0.0038 rad (0.22º)
	 * 
	 * @param z
	 * @return
	 */
	public static double fastAtan(double z) {
		return z * (QRTR_PI + 0.273f * (1 - Math.abs(z)));
	}

	/**
	 * atan2 Approximation. Maximum absolute error of 0.0038 rad (0.22º) Source:
	 * https://www.dsprelated.com/showarticle/1052.php
	 * 
	 * @param y
	 * @param x
	 * @return
	 * @see #fastAtan(float)
	 */
	public static double fastAtan2a(double y, double x) {
		if (x != 0.0f) {
			if (Math.abs(x) > Math.abs(y)) {
				double z = y / x;
				if (x > 0.0) {
					// atan2(y,x) = atan(y/x) if x > 0
					return fastAtan(z);
				} else if (y >= 0.0) {
					// atan2(y,x) = atan(y/x) + PI if x < 0, y >= 0
					return fastAtan(z) + PI;
				} else {
					// atan2(y,x) = atan(y/x) - PI if x < 0, y < 0
					return fastAtan(z) - PI;
				}
			} else // Use property atan(y/x) = PI/2 - atan(x/y) if |y/x| > 1.
			{
				final double z = x / y;
				if (y > 0.0) {
					// atan2(y,x) = PI/2 - atan(x/y) if |y/x| > 1, y > 0
					return -fastAtan(z) + HALF_PI;
				} else {
					// atan2(y,x) = -PI/2 - atan(x/y) if |y/x| > 1, y < 0
					return -fastAtan(z) - HALF_PI;
				}
			}
		} else if (y > 0.0f) { // x = 0, y > 0
			return HALF_PI;
		} else if (y < 0.0f) { // x = 0, y < 0
			return -HALF_PI;
		}
		return 0.0f; // x,y = 0. Could return NaN instead.
	}

	/**
	 * Max error of .01 rads
	 * http://dspguru.com/dsp/tricks/fixed-point-atan2-with-self-normalization/
	 * 
	 * @param y
	 * @param x
	 * @return
	 */
	public static float fastAtan2b(final float y, final float x) {

		float r, angle;
		final float abs_y = Math.abs(y) + 1e-10f; // kludge to prevent 0/0 condition

		if (x < 0.0f) {
			r = (x + abs_y) / (abs_y - x); // (3)
			angle = THREE_QRTR_PI; // (4)
		} else {
			r = (x - abs_y) / (x + abs_y); // (1)
			angle = QRTR_PI; // (2)
		}
		angle += (0.1963f * r * r - 0.9817f) * r; // (2 | 4)
		if (y < 0.0f) {
			return (-angle); // negate if in quad III or IV
		} else {
			return (angle);
		}
	}

	/**
	 * Average error of 0.00231 radians (0.1323 degrees), largest error of 0.00488
	 * radians (0.2796 degrees).
	 * 
	 * @param y
	 * @param x
	 * @return
	 */
	public static float fastAtan2c(float y, float x) {
		if (x == 0f) {
			if (y > 0f) {
				return PI / 2;
			}
			if (y == 0f) {
				return 0f;
			}
			return -PI / 2;
		}
		final float atan, z = y / x;
		if (Math.abs(z) < 1f) {
			atan = z / (1f + 0.28f * z * z);
			if (x < 0f) {
				return atan + (y < 0f ? -PI : PI);
			}
			return atan;
		}
		atan = PI / 2 - z / (z * z + 0.28f);
		return y < 0f ? atan - PI : atan;
	}

	/**
	 * Linearly interpolates between two angles in radians. Takes into account that
	 * angles wrap at two pi and always takes the direction with the smallest delta
	 * angle.
	 * 
	 * @param fromRadians start angle in radians
	 * @param toRadians   target angle in radians
	 * @param progress    interpolation value in the range [0, 1]
	 * @return the interpolated angle in the range [0, 2PI]
	 */
	public static float lerpAngle(float fromRadians, float toRadians, float progress) {
		float delta = ((toRadians - fromRadians + TWO_PI + PI) % TWO_PI) - PI;
		return (fromRadians + delta * progress + TWO_PI) % TWO_PI;
	}

	/**
	 * Finds the two points of intersection between a rectangle and a line, which
	 * given by a point inside the rectangle and an angle.
	 * <p>
	 * A Java/Processing implementation of
	 * <a href="https://gamedev.stackexchange.com/questions/124108/i-need-to-find-
	 * intersection-point-of-a-vector-in-an-axis-aligned-rectangle">this SE Game Dev
	 * answer</a>.
	 * 
	 * 
	 * @param rectCoords a float[4] containing the rectangle corner coordinates
	 *                   {UL,BL,BR,UR}
	 * @param point      2D coordinates of point within rectangle (i.e. where the
	 *                   line originates from)
	 * @param angle      angle of line in radians (where 0 faces east). Increases in
	 *                   a clockwise manner
	 * @return PVector[2] containing the two points of intersection
	 * @see #lineRectIntersection(float, float, PVector, float)
	 */
	public static PVector[] lineRectIntersection(PVector[] rect, PVector point, float angle) {

		PVector[] output = new PVector[2];
		output[0] = new PVector();
		output[1] = new PVector();

		final float tanA = (float) Math.tan(TWO_PI - angle); // 'TWO_PI - ___' for clockwise orientation
		
		// Avoid division by zero
		if (tanA == 0) {
			output[0].x = rect[3].x;
			output[0].y = point.y;
			output[1].x = rect[1].x;
			output[1].y = point.y;
		} else {
			// Transform input (make p relative to rectangle)
			point.x -= rect[0].x;
			point.y -= rect[0].y;

			float w = rect[3].x - rect[0].x;
			float h = rect[1].y - rect[0].y;

			calcProjection(w, h, point, tanA, output);

			// Transform result back to original coordinates
			output[0].add(rect[0]);
			output[1].add(rect[0]);
		}
		return output;
	}

	/**
	 * An x-axis & y-axis aligned version of
	 * {@link #lineRectIntersection(PVector[], PVector, float) this} method.
	 * 
	 * <p>
	 * This method expects a rectangle that is defined solely by it's size (width
	 * and height). Its top left corner lies on the origin [0,0]. The Y-axis of the
	 * coordinate system expands downwards.
	 * 
	 * @param rectWidth
	 * @param rectHeight
	 * @param point      2D coordinates of point within rectangle (i.e. where the
	 *                   line originates from)
	 * @param angle      angle of line in radians (where 0 faces east). Increases in
	 *                   a clockwise manner
	 * @return PVector[2] containing the two points of intersection
	 * @see #lineRectIntersection(PVector[], PVector, float)
	 */
	public static PVector[] lineRectIntersection(float rectWidth, float rectHeight, PVector point, float angle) {
		final PVector[] rect = new PVector[4];
		rect[0] = new PVector(0, 0);
		rect[1] = new PVector(0, rectHeight);
		rect[2] = new PVector(rectWidth, rectHeight);
		rect[3] = new PVector(rectWidth, 0);
		return lineRectIntersection(rect, point, angle);
	}

	/**
	 * Projects a point onto imaginary sides CD and AB of an underlying rectangle
	 * and checks for intersection.
	 * 
	 * @param w     rect width
	 * @param h     rect height
	 * @param point 2D coordinates of point within rectangle
	 * @param tanA  tangent of the angle of the line within rectangle
	 * @return points of intersection
	 */
	private static void calcProjection(float w, float h, PVector point, float tanA, PVector[] output) {

		float yCD = point.y - (tanA * (w - point.x));
		float yAB = point.y + (tanA * point.x);

		if (tanA < 0) {
			tanA = -tanA;
		}

		// First projection onto CD
		if (yCD < 0) {
			output[0].x = w + (yCD / tanA);
		} else if (yCD > h) {
			float opposite = yCD - h;
			output[0].x = w - (opposite / tanA);
			output[0].y = h;
		} else {
			output[0].x = w;
			output[0].y = yCD;
		}

		// Second projection onto AB
		if (yAB < 0) {
			output[1].x = -yAB / tanA;
		} else if (yAB > h) {
			float opposite = yAB - h;
			output[1].x = opposite / tanA;
			output[1].y = h;
		} else {
			output[1].y = yAB;
		}
	}

}
