package peasyGradients.colourSpaces;

import peasyGradients.utilities.Functions;

/**
 * Hue, Colour, Greyness https://nicedoc.io/acterhd/hcg-color * hue.. [0..360]
 * chroma .. [0..1] grayness .. [0..1]
 * 
 * @author micycle1
 *
 */
public final class HCG {

	public static float[] rgb2hcg(float[] rgba) {
		float r = rgba[0];
		float g = rgba[1];
		float b = rgba[2];

		final float min = Functions.min(r, g, b);
		final float max = Functions.max(r, g, b);
		final float delta = max - min;
		final float c = delta * 100 / 255;
		final float _g = min / (255 - delta) * 100;

		float h = 0;
		if (delta == 0) {
			h = 0;
		} else {
			if (r == max) {
				h = (g - b) / delta;
			}
			if (g == max) {
				h = 2 + (b - r) / delta;
			}
			if (b == max) {
				h = 4 + (r - g) / delta;
			}
			h *= 60;
			if (h < 0) {
				h += 360;
			}
		}
		return new float[] { h, c, _g };
	}

	public static float[] hcg2rgb(float[] hcg) {
		float h = hcg[0];
		final float c = hcg[1];
		float _g = hcg[2];
		float r = 0, g = 0, b = 0;

		_g *= 255;

		final float _c = c * 255;

		if (c == 0) {
			r = g = b = _g;
		} else {
			if (h == 360)
				h = 0;
			if (h > 360)
				h -= 360;
			if (h < 0)
				h += 360;
			h /= 60;
			final int i = (int) h;

			final float f = h - i;
			final float p = _g * (1 - c);
			final float q = p + _c * (1 - f);
			final float t = p + _c * f;
			final float v = p + _c;
			switch (i) {
				case 0 :
					return new float[] { v, t, p };
				case 1 :
					return new float[] { q, v, p };
				case 2 :
					return new float[] { p, v, t };
				case 3 :
					return new float[] { p, q, v };
				case 4 :
					return new float[] { t, p, v };
				case 5 :
					return new float[] { v, p, q };
			}
		}
		return new float[] { r, g, b };
	}

	public static float[] interpolate(float[] a, float[] b, float step, float[] out) {
		out[0] = a[0] + step * (b[0] - a[0]);
		out[1] = a[1] + step * (b[1] - a[1]);
		out[2] = a[2] + step * (b[2] - a[2]);
		return out;
	}

}
