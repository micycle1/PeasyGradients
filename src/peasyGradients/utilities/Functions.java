package peasyGradients.utilities;

import java.util.Random;

import net.jafama.FastMath;
import peasyGradients.utilities.fastLog.DFastLog;
import peasyGradients.utilities.fastLog.FastLog;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

/**
 * This class contains static functions mostly related to colour/gradient
 * processing.
 * 
 * @author micycle1
 * @author Jeremy Behreand (colour composition methods)
 */
public final class Functions {
	
	private static final FastLog fastLog = new DFastLog(12);
	private static final float PI = (float) Math.PI;
	private static final float HALF_PI = (float) (0.5f * Math.PI);
	private static final float QRTR_PI = (float) (0.25f * Math.PI);

	private static final Random random = new Random();

	public static void nextStepMode() {
		stepMode = stepMode.next();
	}

	public static void prevStepMode() {
		stepMode = stepMode.prev();
	}

	public static Interpolation stepMode = Interpolation.IDENTITY;

	/**
	 * Calculate the step by passing it to the selected smoothing function. Allows
	 * gradient renderer to easily change how the gradient is smoothed.
	 * 
	 * @param step
	 * @return the new step (mapped to a
	 */
	public static float functStep(float step) {
		switch (stepMode) {
			case LINEAR :
				return step;
			case KPERLIN :
				return step * step * step * (step * (step * 6 - 15) + 10);
			case EXPONENTIAL :
				return step == 1.0f ? step : 1.0f - FastPow.fastPow(2, -10 * step);
			case CUBIC :
				return step * step * step;
			case BOUNCE :
				float sPrime = step;

				if (sPrime < 0.36364) { // 1/2.75
					return 7.5625f * sPrime * sPrime;
				}
				if (sPrime < 0.72727) // 2/2.75
				{
					return 7.5625f * (sPrime -= 0.545454f) * sPrime + 0.75f;
				}
				if (sPrime < 0.90909) // 2.5/2.75
				{
					return 7.5625f * (sPrime -= 0.81818f) * sPrime + 0.9375f;
				}

				return 7.5625f * (sPrime -= 0.95455f) * sPrime + 0.984375f;
			case CIRCULAR :
				return (float) FastMath.sqrtQuick((2.0 - step) * step);
			case SINE :
				return (float) FastMath.sinQuick(step);
			case PARABOLA :
				return (float) FastMath.sqrt(4.0 * step * (1.0 - step));
			case IDENTITY :
				return step * step * (2.0f - step);
			case SINC :
				final double z = FastMath.PI * (5 * step - 1.0);
				return (float) FastMath.abs((FastMath.sin(z) / z));
			case GAIN1 :
				final float c = (float) (0.5 * FastPow.fastPow(2.0 * ((step < 0.5) ? step : 1.0 - step), 0.3));
				return (step < 0.5) ? c : 1.0f - c;
			case GAIN2 :
				final float d = (float) (0.5 * FastPow.fastPow(2.0 * ((step < 0.5) ? step : 1.0 - step), 3.3333));
				return (step < 0.5) ? d : 1.0f - d;
			case EXPIMPULSE :
				return (float) (2 * step * FastMath.exp(1.0 - (2 * step)));
			default :
				return step;
		}
	}

	/**
	 * Linearly interpolate between 2 colours (color-space independent)
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
	 * Processing's native lerpColor() method (which is linear).
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
	 * TODO https://discourse.processing.org/t/per-vertex-color-gradient-fill/9679/7
	 * 
	 * @param w
	 * @param h
	 * @param corners
	 * @return
	 */
	static int[] interpolateBilinear(int w, int h, int[] corners) {
		int[] arr = new int[w * h];
//		  for (int x = 0; x < w; x++) {
//		    float xinc = (float) x/w;
//		    RGB.interpolate(decomposeclr(corners[0], corners[0]), col2, st, out)
//		    int t = lerpint(corners[0], corners[2], xinc);
//		    int b = lerpint(corners[1], corners[3], xinc);
//		    for (int y = 0; y < h; y++) {
//		      float yinc = (float) y/h;
//		      int m = lerpint(t, b, yinc);
//		      arr[x + y*w] = m;
//		    }
//		  }
		return arr;
	}

