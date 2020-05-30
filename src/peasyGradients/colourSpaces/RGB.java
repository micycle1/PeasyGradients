package peasyGradients.colourSpaces;

import processing.core.PApplet;

public final class RGB {
	
	public static float[] rgbToHsb(int clr) {
		return rgbToHsb(clr, new float[] { 0, 0, 0, 1 });
	}

	static float[] rgbToHsb(int clr, float[] out) {
		return rgbToHsb((clr >> 16 & 0xff) * 0.003921569f, (clr >> 8 & 0xff) * 0.003921569f,
				(clr & 0xff) * 0.003921569f, (clr >> 24 & 0xff) * 0.003921569f, out);
	}

	private static float[] rgbToHsb(float[] in, float[] out) {
		if (in.length == 3) {
			return rgbToHsb(in[0], in[1], in[2], 1, out);
		} else if (in.length == 4) {
			return rgbToHsb(in[0], in[1], in[2], in[3], out);
		}
		return out;
	}

	private static float[] rgbToHsb(float red, float green, float blue, float alpha, float[] out) {

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
	public static float[] interpolate(float[] col1, float[] col2, float st, float[] out) {
		out[0] = col1[0] + st * (col2[0] - col1[0]);
		out[1] = col1[1] + st * (col2[1] - col1[1]);
		out[2] = col1[2] + st * (col2[2] - col1[2]);
		out[3] = col1[3] + st * (col2[3] - col1[3]);
		return out;
	}

	/**
	 * An n-color alternative to the
	 * {@link #interpolate(float[], float[], float, float[]) two color} method.
	 * 
	 * @param arr array[][] of colors, where each color[] is a [R,G,B,A] array with
	 *            each value between 0...1.
	 * @param st  step, global
	 * @param out
	 * @return
	 * @deprecated
	 */
	private static float[] interpolate(float[][] arr, float st, float[] out) {
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
		out[0] = arr[i][0] + st * (arr[i + 1][0] - arr[i][0]);
		out[1] = arr[i][1] + st * (arr[i + 1][1] - arr[i][1]);
		out[2] = arr[i][2] + st * (arr[i + 1][2] - arr[i][2]);
		out[3] = arr[i][3] + st * (arr[i + 1][3] - arr[i][3]);
		return out;
	}

}
