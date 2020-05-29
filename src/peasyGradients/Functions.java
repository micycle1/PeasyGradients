package peasyGradients;

import static processing.core.PApplet.round;
import static processing.core.PApplet.pow;

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
final class Functions {

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
	static float calcStep(float step) {
		switch (stepMode) {
			case KPERLIN :
				return step * step * step * (step * (step * 6 - 15) + 10);
			case LINEAR :
				return step;
			case EXPONENTIAL :
				return step == 1.0f ? step : 1.0f - pow(2, -10 * step);
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
						0.5 - a * pow(2, -10 * sPrime1) * Math.sin((sPrime1 - s) * PConstants.TWO_PI / p));
			case CIRCULAR :
				return PApplet.sqrt((2.0f - step) * step);
			case SINE :
				PApplet.sin(step * PConstants.HALF_PI);
			default :
				return step;
		}
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
	static float[] smootherStepRgb(float[] col1, float[] col2, float st, float[] out) {
		float smoothStep = calcStep(st); // apply sigmoid-like function to step
		out[0] = col1[0] + smoothStep * (col2[0] - col1[0]);
		out[1] = col1[1] + smoothStep * (col2[1] - col1[1]);
		out[2] = col1[2] + smoothStep * (col2[2] - col1[2]);
		out[3] = col1[3] + smoothStep * (col2[3] - col1[3]);
		return out;
	}

	/**
	 * An n-color alternative to the
	 * {@link #smootherStepHsb(float[], float[], float, float[]) two color} method.
	 * 
	 * @param arr array[][] of colors, where each color[] is a [R,G,B,A] array with
	 *            each value between 0...1.
	 * @param st  step, global
	 * @param out
	 * @return
	 */
	float[] smootherStepRgb(float[][] arr, float st, float[] out) {
		int sz = arr.length;
		if (sz == 1 || st < 0) {
			out = java.util.Arrays.copyOf(arr[0], 0);
			return out;
		} else if (st > 1) {
			out = java.util.Arrays.copyOf(arr[sz - 1], 0);
			return out;
		}
		float scl = st * (sz - 1);
		int i = (int) scl;
		float eval = calcStep(scl - i);
		out[0] = arr[i][0] + eval * (arr[i + 1][0] - arr[i][0]);
		out[1] = arr[i][1] + eval * (arr[i + 1][1] - arr[i][1]);
		out[2] = arr[i][2] + eval * (arr[i + 1][2] - arr[i][2]);
		out[3] = arr[i][3] + eval * (arr[i + 1][3] - arr[i][3]);
		return out;
	}

	/**
	 * Returns a color by interpolating between two given colors. An alternative to
	 * Processing's native lerpColor() method (which is linear).
	 * 
	 * @param col1 First color, represented as [H,S,B,A] array; each value between
	 *             0...1.
	 * @param col2 Second color, represented as [H,S,B,A] array; each value between
	 *             0...1.
	 * @param st   step: percentage between the two colors.
	 * @param out  The new interpolated color, represented by a [H,S,B,A] array.
	 * @return
	 */
	static float[] smootherStepHsb(float[] a, float[] b, float st, float[] out) {

		// Find difference in hues.
		float huea = a[0];
		float hueb = b[0];
		float delta = hueb - huea;

		// Prefer shortest distance.
		if (delta < -0.5) {
			hueb += 1.0;
		} else if (delta > 0.5) {
			huea += 1.0;
		}

		float eval = calcStep(st);

		// The two hues may be outside of 0 .. 1 range,
		// so modulate by 1.
		out[0] = (huea + eval * (hueb - huea)) % 1;
		out[1] = a[1] + eval * (b[1] - a[1]);
		out[2] = a[2] + eval * (b[2] - a[2]);
		out[3] = a[3] + eval * (b[3] - a[3]);
		return out;
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
	 * @return
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
		return PApplet.constrain(opXod / odSq, 0, 1);
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

	static float[] hsbToRgb(float[] in) {
		float[] out = new float[] { 0, 0, 0, 1 };
		return hsbToRgb(in[0], in[1], in[2], in[3], out);
	}

	static float[] hsbToRgb(float[] in, float[] out) {
		if (in.length == 3) {
			return hsbToRgb(in[0], in[1], in[2], 1, out);
		} else if (in.length == 4) {
			return hsbToRgb(in[0], in[1], in[2], in[3], out);
		}
		return out;
	}

	static float[] hsbToRgb(float hue, float sat, float bri, float alpha) {
		float[] out = new float[] { 0, 0, 0, 1 };
		return hsbToRgb(hue, sat, bri, alpha, out);
	}

	static float[] hsbToRgb(float hue, float sat, float bri, float alpha, float[] out) {
		if (sat == 0.0) {
			// 0.0 saturation is grayscale, so all values are equal.
			out[0] = out[1] = out[2] = bri;
		} else {

			// Divide color wheel into 6 sectors.
			// Scale up hue to 6, convert to sector index.
			float h = hue * 6;
			int sector = (int) h;

			// Depending on the sector, three tints will
			// be distributed among R, G, B channels.
			float tint1 = bri * (1 - sat);
			float tint2 = bri * (1 - sat * (h - sector));
			float tint3 = bri * (1 - sat * (1 + sector - h));

			switch (sector) {
				case 1 :
					out[0] = tint2;
					out[1] = bri;
					out[2] = tint1;
					break;
				case 2 :
					out[0] = tint1;
					out[1] = bri;
					out[2] = tint3;
					break;
				case 3 :
					out[0] = tint1;
					out[1] = tint2;
					out[2] = bri;
					break;
				case 4 :
					out[0] = tint3;
					out[1] = tint1;
					out[2] = bri;
					break;
				case 5 :
					out[0] = bri;
					out[1] = tint1;
					out[2] = tint2;
					break;
				default :
					out[0] = bri;
					out[1] = tint3;
					out[2] = tint1;
			}
		}

		out[3] = alpha;
		return out;
	}

	static float[] rgbToHsb(int clr) {
		return rgbToHsb(clr, new float[] { 0, 0, 0, 1 });
	}

	static float[] rgbToHsb(int clr, float[] out) {
		return rgbToHsb((clr >> 16 & 0xff) * 0.003921569f, (clr >> 8 & 0xff) * 0.003921569f,
				(clr & 0xff) * 0.003921569f, (clr >> 24 & 0xff) * 0.003921569f, out);
	}

	static float[] rgbToHsb(float[] in, float[] out) {
		if (in.length == 3) {
			return rgbToHsb(in[0], in[1], in[2], 1, out);
		} else if (in.length == 4) {
			return rgbToHsb(in[0], in[1], in[2], in[3], out);
		}
		return out;
	}

	static float[] rgbToHsb(float red, float green, float blue, float alpha, float[] out) {

		// Find highest and lowest values.
		float max = PApplet.max(red, green, blue);
		float min = PApplet.min(red, green, blue);

		// Find the difference between max and min.
		float delta = max - min;

		// Calculate hue.
		float hue = 0;
		if (delta != 0.0) {
			if (red == max) {
				hue = (green - blue) / delta;
			} else if (green == max) {
				hue = 2 + (blue - red) / delta;
			} else {
				hue = 4 + (red - green) / delta;
			}

			hue /= 6.0;
			if (hue < 0.0) {
				hue += 1.0;
			}
		}

		out[0] = hue;
		out[1] = max == 0 ? 0 : (max - min) / max;
		out[2] = max;
		out[3] = alpha;
		return out;
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
		return round(255) << 24 | round(red) << 16 | round(green) << 8 | round(blue);
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
	 * Decompose color integer (RGBA) into 4 separate components dont scale
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

}
