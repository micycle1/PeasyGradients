package micycle.peasygradients.colorspace;

/**
 * Fast XYZ, suitable to be used standalone for gradient color interpolation.
 * Unsuitable to be used to convert from into other colorspaces, as underlying
 * XYZ_FAST values are not divided by certain constants.
 * 
 * @author Michael Carleton
 * @deprecated not needed since the LUT optimisation.
 */
@Deprecated
final class XYZ_FAST {

	/**
	 * This method is not faster, since only called once during colorstop
	 * instantiation.
	 * 
	 * @param sRGB [R,G,B] where values are 0...1.0
	 * @return XYZ representation (where values have same relative difference as
	 *         real XYZ values)
	 */
	static double[] rgb2xyz(double[] rgb) {

		double x = rgb[0];
		double y = rgb[1];
		double z = rgb[2];

		if (x > 0.04045) {
			x = Math.pow((x + 0.055) / 1.055, 2.4);
		}
		if (y > 0.04045) {
			y = Math.pow((y + 0.055) / 1.055, 2.4);
		}
		if (z > 0.04045) {
			z = Math.pow((z + 0.055) / 1.055, 2.4);
		}

		return new double[] { x * 0.4124 + y * 0.3576 + z * 0.1805, x * 0.2126 + y * 0.7152 + z * 0.0722,
				x * 0.0193 + y * 0.1192 + z * 0.9505 };
	}

	/**
	 * @param XYZ representation [X,Y,Z]
	 * @return [R,G,B] where values are 0...1.0
	 */
	static double[] xyz2rgbVeryQuick(final double[] xyz) {

		double r, g, b;

		r = xyz[0] * 3.2406 + xyz[1] * -1.5372 + xyz[2] * -0.4986;
		g = xyz[0] * -0.9689 + xyz[1] * 1.8758 + xyz[2] * 0.0415;
		b = xyz[0] * 0.0557 + xyz[1] * -0.2040 + xyz[2] * 1.0570;

		if (r > 0.0031308) {
			r = linear_to_srgb(r);
		}
		if (g > 0.0031308) {
			g = linear_to_srgb(g);
		}
		if (b > 0.0031308) {
			b = linear_to_srgb(b);
		}

		return new double[] { r, g, b };
	}

	/**
	 * The following polynomial approximations have been generated with sollya, and
	 * have a worst case relative error of 0.0144%. Source:
	 * https://stackoverflow.com/a/39652091/9808792
	 * 
	 * @param x
	 * @return
	 */
	private static double linear_to_srgb(double x) {
//	    if (x <= 0.0031308) return x * 12.92;

		// Piecewise polynomial approximation (divided by x^3)
		// of 1.055 * x^(1/2.4) - 0.055.
		if (x <= 0.0523) {
			return poly7(x, -6681.49576364495442248881, 1224.97114922729451791383, -100.23413743425112443219, 6.60361150127077944916,
					0.06114808961060447245, -0.00022244138470139442, 0.00000041231840827815, -0.00000000035133685895) / (x * x * x);
		}

		return poly7(x, -0.18730034115395793881, 0.64677431008037400417, -0.99032868647877825286, 1.20939072663263713636,
				0.33433459165487383613, -0.01345095746411287783, 0.00044351684288719036, -0.00000664263587520855) / (x * x * x);
	}

	private static double poly7(double x, double a, double b, double c, double d, double e, double f, double g, double h) {
		double ab, cd, ef, gh, abcd, efgh, x2, x4;
		x2 = x * x;
		x4 = x2 * x2;
		ab = a * x + b;
		cd = c * x + d;
		ef = e * x + f;
		gh = g * x + h;
		abcd = ab * x2 + cd;
		efgh = ef * x2 + gh;
		return abcd * x4 + efgh;
	}
}