	/**
	 * Project a given pixel coordinate (x, y) onto the imaginary spine of the
	 * gradient (linear gradients only).
	 * 
	 * @param originX
	 * @param originY
	 * @param destX
	 * @param destY
	 * @param pointX
	 * @param pointY
	 * @return between 0...1
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
		float a = (float) Math.atan2(tail.y - head.y, tail.x - head.x);
		if (a < 0) {
			a += PConstants.TWO_PI;
		}
		return a;
	}

	/**
	 * Compose an sRGBA int from float[] 0...1
	 * 
	 * @param in
	 * @return
	 */
	public static int composeclr(float[] RGBA) {
		return (int) (RGBA[3] * 255) << 24 | (int) (RGBA[0] * 255) << 16 | (int) (RGBA[1] * 255) << 8 | (int) (RGBA[2] * 255);
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

	private static int fullAlpha = 255 << 24;

	/**
	 * sRGB (0...1) in
	 * 
	 * @param in
	 * @return
	 */
	public static int composeclr(double[] in) {
		return fullAlpha | (int) (in[0] * 255 + 0.5) << 16 | (int) (in[1] * 255 + 0.5) << 8 | (int) (in[2] * 255 + 0.5);
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
		return new float[] { (clr >> 16 & 0xff) * 0.003921569f, (clr >> 8 & 0xff) * 0.003921569f, (clr & 0xff) * 0.003921569f,
				(clr >> 24 & 0xff) * 0.003921569f };
	}

	/**
	 * Decompose color integer (RGBA) into 4 separate components (0...255)
	 * 
	 * @param clr
	 * @param out
	 * @return [R,G,B] 0...255
	 */
	public static float[] decomposeclrRGB(int clr) {
		float[] out = new float[3];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
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

	public static float[] decomposeclrRGBA(int clr) {
		float[] out = new float[4];
		out[3] = (clr >> 24 & 0xff) * 0.003921569f;
		out[0] = (clr >> 16 & 0xff) * 0.003921569f;
		out[1] = (clr >> 8 & 0xff) * 0.003921569f;
		out[2] = (clr & 0xff) * 0.003921569f;
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
		out[0] = (clr >> 16 & 0xff) * 0.003921569f;
		out[1] = (clr >> 8 & 0xff) * 0.003921569f;
		out[2] = (clr & 0xff) * 0.003921569f;
		return out;
	}

	/**
	 * Min of 3 floats
	 */
	public static float min(float a, float b, float c) {
		return (a < b) ? ((a < c) ? a : c) : ((b < c) ? b : c);
	}

	/**
	 * Max of 3 floats
	 */
	public static float max(float a, float b, float c) {
		return (a > b) ? ((a > c) ? a : c) : ((b > c) ? b : c);
	}
	
	public static double floorMod(double t, double b) {
		return (t - b * Math.floor(t / b));
	}

	/**
	 * Returns a pseudorandom, uniformly distributed float value between the given range.
	 * @param min range min
	 * @param max range max
	 * @return
	 */
	public static float random(float min, float max) {
		return min + random.nextFloat() * (max - min);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed float value between 0.0 and
	 * 1.0.
	 * 
	 * @return
	 */
	public static float randomFloat() {
		return random.nextFloat();
	}
	
	/**
	 * Very fast and fairly accurate (for its speed).
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
		
	/**
	 * Polynomial approximating arctangenet on the range -1, 1.
	 * Implementation of function in 'Efficient Approximations for the Arctangent Function' by Rajan et al.
	 * Maximum absolute error of 0.0038 rad (0.22º) 
	 * 
	 * @param z
	 * @return
	 */
	public static float fastAtan(float z) {
		return z * (QRTR_PI + 0.273f * (1 - Math.abs(z)));
	}

	/**
	 * atan2 Approximation. Maximum absolute error of 0.0038 rad (0.22º)
	 * Source: https://www.dsprelated.com/showarticle/1052.php
	 * @param y
	 * @param x
	 * @return
	 * @see #fastAtan(float)
	 */
	public static float fastAtan2(float y, float x) {
		if (x != 0.0f) {
			if (Math.abs(x) > Math.abs(y)) {
				float z = y / x;
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
				final float z = x / y;
				if (y > 0.0) {
					// atan2(y,x) = PI/2 - atan(x/y) if |y/x| > 1, y > 0
					return -fastAtan(z) + HALF_PI;
				} else {
					// atan2(y,x) = -PI/2 - atan(x/y) if |y/x| > 1, y < 0
					return -fastAtan(z) - HALF_PI;
				}
			}
		} else {
			if (y > 0.0f) { // x = 0, y > 0
				return HALF_PI;
			} else if (y < 0.0f) { // x = 0, y < 0
				return -HALF_PI;
			}
		}
		return 0.0f; // x,y = 0. Could return NaN instead.
	}
	
	/**
	 * Finds the two points of intersection between a rectange and a line, which
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

		float tanA = (float) Math.tan(PApplet.TWO_PI - angle); // TWO_PI - ... clockwise orientation

		// Avoid division by zero
		if (tanA == 0) {
			output[0].x = rect[3].x;
			output[0].y = point.y;
			output[1].x = rect[3].x;
			output[1].y = point.y;
		} else {
			// Transform input (make p relative to rectangle)
			point.x -= rect[0].x;
			point.y -= rect[0].y;

			float w = rect[3].x - rect[0].x;
			float h = rect[1].y - rect[0].y;
			calcProjection(w, h, point, tanA, output);

			// Transform result back to original coordinates
			output[0].x += rect[0].x;
			output[0].y += rect[0].y;
			output[1].x += rect[0].x;
			output[1].y += rect[0].y;
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
