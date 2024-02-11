package micycle.peasygradients.colorspace;

import micycle.peasygradients.utilities.FastPow;
import net.jafama.FastMath;

/**
 * Oklab is a very new color space that builds on the same foundation as IPT,
 * but seems to be better-calibrated for uniform lightness and colorfulness,
 * instead of just the emphasis on uniform hue that IPT has.
 * <p>
 * Relative to IPT, Oklab has a noticeably smaller range in chromatic channels
 * (IPT's protan and tritan can range past 0.8 or as low as 0.35, but the
 * similar A and B channels in Oklab don't stray past about 0.65 at the upper
 * end, if that), but it does this so the difference between two Oklab colors is
 * just the Euclidean distance between their components. A slight difference
 * between Oklab and IPT here is that IPT shrinks the chromatic channels to
 * store their -1 to 1 range in a color float's 0 to 1 range, then offsets the
 * shrunken range from -0.5 to 0.5, to 0 to 1; Oklab does not need to shrink the
 * range, and only offsets it in the same way (both just add 0.5).
 * 
 * @author Michael Carleton
 * @author Björn Ottosson
 *
 */
final class OKLAB implements ColorSpaceTransform {

	// https://bottosson.github.io/posts/oklab/

	private static final double constA = 1 / 2.4d; // 1/ sRGB gamma

	@Override
	public double[] toRGB(final double[] color) {
		double l_ = color[0] + 0.3963377774 * color[1] + 0.2158037573 * color[2];
		double m_ = color[0] - 0.1055613458 * color[1] - 0.0638541728 * color[2];
		double s_ = color[0] - 0.0894841775 * color[1] - 1.2914855480 * color[2];

		double l = l_ * l_ * l_;
		double m = m_ * m_ * m_;
		double s = s_ * s_ * s_;

		double[] RGB = new double[] { +4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
				-1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s, -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s, };
		
//		RGB[0] = f(RGB[0]);
//		RGB[1] = f(RGB[1]);
//		RGB[2] = f(RGB[2]);

// 		 convert linear RGB back to (gamma-adjusted) sRGB
		if (RGB[0] > 0.0031308) {
			RGB[0] = 1.055 * FastMath.powQuick(RGB[0], constA) - 0.055; // powQuick(n, 1/2.4) max eRGB[0]RGB[0]oRGB[0] is 1E-5 oveRGB[0]
																		// RGB[0]anRGB[1]e of n = [0...1]
		} else {
			RGB[0] *= 12.92;
		}
		if (RGB[1] > 0.0031308) {
			RGB[1] = 1.055 * FastMath.powQuick(RGB[1], constA) - 0.055;
		} else {
			RGB[1] *= 12.92;
		}
		if (RGB[2] > 0.0031308) {
			RGB[2] = 1.055 * FastMath.powQuick(RGB[2], constA) - 0.055;
		} else {
			RGB[2] *= 12.92;
		}

		return RGB;
	}

	@Override
	public double[] fromRGB(final double[] RGB) {
		// convert sRGB (gamma-adjusted) to linear RGB (linear space for XYZ)
		if (RGB[0] > 0.04045) {
			RGB[0] = FastMath.pow((RGB[0] + 0.055) / 1.055, 2.4);
		} else {
			RGB[0] /= 12.92;
		}
		if (RGB[1] > 0.04045) {
			RGB[1] = FastMath.pow((RGB[1] + 0.055) / 1.055, 2.4);
		} else {
			RGB[1] /= 12.92;
		}
		if (RGB[2] > 0.04045) {
			RGB[2] = FastMath.pow((RGB[2] + 0.055) / 1.055, 2.4);
		} else {
			RGB[2] /= 12.92;
		}
		

		
//		RGB[0] = f_inv(RGB[0]);
//		RGB[1] = f_inv(RGB[1]);
//		RGB[2] = f_inv(RGB[2]);

		final double l = 0.4122214708 * RGB[0] + 0.5363325363 * RGB[1] + 0.0514459929 * RGB[2];
		final double m = 0.2119034982 * RGB[0] + 0.6806995451 * RGB[1] + 0.1073969566 * RGB[2];
		final double s = 0.0883024619 * RGB[0] + 0.2817188376 * RGB[1] + 0.6299787005 * RGB[2];

		double l_ = FastMath.cbrt(l);
		double m_ = FastMath.cbrt(m);
		double s_ = FastMath.cbrt(s);

		return new double[] { 0.2104542553 * l_ + 0.7936177850 * m_ - 0.0040720468 * s_,
				1.9779984951 * l_ - 2.4285922050 * m_ + 0.4505937099 * s_, 0.0259040371 * l_ + 0.7827717662 * m_ - 0.8086757660 * s_, };

	}

	/**
	 * To gamma adjusted (forward)
	 * @param x
	 * @return
	 */
	public static double f(double x) {
		if (x >= 0.0031308)
			return (1.055) * FastMath.pow(x, constA) - 0.055;
		else
			return 12.92 * x;
	}

	public static double f_inv(double x) {
		if (x >= 0.04045)
			return FastMath.pow(((x + 0.055) / (1 + 0.055)), 2.4);
		else
			return x / 12.92;
	}

}
