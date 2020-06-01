package peasyGradients.colourSpaces;

import peasyGradients.utilities.Functions;

/**
 * Red, yellow, blue RYB Color Compositing:
 * http://nishitalab.org/user/UEI/publication/Sugita_IWAIT2015.pdf
 * http://nishitalab.org/user/UEI/publication/Sugita_SIG2015.pdf
 * // TODO check working properly
 * @author micycle1
 *
 */
public final class RYB {

	/**
	 * 
	 * @param RGBA vals 0...1
	 * @return
	 */
	public static float[] rgb2ryb(float[] RGBA) {
		float R = RGBA[0];
		float G = RGBA[1];
		float B = RGBA[2];

		float r, y, b;

		// subtract whiteness
		final float w = Functions.min(R, G, B); // calc white component
		R -= w;
		G -= w;
		B -= w;

		r = R - (R > G ? G : R); // min
		y = (G + (R > G ? G : R)) / 2;
		b = (B + G - (R > G ? G : R)) / 2;

		// normalise
		float n = Functions.max(r, y, b) / Functions.max(R, G, B);
		if (!Float.isNaN(n)) {
			r /= n;
			y /= n;
			b /= n;
		}

		// add blackness
		final float black = Functions.min(1 - RGBA[0], 1 - RGBA[1], 1 - RGBA[2]);
		r += black;
		y += black;
		b += black;

		return new float[] { r, y, b, RGBA[3] };
	}

	public static float[] ryb2rgb(float[] ryba) {
		float r = ryba[0];
		float y = ryba[1];
		float b = ryba[2];

		float R, G, B;

		// subtract whiteness
		final float black = Functions.min(r, y, b); // calc white component
		r -= black;
		y -= black;
		b -= black;

		R = r + y - (y > b ? b : y);
		G = y + 2 * (y > b ? b : y);
		B = 2 * (b - (y > b ? b : y));

		// normalise
		float n = Functions.max(R, G, B) / Functions.max(r, y, b);
		if (!Float.isNaN(n)) {
			R /= n;
			G /= n;
			B /= n;
		}

		// add blackness
		final float w = Functions.min(1 - ryba[0], 1 - ryba[1], 1 - ryba[2]);
		R += w;
		G += w;
		B += w;

		return new float[] { R, G, B, ryba[3] };
	}

}
