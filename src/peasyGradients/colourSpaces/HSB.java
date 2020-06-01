package peasyGradients.colourSpaces;

/**
 * Same as HSV
 * @author micycle1
 *
 */
public final class HSB {

	public static float[] hsbToRgb(float[] in) {
		float[] out = new float[] { 0, 0, 0, 1 };
		return hsbToRgb(in[0], in[1], in[2], in[3], out);
	}

	/**
	 * 
	 * @param hue 0...1
	 * @param sat
	 * @param bri
	 * @param alpha
	 * @return
	 */
	public static float[] hsbToRgb(float hue, float sat, float bri, float alpha) {
		float[] out = new float[] { 0, 0, 0, 1 };
		return hsbToRgb(hue, sat, bri, alpha, out);
	}

	private static float[] hsbToRgb(float hue, float sat, float bri, float alpha, float[] out) {
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

	/**
	 * Returns a color by interpolating between two given colors. An alternative to
	 * Processing's native lerpColor() method (which is linear). The shortest path
	 * between hues is used.
	 * 
	 * @param col1 First color, represented as [H,S,B,A] array; each value between
	 *             0...1.
	 * @param col2 Second color, represented as [H,S,B,A] array; each value between
	 *             0...1.
	 * @param st   step: percentage between the two colors.
	 * @param out  The new interpolated color, represented by a [H,S,B,A] array.
	 * @return
	 */
	public static float[] interpolateShort(float[] a, float[] b, float st, float[] out) {

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

		// The two hues may be outside of 0 .. 1 range,
		// so modulate by 1.
		out[0] = (huea + st * (hueb - huea)) % 1;
		out[1] = a[1] + st * (b[1] - a[1]);
		out[2] = a[2] + st * (b[2] - a[2]);
		out[3] = a[3] + st * (b[3] - a[3]);
		return out;
	}

	/**
	 * Like {@link #interpolateShort(float[], float[], float, float[])
	 * interpolateShort()}, but does not use the shortest path between hues.
	 * 
	 * @param a
	 * @param b
	 * @param st
	 * @param out
	 * @return
	 */
	public static float[] interpolateLong(float[] a, float[] b, float st, float[] out) {
		// The two hues may be outside of 0 .. 1 range,
		// so modulate by 1.
		out[0] = (a[0] + st * (b[0] - a[0])) % 1;
		out[1] = a[1] + st * (b[1] - a[1]);
		out[2] = a[2] + st * (b[2] - a[2]);
		out[3] = a[3] + st * (b[3] - a[3]);
		return out;
	}

}
