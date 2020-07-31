package peasyGradients.colourSpaces;

import net.jafama.FastMath;
import peasyGradients.utilities.FastPow;

/**
 * https://observablehq.com/@jrus/jzazbz
 * https://stackoverflow.com/questions/49464451/jzazbz-java-implementation-precision
 * https://www.osapublishing.org/DirectPDFAccess/0E0CA420-C7CB-756F-F502C2277BCB8255_368272/oe-25-13-15131.pdf
 * https://github.com/quag/JzAzBz/blob/master/glsl/jchz.glsl
 * http://im.snibgo.com/jzazbz.htm
 * 
 * <p>
 * JAB (JzAzBz) is a a colour space designed for perceptul uniformity in high
 * dynamic range (HDR) and wide colour gamut (WCG) applications. Conceptually it
 * is similar to CIE L*a*b*, but has claimed improvements:
 * 
 * Perceptual colour difference is predicted by Euclidean distance. Perceptually
 * uniform: MacAdam ellipses of just-noticable-difference (JND) are more
 * circular, and closer to the same sizes. Hue linearity: changing saturation or
 * lightness has less shift in hue.
 * 
 * Removed *\10000 because doesn't affect interpolation  
 * 
 * @author micycle1
 *
 */
final class JAB implements ColourSpace {

	private static final double b = 1.15;
	private static final double g = 0.66;
	private static final double c1 = 3424 / Math.pow(2, 12);
	private static final double c2 = 2413 / Math.pow(2, 7);
	private static final double c3 = 2392 / Math.pow(2, 7);
	private static final double n = 2610 / Math.pow(2, 14);
	private static final double nInverse = 1 / n;
	private static final double p = 1.7 * (2523 / Math.pow(2, 5));
	private static final double pInverse = 1 / p;
	private static final double d = -0.56;
	private static final double d0 = 1.6295499532821567 * Math.pow(10, -11);
	
	JAB() {
	}

	public double[] fromRGB(double[] rgb) {
		return xyz2jab(XYZ.rgb2xyz(rgb));
	}

	public double[] toRGB(double[] jab) {
		return XYZ.xyz2rgb(jab2xyz(jab));
	}

	public static double[] jab2rgbQuick(double[] jab) {
		return XYZ.xyz2rgbQuick(jab2xyzQuick(jab));
	}

	private static double[] xyz2jab(double[] xyz) {
		double[] jab = new double[3];

		double[] XYZp = new double[3];
		XYZp[0] = b * xyz[0] - ((b - 1) * xyz[2]);
		XYZp[1] = g * xyz[1] - ((g - 1) * xyz[0]);
		XYZp[2] = xyz[2];

		double[] LMS = new double[3];
		LMS[0] = 0.41478972 * XYZp[0] + 0.579999 * XYZp[1] + 0.0146480 * XYZp[2];
		LMS[1] = -0.2015100 * XYZp[0] + 1.120649 * XYZp[1] + 0.0531008 * XYZp[2];
		LMS[2] = -0.0166008 * XYZp[0] + 0.264800 * XYZp[1] + 0.6684799 * XYZp[2];

		double[] LMSp = new double[3];
		for (int i = 0; i < 3; i++) {
			LMSp[i] = Math.pow(
					(c1 + (c2 * FastMath.pow(LMS[i], n))) / (1 + (c3 * FastMath.pow(LMS[i], n))), p);
		}

		double[] Iab = new double[3];
		Iab[0] = 0.5 * LMSp[0] + 0.5 * LMSp[1];
		Iab[1] = 3.524000 * LMSp[0] - 4.066708 * LMSp[1] + 0.542708 * LMSp[2];
		Iab[2] = 0.199076 * LMSp[0] + 1.096799 * LMSp[1] - 1.295875 * LMSp[2];

		jab[0] = ((1 + d) * Iab[0] / (1 + d * Iab[0])) - d0;
		jab[1] = Iab[1];
		jab[2] = Iab[2];

		return jab;
	}

