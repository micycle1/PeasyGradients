package micycle.peasygradients.colorSpaces;

/**
 * The HSB color system describes a color based on the three qualities of hue,
 * saturation, and value. A given color will be represented by three numbers,
 * (H,S,B). H, the value of hue, is an angle between 0 and 360 degrees, with 0
 * representing red. S is the saturation, and is between 0 and 1. Finally, B
 * measures brightness, which goes from 0 for black, increasing to a maximum of
 * 1 for the brightest colors. The HSB color system is sometimes also called
 * HSV, where the V stands for value.
 * 
 * @author micycle1
 *
 */
final class HSB implements ColorSpace {

	HSB() {
	}

	/**
	 * 
	 * @param hue 0...1
	 * @param sat
	 * @param bri
	 * @param out
	 * @return
	 */
	public double[] toRGB(double[] HSB) {
		double[] RGB = new double[3];

		if (HSB[1] == 0.0) {
			// 0.0 saturation is grayscale, so all values are equal.
			RGB[0] = RGB[1] = RGB[2] = HSB[2];
		} else {

			// Divide color wheel into 6 sectors.
			// Scale up hue to 6, convert to sector index.
			double h = HSB[0] * 6;
			int sector = (int) h;

			// Depending on the sector, three tints will
			// be distributed among R, G, B channels.
			double tint1 = HSB[2] * (1 - HSB[1]);
			double tint2 = HSB[2] * (1 - HSB[1] * (h - sector));
			double tint3 = HSB[2] * (1 - HSB[1] * (1 + sector - h));

			switch (sector) {
				case 1 :
					RGB[0] = tint2;
					RGB[1] = HSB[2];
					RGB[2] = tint1;
					break;
				case 2 :
					RGB[0] = tint1;
					RGB[1] = HSB[2];
					RGB[2] = tint3;
					break;
				case 3 :
					RGB[0] = tint1;
					RGB[1] = tint2;
					RGB[2] = HSB[2];
					break;
				case 4 :
					RGB[0] = tint3;
					RGB[1] = tint1;
					RGB[2] = HSB[2];
					break;
				case 5 :
					RGB[0] = HSB[2];
					RGB[1] = tint1;
					RGB[2] = tint2;
					break;
				default :
					RGB[0] = HSB[2];
					RGB[1] = tint3;
					RGB[2] = tint1;
			}
		}
		return RGB;
	}

//	@Override
//	public double[] fromRGB(double[] RGB) {
//		double[] HSB = new double[3];
//
//		// Find highest and lowest values.
//		double max = Functions.max(RGB[0], RGB[1], RGB[2]);
//		double min = Functions.min(RGB[0], RGB[1], RGB[2]);
//
//		// Find the difference between max and min.
//		double delta = max - min;
//
//		// Calculate hue.
//		double hue = 0;
//		if (delta != 0.0) {
//			if (RGB[0] == max) {
//				hue = (RGB[1] - RGB[2]) / delta;
//			} else if (RGB[1] == max) {
//				hue = 2 + (RGB[2] - RGB[0]) / delta;
//			} else {
//				hue = 4 + (RGB[0] - RGB[1]) / delta;
//			}
//
//			hue /= 6.0;
//			if (hue < 0.0) {
//				hue += 1.0;
//			}
//		}
//
//		HSB[0] = hue;
//		HSB[1] = max == 0 ? 0 : (max - min) / max;
//		HSB[2] = max;
//		return HSB;
//	}

	/**
	 * http://lolengine.net/blog/2013/01/13/fast-rgb-to-hsv Possibly faster in Java,
	 * haven't tested.
	 */
	@Override
	public double[] fromRGB(double[] RGB) {
		double[] HSB = new double[3];

		double r = RGB[0];
		double g = RGB[1];
		double b = RGB[2];

		double K = 0;

		if (g < b) {

			// swap variables
			g = g + b;
			b = g - b;
			g = g - b;

			K = -1;
		}

		if (r < g) {

			// swap variables
			r = r + g;
			g = r - g;
			r = r - g;

			K = -2d / 6d - K;
		}

		final double chroma = r - (g > b ? b : g); // min(g, b)

		HSB[0] = Math.abs(K + (g - b) / (6 * chroma + 1e-20d));
		HSB[1] = chroma / (r + 1e-20d);
		HSB[2] = r;

		return HSB;
	}

	/**
	 * Returns a color by interpolating between two given colors. An alternative to
	 * Processing's native lerpcolor() method (which is linear). The shortest path
	 * between hues is used.
	 * 
	 * @param col1 First color, represented as [H,S,B] array; each value between
	 *             0...1.
	 * @param col2 Second color, represented as [H,S,B] array; each value between
	 *             0...1.
	 * @param st   step: percentage between the two colors.
	 * @param out  The new interpolated color, represented by a [H,S,B] array.
	 * @return
	 */
	public double[] interpolateLinear(double[] a, double[] b, double st, double[] out) {

		// Find difference in hues.
		double huea = a[0];
		double hueb = b[0];
		double delta = hueb - huea;

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
		return out;
	}
}
