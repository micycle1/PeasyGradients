package micycle.peasygradients.colorspace;

import micycle.peasygradients.utilities.FastPow;
import micycle.peasygradients.utilities.Functions;
import net.jafama.FastMath;

/**
 * CIE XYZ (aka CIE 1931) (aka tristimulus values) is a device-invariant
 * representation of color. It serves as a standard reference against which many
 * other color spaces are defined.
 *
 * @author Michael Carleton
 *
 */
public final class XYZ implements ColorSpace {

	private static final double constA = 1 / 2.4d; // 1/ sRGB gamma

	public XYZ() {
	}

	/**
	 * 
	 * @param rgb [R,G,B] where values are 0...1.0
	 * @return
	 */
	@Override
	public double[] fromRGB(double[] rgb) {
		return rgb2xyz(rgb);
	}

	/**
	 * 
	 * @param xyz [X,Y,Z]
	 * @return [R,G,B] where values are 0...1.0
	 */
	@Override
	public double[] toRGB(final double[] xyz) {

		double r, g, b;

		double x = xyz[0] * 0.01;
		double y = xyz[1] * 0.01;
		double z = xyz[2] * 0.01;

		r = x * 3.2406 + y * -1.5372 + z * -0.4986;
		g = x * -0.9689 + y * 1.8758 + z * 0.0415;
		b = x * 0.0557 + y * -0.2040 + z * 1.0570;

		if (r > 0.0031308) {
			r = 1.055 * FastMath.pow(r, constA) - 0.055;
		} else {
			r *= 12.92;
		}
		if (g > 0.0031308) {
			g = 1.055 * FastMath.pow(g, constA) - 0.055;
		} else {
			g *= 12.92;
		}
		if (b > 0.0031308) {
			b = 1.055 * FastMath.pow(b, constA) - 0.055;
		} else {
			b *= 12.92;
		}

		return new double[] { r, g, b };
	}

	/**
	 * Fast, negligible visual effect inaccuracy.
	 * 
	 * @param xyz [X,Y,Z]
	 * @return [R,G,B] where values are 0...1.0
	 * @deprecated
	 */
	@Deprecated
	public static double[] xyz2rgbQuick(final double[] xyz) {

		double r, g, b;

		xyz[0] *= 0.01;
		xyz[1] *= 0.01;
		xyz[2] *= 0.01;

		r = xyz[0] * 3.2406 + xyz[1] * -1.5372 + xyz[2] * -0.4986;
		g = xyz[0] * -0.9689 + xyz[1] * 1.8758 + xyz[2] * 0.0415;
		b = xyz[0] * 0.0557 + xyz[1] * -0.2040 + xyz[2] * 1.0570;

		if (r > 0.0031308) {
			r = 1.055 * FastPow.fastPow(r, constA) - 0.055;
		} else {
			r *= 12.92;
		}
		if (g > 0.0031308) {
			g = 1.055 * FastPow.fastPow(g, constA) - 0.055;
		} else {
			g *= 12.92;
		}
		if (b > 0.0031308) {
			b = 1.055 * FastPow.fastPow(b, constA) - 0.055;
		} else {
			b *= 12.92;
		}
		return new double[] { r, g, b };
	}

	/**
	 * Very fast, but visible inaccuracies
	 * 
	 * @param xyz [X,Y,Z]
	 * @return [R,G,B] where values are 0...1.0
	 * @deprecated
	 */
	@Deprecated
	public static double[] xyz2rgbVeryQuick(final double[] xyz) {

		double r, g, b;

		xyz[0] *= 0.01;
		xyz[1] *= 0.01;
		xyz[2] *= 0.01;

		r = xyz[0] * 3.2406 + xyz[1] * -1.5372 + xyz[2] * -0.4986;
		g = xyz[0] * -0.9689 + xyz[1] * 1.8758 + xyz[2] * 0.0415;
		b = xyz[0] * 0.0557 + xyz[1] * -0.2040 + xyz[2] * 1.0570;

		if (r > 0.0031308) {
			r = 1.055 * Functions.veryFastPow(r, constA) - 0.055;
		} else {
			r *= 12.92;
		}
		if (g > 0.0031308) {
			g = 1.055 * Functions.veryFastPow(g, constA) - 0.055;
		} else {
			g *= 12.92;
		}
		if (b > 0.0031308) {
			b = 1.055 * Functions.veryFastPow(b, constA) - 0.055;
		} else {
			b *= 12.92;
		}

		return new double[] { r, g, b };
	}

	/**
	 * Static for use by other classes
	 * 
	 * @param rgb [R,G,B] where values are 0...1.0
	 * @return
	 */
	public static double[] rgb2xyz(double[] rgb) {

		double x = rgb[0];
		double y = rgb[1];
		double z = rgb[2];

		// convert sRGB (gamma-adjusted) to linear RGB (linear space for XYZ)
		if (x > 0.04045) {
			x = FastMath.pow((x + 0.055) / 1.055, 2.4);
		} else {
			x /= 12.92;
		}
		if (y > 0.04045) {
			y = FastMath.pow((y + 0.055) / 1.055, 2.4);
		} else {
			y /= 12.92;
		}
		if (z > 0.04045) {
			z = FastMath.pow((z + 0.055) / 1.055, 2.4);
		} else {
			z /= 12.92;
		}

		/**
		 * Normalise values to fall between 0 and 100 (roughly speaking – values greater
		 * than 100 are valid in certain cases)
		 */
		x *= 100;
		y *= 100;
		z *= 100;

		return new double[] { x * 0.41239079926595 + y * 0.35758433938387 + z * 0.18048078840183,
				x * 0.21263900587151 + y * 0.71516867876775 + z * 0.072192315360733,
				x * 0.019330818715591 + y * 0.11919477979462 + z * 0.95053215224966 };

	}

	/**
	 * Static for use by other colorspace classes
	 * 
	 * @param xyz
	 * @return
	 */
	static double[] xyz2rgb(double[] xyz) {

		/**
		 * Unnormalise values from XYZ100 to XYZ1
		 */
		double x = xyz[0] / 100;
		double y = xyz[1] / 100;
		double z = xyz[2] / 100;

		double r, g, b;

		r = x * 3.2406 + y * -1.5372 + z * -0.4986;
		g = x * -0.9689 + y * 1.8758 + z * 0.0415;
		b = x * 0.0557 + y * -0.2040 + z * 1.0570;

		// convert linear RGB back to (gamma-adjusted) sRGB
		if (r > 0.0031308) {
			r = 1.055 * FastMath.powQuick(r, constA) - 0.055; // powQuick(n, 1/2.4) max error is 1E-5 over range of n = [0...1]
		} else {
			r *= 12.92;
		}
		if (g > 0.0031308) {
			g = 1.055 * FastMath.powQuick(g, constA) - 0.055;
		} else {
			g *= 12.92;
		}
		if (b > 0.0031308) {
			b = 1.055 * FastMath.powQuick(b, constA) - 0.055;
		} else {
			b *= 12.92;
		}

		// TODO constrain here?

		return new double[] { r, g, b };
	}
}
