package peasyGradients.colorSpaces;

import peasyGradients.utilities.Functions;

/**
 * Red, yellow, blue RYB color Compositing:
 * http://nishitalab.org/user/UEI/publication/Sugita_IWAIT2015.pdf
 * http://nishitalab.org/user/UEI/publication/Sugita_SIG2015.pdf
 * // TODO check working properly
 * @author micycle1
 *
 */
final class RYB implements ColorSpace {
	
	RYB() {
	}

	/**
	 * 
	 * @param RGBA vals 0...1
	 * @return
	 */
	public double[] fromRGB(double[] RGB) {
		double R = RGB[0];
		double G = RGB[1];
		double B = RGB[2];

		double r, y, b;

		// subtract whiteness
		final double w = Functions.min(R, G, B); // calc white component
		R -= w;
		G -= w;
		B -= w;

		r = R - (R > G ? G : R); // min
		y = (G + (R > G ? G : R)) / 2;
		b = (B + G - (R > G ? G : R)) / 2;

		// normalise
		double n = Functions.max(r, y, b) / Functions.max(R, G, B);
		if (!Double.isNaN(n)) {
			r /= n;
			y /= n;
			b /= n;
		}

		// add blackness
		final double black = Functions.min(1 - RGB[0], 1 - RGB[1], 1 - RGB[2]);
		r += black;
		y += black;
		b += black;

		return new double[] { r, y, b};
	}

	public double[] toRGB(double[] ryb) {
		double r = ryb[0];
		double y = ryb[1];
		double b = ryb[2];

		double R, G, B;

		// subtract whiteness
		final double black = Functions.min(r, y, b); // calc white component
		r -= black;
		y -= black;
		b -= black;

		R = r + y - (y > b ? b : y);
		G = y + 2 * (y > b ? b : y);
		B = 2 * (b - (y > b ? b : y));

		// normalise
		double n = Functions.max(R, G, B) / Functions.max(r, y, b);
		if (!Double.isNaN(n)) {
			R /= n;
			G /= n;
			B /= n;
		}

		// add blackness
		final double w = Functions.min(1 - ryb[0], 1 - ryb[1], 1 - ryb[2]);
		R += w;
		G += w;
		B += w;

		return new double[] { R, G, B};
	}

}
