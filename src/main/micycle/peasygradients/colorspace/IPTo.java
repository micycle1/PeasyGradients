package micycle.peasygradients.colorspace;

import micycle.peasygradients.utilities.FastPow;
import net.jafama.FastMath;

/**
 * Ragoo and Farup's (2021) optimised IPT colorspace, which improves both colour
 * order and perceptual uniformity with respect to the original IPT transform.
 * <p>
 * IPT opponents: I light–dark; P red–green, T yellow–blue.
 * 
 * @author Michael Carleton
 *
 */
final class IPTo implements ColorSpaceTransform {

	// https://www.researchgate.net/publication/356199519

	@Override
	public double[] toRGB(final double[] IPT) {

		// IPT -> L'M'S'
		final double[] LMS = IPTtoLMSInverse(IPT);

		double L = LMS[0];
		double M = LMS[1];
		double S = LMS[2];

		// L'M'S' -> LMS
		final double power = 2.4563989; // = 1 / 0.4071
		if (L < 0) {
			L = -FastPow.fastPow(L, power);
		} else { // >=0
			L = FastPow.fastPow(L, power);
		}
		if (M < 0) {
			M = -FastPow.fastPow(M, power);
		} else { // >=0
			M = FastPow.fastPow(M, power);
		}
		if (S < 0) {
			S = -FastPow.fastPow(S, power);
		} else { // >=0
			S = FastPow.fastPow(S, power);
		}

		// apply LMS->XYZ matrix
		return XYZ.xyz2rgb(LMStoXYZInverse(new double[] { L, M, S }));

	}

	/**
	 * Reverses {@link #LMStoXYZ(double[])}
	 * 
	 * @param IPT
	 * @return
	 */
	public static double[] LMStoXYZInverse(double[] IPT) {
		//@formatter:off
	    return new double[] { 
			IPT[0] * 1.80808907 + IPT[1] * -1.12559776 + IPT[2] * 0.26790865, 
			IPT[0] * 0.28725347 + IPT[1] * 0.69759246 + IPT[2] * 0.0151522,
			IPT[0] * -0.21879 + IPT[1] * -0.0496069 + IPT[2] * 1.35728432
		};
	  //@formatter:on
	}

	/**
	 * Reverses L'M'S' -> IPT (inline in fromRGB())
	 * 
	 * @param IPT
	 * @return
	 */
	private static double[] IPTtoLMSInverse(double[] IPT) {
		//@formatter:off
		return new double[] { 
			IPT[0] * 0.99988723 + IPT[1] * 0.12783032 + IPT[2] * 0.12216833,
			IPT[0] * 0.99990673 + IPT[1] * -0.06722854 + IPT[2] * -0.0249936,
			IPT[0] * 0.99987776 + IPT[1] * 0.22247761 + IPT[2] * -0.73865223
		};
		//@formatter:on
	}

	@Override
	public double[] fromRGB(final double[] RGB) {
		final double[] LMS = XYZtoLMS(XYZ.rgb2xyz(RGB)); // convert XYZ to LMS (using IPT matrix)

		double L = LMS[0];
		double M = LMS[1];
		double S = LMS[2];

		// LMS -> L'M'S'
		final double power = 0.4071;
		if (L < 0) {
			L = -FastMath.pow(L, power);
		} else { // >=0
			L = FastMath.pow(L, power);
		}
		if (M < 0) {
			M = -FastMath.pow(M, power);
		} else { // >=0
			M = FastMath.pow(M, power);
		}
		if (S < 0) {
			S = -FastMath.pow(S, power);
		} else { // >=0
			S = FastMath.pow(S, power);
		}

		// L'M'S' -> IPT
		//@formatter:off
		return new double[] { 
		    L * 0.3037 + M * 0.6688 + S * 0.0276, 
		    L * 3.9247 + M * -4.7339 + S * 0.8093, 
		    L * 1.5932 + M * -0.5205 + S * -1.0727 
		};
		//@formatter:on
	}

	/**
	 * Applies the Hunt-Pointer-Estevez (HPE) transformation matrix, normalised to
	 * D65, to XYZ values.
	 * 
	 * @param XYZ
	 * @return
	 */
	private static double[] XYZtoLMS(double[] XYZ) {
		//@formatter:off
	    return new double[] { 
	        XYZ[0] * 0.4321 + XYZ[1] * 0.6906 + XYZ[2] * -0.0930, 
	        XYZ[0] * -0.1793 + XYZ[1] * 1.1458 + XYZ[2] * 0.0226,
	        XYZ[0] * 0.0631 + XYZ[1] * 0.1532 + XYZ[2] * 0.7226 
	    };
	  //@formatter:on
	}

}
