package peasyGradients.colourSpaces;

import peasyGradients.utilities.Functions;

/**
 * Hue, Colour, Greyness
 * 
 * https://github.com/d3/d3-hcg/blob/master/src/hcg.js
 * 
 * @author micycle1
 *
 */
public final class HCG {

	/**
	 * IN RGA 0...255
	 * 
	 * @param rgba
	 * @return
	 */
	public static float[] rgb2hcg(float[] rgba) {
		float r = rgba[0] / 255;
		float g = rgba[1] / 255;
		float b = rgba[2] / 255;

		final float min = Functions.min(r, g, b);
		final float max = Functions.max(r, g, b);
		final float d = max - min;
		float h = Float.NaN;
		float gr = min / (1 - d);

		if (d != 0) {
			if (r == max) {
				h = (g - b) / d + (g < b ? 1 : 0) * 6;
			} else if (g == max) {
				h = (b - r) / d + 2;
			} else {
				h = (r - g) / d + 4;
			}
			h *= 60;
		}
		return new float[] { h, d, gr };
	}

	public static float[] hcg2rgb(float[] hcg) {

		float h = Float.isNaN(hcg[0]) ? 0 : hcg[0] % 360 + (hcg[0] < 0 ? 1 : 0) * 360;
		final float c = hcg[1];
		float g = Float.isNaN(hcg[2]) ? 0 : hcg[2];

		float x = c * (1 - Math.abs((h / 60) % 2 - 1));
		float m = g * (1 - c);

		// @formatter:off
		return h < 60 ? hcg2rgb(c, x, 0, m)
				: h < 120 ? hcg2rgb(x, c, 0, m)
				: h < 180 ? hcg2rgb(0, c, x, m)
				: h < 240 ? hcg2rgb(0, x, c, m)
				: h < 300 ? hcg2rgb(x, 0, c, m)
				: hcg2rgb(c, 0, x, m);
		// @formatter:on
	}

	/**
	 * out rgb 0...255
	 * 
	 * @param r
	 * @param g
	 * @param b
	 * @param m
	 * @return
	 */
	private static float[] hcg2rgb(float r, float g, float b, float m) {
		return new float[] { (r + m) * 255, (g + m) * 255, (b + m) * 255, 1 }; // alpha = 1 TODO
	}

	public static float[] interpolate(float[] a, float[] b, float step, float[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}

}