	private static double[] jab2xyz(double[] jab) {
		double[] xyz = new double[3];

		double iab0 = (jab[0] + d0) / (1 + d - d * (jab[0] + d0));

		double[] LMSp = new double[3];
		LMSp[0] = iab0 + 0.138605043271539 * jab[1] + 0.058047316156119 * jab[2];
		LMSp[1] = iab0 - 0.138605043271539 * jab[1] - 0.058047316156119 * jab[2];
		LMSp[2] = iab0 - 0.096019242026319 * jab[1] - 0.811891896056039 * jab[2];

		double[] LMS = new double[3];
		LMS[0] = FastMath
				.pow((c1 - FastMath.pow(LMSp[0], pInverse)) / ((c3 * FastMath.pow(LMSp[0], pInverse)) - c2), nInverse);
		LMS[1] = FastMath
				.pow((c1 - FastMath.pow(LMSp[1], pInverse)) / ((c3 * FastMath.pow(LMSp[1], pInverse)) - c2), nInverse);
		LMS[2] = FastMath
				.pow((c1 - FastMath.pow(LMSp[2], pInverse)) / ((c3 * FastMath.pow(LMSp[2], pInverse)) - c2), nInverse);

		double[] XYZp = new double[3];
		XYZp[0] = 1.924226435787607 * LMS[0] - 1.004792312595365 * LMS[1] + 0.037651404030618 * LMS[2];
		XYZp[1] = 0.350316762094999 * LMS[0] + 0.726481193931655 * LMS[1] - 0.065384422948085 * LMS[2];
		XYZp[2] = -0.090982810982848 * LMS[0] - 0.312728290523074 * LMS[1] + 1.522766561305260 * LMS[2];

		xyz[0] = (XYZp[0] + (b - 1) * XYZp[2]) / b;
		xyz[1] = (XYZp[1] + (g - 1) * xyz[0]) / g;
		xyz[2] = XYZp[2];

		return xyz;
	}

	/**
	 * TODO
	 * https://stackoverflow.com/questions/6475373/optimizations-for-pow-with-const-non-integer-exponent
	 * @param jab
	 * @return
	 */
	private static double[] jab2xyzQuick(double[] jab) {
		double[] xyz = new double[3];

		double iab0 = (jab[0] + d0) / (1 + d - d * (jab[0] + d0));

		double[] LMSp = new double[3];
		LMSp[0] = iab0 + 0.138605043271539 * jab[1] + 0.058047316156119 * jab[2];
		LMSp[1] = iab0 - 0.138605043271539 * jab[1] - 0.058047316156119 * jab[2];
		LMSp[2] = iab0 - 0.096019242026319 * jab[1] - 0.811891896056039 * jab[2];

		double[] LMS = new double[3];
		// nested fastpow calls require larger LUT to retain accuracy
		LMS[0] = FastPow.fastPow((c1 - FastPow.fastPow(LMSp[0], pInverse)) / ((c3 * FastPow.fastPow(LMSp[0], pInverse)) - c2), nInverse);
		LMS[1] = FastPow.fastPow((c1 - FastPow.fastPow(LMSp[1], pInverse)) / ((c3 * FastPow.fastPow(LMSp[1], pInverse)) - c2), nInverse);
		LMS[2] = FastPow.fastPow((c1 - FastPow.fastPow(LMSp[2], pInverse)) / ((c3 * FastPow.fastPow(LMSp[2], pInverse)) - c2), nInverse);

		double[] XYZp = new double[3];
		XYZp[0] = 1.924226435787607 * LMS[0] - 1.004792312595365 * LMS[1] + 0.037651404030618 * LMS[2];
		XYZp[1] = 0.350316762094999 * LMS[0] + 0.726481193931655 * LMS[1] - 0.065384422948085 * LMS[2];
		XYZp[2] = -0.090982810982848 * LMS[0] - 0.312728290523074 * LMS[1] + 1.522766561305260 * LMS[2];

		xyz[0] = (XYZp[0] + (b - 1) * XYZp[2]) / b;
		xyz[1] = (XYZp[1] + (g - 1) * xyz[0]) / g;
		xyz[2] = XYZp[2];

		return xyz;
	}

}
