package peasyGradients;

import static processing.core.PApplet.round;

import net.jafama.FastMath;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PVector;

/**
 * HSB to RGB RGB to HSB
 * 
 * @author Jeremy Behreand
 * @author micycle1
 *
 */
public final class Functions {

	static enum StepModes {
		/**
		 * No transformation
		 */
		LINEAR,
		/**
		 * Ken Perlin’s smoother step, a simoid like function.
		 * 
		 * @param st step, between 0 and 1
		 * @return the new mapped step, having undergone a transformation according to a
		 *         sigmoid-like function (eg: [0.5 -> 0.5], [0.25 -> 0.104], [0.65
		 *         ->0.765])
		 */
		KPERLIN, EXPONENTIAL, CUBIC,
		/**
		 * Provides a reversible parabolic bouncing easing out function. From t=0 value
		 * starts with an accelerating motion until destination reached then it bounces
		 * back in increasingly small bounces finally settling at 1 when t=1. If the
		 * <code>direction</code> parameter is negative, the direction of the function
		 * is reversed. This can be useful for oscillating animations.
		 */
		BOUNCE,
		/**
		 * Provides an elastic easing out function simulating an increasingly agitated
		 * elastic. From t=0 value starts at 0.5 with increasingly large perturbations
		 * ending at 1 when t=1.
		 */
		ELASTIC, CIRCULAR, SINE;

		private final static StepModes[] vals = values();

		StepModes next() {
			return vals[(ordinal() + 1) % vals.length];
		}
	}

	static void nextStepMode() {
		stepMode = stepMode.next();
	}

	static StepModes stepMode = StepModes.KPERLIN;

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
				return step == 1.0f ? step : 1.0f - (float) FastMath.powQuick(2, -10 * step);
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
			case ELASTIC :
				if (step <= 0) {
					return 0.5f;
				}

				if (step >= 1) {
					return 1;
				}

				float sPrime1 = 1 - step;
				float p = 0.25f; // Period
				float a = 1.05f; // Amplitude.
				float s = 0.0501717f; // asin(1/a)*p/TWO_PI;

				return (float) Math.min(1,
						0.5 - a * FastMath.pow(2, -10 * sPrime1) * FastMath.sinQuick((sPrime1 - s) * PConstants.TWO_PI / p));
			case CIRCULAR :
//				return PApplet.sqrt();
				return (float) FastMath.sqrtQuick((2.0 - step) * step);
			case SINE :
				PApplet.sin(step * PConstants.HALF_PI);
			default :
				return step;
		}
	}

	/**
	 * Project a given pixel coordinate (x, y) onto the imaginary spine of the
	 * gradient.
	 * 
	 * @param originX
	 * @param originY
	 * @param destX
	 * @param destY
	 * @param pointX
	 * @param pointY
	 * @return between 0...1
	 */
	static float project(float originX, float originY, float destX, float destY, int pointX, int pointY) {
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
	static float angleBetween(PVector tail, PVector head) {
		float a = PApplet.atan2(tail.y - head.y, tail.x - head.x);
		if (a < 0) {
			a += PConstants.TWO_PI;
		}
		return a;
	}

	/**
	 * (1) decompose the color, (2) convert the color data from RGB channels to
	 * those of the schema, (3) perform any desired transformations on these colors,
	 * (4) convert back to RGB, and then (5) recompose the color.
	 * 
	 * @param in
	 * @return
	 */
	static int composeclr(float[] in) {
		return composeclr(in[0], in[1], in[2], in[3]);
	}

	/**
	 * Compose an RGBA color using
	 * 
	 * @param red
	 * @param green
	 * @param blue
	 * @param alpha
	 * @return integer representation of RGBA
	 */
	static int composeclr(float red, float green, float blue, float alpha) {
		return round(alpha * 255) << 24 | round(red * 255) << 16 | round(green * 255) << 8 | round(blue * 255);
	}

	static int composeclr(float red, float green, float blue) {
		return 255 << 24 | (int) red << 16 | (int) green << 8 | (int) blue;
	}

	/**
	 * Decompose color integer (RGBA) into 4 separate components and scale between
	 * 0...1
	 * 
	 * @param clr
	 * @param out
	 * @return
	 */
	static float[] decomposeclr(int clr, float[] out) {
		// 1.0 / 255.0 = 0.003921569
		out[3] = (clr >> 24 & 0xff) * 0.003921569f;
		out[0] = (clr >> 16 & 0xff) * 0.003921569f;
		out[1] = (clr >> 8 & 0xff) * 0.003921569f;
		out[2] = (clr & 0xff) * 0.003921569f;
		return out;
	}

	/**
	 * Decompose color integer (RGBA) into 4 separate components (0...255)
	 * 
	 * @param clr
	 * @param out
	 * @return
	 */
	static float[] decomposeclr(int clr) {
		float[] out = new float[3];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		return out;
	}

	static double[] decomposeclrDouble(int clr) {
		double[] out = new double[3];
		out[0] = (clr >> 16 & 0xff);
		out[1] = (clr >> 8 & 0xff);
		out[2] = (clr & 0xff);
		return out;
	}

}
