package micycle.peasygradients.colorspace;

import net.jafama.FastMath;

/**
 * Dolby ITP, aka ICtCp (not be confused with the IPT color space by Ebner and
 * Fairchild, 1998).
 * https://www.dolby.com/us/en/technologies/dolby-vision/ictcp-white-paper.pdf
 * https://www.dolby.com/us/en/technologies/dolby-vision/measuring-perceptual-color-volume-v7.1.pdf
 * 
 * @author Michael Carleton
 *
 */
final class ITP implements ColorSpace {

	private static final double m1 = 2610d / 16384;
	private static final double m2 = 2523d / 4096 * 128;
	private static final double m1B = 1d / m1;
	private static final double m2B = 1d / m2;
	private static final double c1 = 3424d / 4096;
	private static final double c2 = 2413d / 4096 * 32;
	private static final double c3 = 2392d / 4096 * 32;

	/**
	 * Look-up table for EOTF function (as domain is 0...1)
	 */
	private static final double[] LUT;
	private static final int LUT_SIZE = 2000; // 2500 seems more than sufficient

	static {
		LUT = new double[LUT_SIZE];
		for (int i = 0; i < LUT.length; i++) {
			LUT[i] = EOTF((1d / LUT_SIZE) * i);
		}
	}

	ITP() {
	}

	/**
	 * 
	 * @param rgb [R,G,B] 0...1
	 * @return [I,T,P]
	 */
	@Override
	public double[] fromRGB(double[] rgb) {
		double L = 1688 * rgb[0] + 2146 * rgb[1] + 262 * rgb[2];
		double M = 683 * rgb[0] + 2951 * rgb[1] + 462 * rgb[2];
		double S = 99 * rgb[0] + 309 * rgb[1] + 3688 * rgb[2];

		L /= 4096;
		M /= 4096;
		S /= 4096;

		L = inverseEOTF(L);
		M = inverseEOTF(M);
		S = inverseEOTF(S);

		double I, P, T;

		I = 2048 * L + 2048 * M;
		P = 6610 * L + -13613 * M + 7003 * S;
		T = 17933 * L + -17390 * M + -543 * S;

		I /= 4096;
		P /= 4096;
		T /= 4096;

		return new double[] { I, P, T };
	}

	@Override
	public double[] toRGB(double[] itp) {
		double I = itp[0] + 0.00860904 * itp[1] + 0.11102963 * itp[2];
		double T = itp[0] + -0.00860904 * itp[1] + -0.11102963 * itp[2];
		double P = itp[0] + 0.56003134 * itp[1] + -0.32062717 * itp[2];

		double[] LMS = new double[] { EOTF(I), EOTF(T), EOTF(P) };

		double R = 3.43660669 * LMS[0] + -2.50645212 * LMS[1] + 0.06984542 * LMS[2];
		double G = -0.79132956 * LMS[0] + 1.98360045 * LMS[1] + -0.1922709 * LMS[2];
		double B = -0.0259499 * LMS[0] + -0.09891371 * LMS[1] + 1.12486361 * LMS[2];

		return new double[] { R, G, B };
	}

	public static double[] itp2rgbQuick(double[] itp) {
		double I = itp[0] + 0.00860904 * itp[1] + 0.11102963 * itp[2];
		double T = itp[0] + -0.00860904 * itp[1] + -0.11102963 * itp[2];
		double P = itp[0] + 0.56003134 * itp[1] + -0.32062717 * itp[2];

		double[] LMS = new double[] { LUT[(int) (I * LUT_SIZE)], LUT[(int) (T * LUT_SIZE)], LUT[(int) (P * LUT_SIZE)] };

		double R = 3.43660669 * LMS[0] + -2.50645212 * LMS[1] + 0.06984542 * LMS[2];
		double G = -0.79132956 * LMS[0] + 1.98360045 * LMS[1] + -0.1922709 * LMS[2];
		double B = -0.0259499 * LMS[0] + -0.09891371 * LMS[1] + 1.12486361 * LMS[2];

		return new double[] { R, G, B };
	}

	/**
	 * SMPTE ST 2084:2014 optimised perceptual inverse electro-optical transfer
	 * function (EOTF / EOCF).
	 * 
	 * https://color.readthedocs.io/en/develop/_modules/color/models/rgb/transfer_functions/st_2084.html#eotf_inverse_ST2084
	 */
	private static double inverseEOTF(double F) {
		double Y = Math.pow(F, m1); // reference implementation is F / 10000

		return Math.pow(((c1 + c2 * Y) / (1 + c3 * Y)), m2);
	}

	private static double EOTF(double N) {
		double V_p = FastMath.pow(N, m2B);

		double n = V_p - c1;
		n = n < 0 ? 0 : n;

		return FastMath.pow(n / (c2 - c3 * V_p), m1B); // L; reference implementation is ___ * 10000
	}

	/**
	 * Quicker EOTF that uses a pre-computed LUT to return values.
	 * 
	 * @param N (is between 0...1)
	 * @return
	 * @deprecated LUT called inline in {@link #itp2rgbQuick(double[])} instead
	 */
	@Deprecated
	private static double EOTFQuick(double N) {
		return LUT[(int) (N * LUT_SIZE)];
	}

}
