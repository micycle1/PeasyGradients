package peasyGradients.colorSpaces;

import net.jafama.FastMath;
import peasyGradients.utilities.FastPow;

/**
 * IPT space, published by Ebner and Fairchild (1998)
 * 
 * <p>
 * The IPT space was derived specifically for image processing applications to
 * have a relatively simple formulation and specifically to have a hue angle
 * component with good prediction of constant perceived hue (important in
 * gamut-mapping applications).
 * 
 * <p>
 * IPT opponent space: I light–dark; P red–green, T yellow–blue.
 * 
 * @author micycle1
 *
 */
public final class IPT implements ColorSpace {

	@Override
	public double[] toRGB(final double[] IPT) {

		// IPT -> L'M'S'
		final double[] LMS = IPTtoLMSInverse(IPT);

		double L = LMS[0];
		double M = LMS[1];
		double S = LMS[2];

		// L'M'S' -> LMS
		if (L < 0) {
			L = -FastPow.fastPow(L, 2.3256); // 2.3256 == 1 / 0.43
		} else { // >=0
			L = FastPow.fastPow(L, 2.3256);
		}
		if (M < 0) {
			M = -FastPow.fastPow(M, 2.3256);
		} else { // >=0
			M = FastPow.fastPow(M, 2.3256);
		}

		if (S < 0) {
			S = -FastPow.fastPow(S, 2.3256);
		} else { // >=0
			S = FastPow.fastPow(S, 2.3256);
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
		return new double[] { IPT[0] * 1.8501 + IPT[1] * -1.1383 + IPT[2] * 0.2385, IPT[0] * 0.3668 + IPT[1] * 0.6439 + IPT[2] * -0.0107,
				IPT[0] * 0 + IPT[1] * 0 + IPT[2] * 1.0889 };
	}

	/**
	 * Reverses L'M'S' -> IPT (inline in fromRGB())
	 * 
	 * @param IPT
	 * @return
	 */
	private static double[] IPTtoLMSInverse(double[] IPT) {
		return new double[] { IPT[0] * 1 + IPT[1] * 0.0976 + IPT[2] * 0.2052, IPT[0] * 1 + IPT[1] * -0.1139 + IPT[2] * 0.1332,
				IPT[0] * 1 + IPT[1] * 0.0326 + IPT[2] * -0.6769 };
	}

	@Override
	public double[] fromRGB(final double[] RGB) {
		final double[] LMS = XYZtoLMS(XYZ.rgb2xyz(RGB)); // convert XYZ to LMS (using IPT matrix)

		double L = LMS[0];
		double M = LMS[1];
		double S = LMS[2];

		// LMS -> L'M'S'
		if (L < 0) {
			L = -FastMath.pow(L, 0.43);
		} else { // >=0
			L = FastMath.pow(L, 0.43);
		}
		if (M < 0) {
			M = -FastMath.pow(M, 0.43);
		} else { // >=0
			M = FastMath.pow(M, 0.43);
		}

		if (S < 0) {
			S = -FastMath.pow(S, 0.43);
		} else { // >=0
			S = FastMath.pow(S, 0.43);
		}

		// L'M'S' -> IPT
		return new double[] { L * 0.40 + M * 0.40 + S * 0.2, L * 4.455 + M * -4.8510 + S * 0.3960, L * 0.8056 + M * 0.3572 + S * -1.1628 };
	}

	/**
	 * Applies the Hunt-Pointer-Estevez (HPE) transformation matrix, normalised to
	 * D65, to XYZ values.
	 * 
	 * @param XYZ
	 * @return
	 */
	private static double[] XYZtoLMS(double[] XYZ) {
		return new double[] { XYZ[0] * 0.4002 + XYZ[1] * 0.7075 + XYZ[2] * -0.0807, XYZ[0] * -0.2280 + XYZ[1] * 1.15 + XYZ[2] * 0.0612,
				XYZ[0] * 0.0 + XYZ[1] * 0.0 + XYZ[2] * 0.9184 };
	}
}
