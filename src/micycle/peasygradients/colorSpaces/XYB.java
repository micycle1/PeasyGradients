package micycle.peasygradients.colorSpaces;

import net.jafama.FastMath;

/**
 * The XYB colorspace is a trichromatic perceptually-motivated colorspace, where
 * X is roughly red-subtract-green; Y is yellow; B is blue. The XYB color
 * transform is a hybrid color model inspired by the human visual system,
 * facilitating perceptually uniform quantization.
 * 
 * <p>
 * XYB is used in JPEG XL (see
 * <a href="https://arxiv.org/ftp/arxiv/papers/1908/1908.03565.pdf">this</a>
 * paper or <a href="https://arxiv.org/pdf/1803.09165.pdf">this</a> one) and was
 * introduced in Google's butteraugli tool by Jyrki Alakuijala (as far as I can
 * tell).
 * 
 * <p>
 * This Java implementation was adapted from Matt DesLauriers' Javascript
 * implementation found <a href=
 * "https://observablehq.com/@mattdesl/perceptually-smooth-multi-color-linear-gradients">here</a>.
 * 
 * @author micycle1
 *
 */
final class XYB implements ColorSpace {

	// Parameters for gamma conversion (linear RGB <-> sRGB)
	private static final double gamma = 2.4;
	private static final double inverseGamma = 1 / gamma;

	// Unscaled values for kOpsinAbsorbanceBias
	private static final double kB0 = 0.96723368009523958;
	private static final double kB1 = kB0;
	private static final double kB2 = kB0;
	private static final double kScale = 255.0;
	private static final double kInvScaleR = 1.0;
	private static final double kInvScaleG = 1.0;

	// Parameters for opsin absorbance.
	private static final double kM02 = 0.078;
	private static final double kM00 = 0.30;
	private static final double kM01 = 1.0 - kM02 - kM00;

	private static final double kM12 = 0.078;
	private static final double kM10 = 0.23;
	private static final double kM11 = 1.0 - kM12 - kM10;

	private static final double kM20 = 0.24342268924547819;
	private static final double kM21 = 0.20476744424496821;
	private static final double kM22 = 1.0 - kM20 - kM21;

	private static final double[] premul_absorb = new double[12];

	// RGB->XYB
	private static final double[] kOpsinAbsorbanceMatrix;
	private static final double[] kOpsinAbsorbanceBias;

	// XYB->RGB
	private static final double[] kNegOpsinAbsorbanceBiasRGB;
	private static final double[] kNegOpsinAbsorbanceBiasCbrt;
	private static final double[] kDefaultInverseOpsinAbsorbanceMatrix;

	static {

		kOpsinAbsorbanceBias = new double[] { kB0 / kScale, kB1 / kScale, kB2 / kScale };

		kOpsinAbsorbanceMatrix = new double[] { kM00 / kScale, kM01 / kScale, kM02 / kScale, kM10 / kScale, kM11 / kScale, kM12 / kScale,
				kM20 / kScale, kM21 / kScale, kM22 / kScale, };

		for (int i = 0; i < 9; i++) {
			premul_absorb[i] = kOpsinAbsorbanceMatrix[i];
		}
		for (int i = 0; i < 3; i++) {
			premul_absorb[9 + i] = -Math.cbrt(kOpsinAbsorbanceBias[i]);
		}

		kNegOpsinAbsorbanceBiasRGB = new double[] { -kOpsinAbsorbanceBias[0], -kOpsinAbsorbanceBias[1], -kOpsinAbsorbanceBias[2], 255 };

		kNegOpsinAbsorbanceBiasCbrt = new double[kNegOpsinAbsorbanceBiasRGB.length];
		for (int i = 0; i < kNegOpsinAbsorbanceBiasRGB.length; i++) {
			kNegOpsinAbsorbanceBiasCbrt[i] = Math.cbrt(kNegOpsinAbsorbanceBiasRGB[i]);
		}

		kDefaultInverseOpsinAbsorbanceMatrix = new double[] { 2813.04956, -2516.07070, -41.9788641, -829.807582, 1126.78645, -41.9788641,
				-933.007078, 691.795377, 496.211701 };

	}

	/**
	 * Returns normalised sRGB [0...1]
	 */
	@Override
	public double[] toRGB(double[] XYB) {

		final double opsin_x = XYB[0];
		final double opsin_y = XYB[1];
		final double opsin_b = XYB[2];

		// Color space: XYB -> RGB
		double gamma_r = kInvScaleR * (opsin_y + opsin_x);
		double gamma_g = kInvScaleG * (opsin_y - opsin_x);
		double gamma_b = opsin_b;

		gamma_r -= kNegOpsinAbsorbanceBiasCbrt[0];
		gamma_g -= kNegOpsinAbsorbanceBiasCbrt[1];
		gamma_b -= kNegOpsinAbsorbanceBiasCbrt[2];

		// Undo gamma compression: linear = gamma^3 for efficiency.
		final double gamma_r2 = gamma_r * gamma_r;
		final double gamma_g2 = gamma_g * gamma_g;
		final double gamma_b2 = gamma_b * gamma_b;
		final double mixed_r = mulAdd(gamma_r2, gamma_r, kNegOpsinAbsorbanceBiasRGB[0]);
		final double mixed_g = mulAdd(gamma_g2, gamma_g, kNegOpsinAbsorbanceBiasRGB[1]);
		final double mixed_b = mulAdd(gamma_b2, gamma_b, kNegOpsinAbsorbanceBiasRGB[2]);

		// Unmix (multiply by 3x3 inverse_matrix)
		double linear_r, linear_g, linear_b;
		linear_r = kDefaultInverseOpsinAbsorbanceMatrix[0] * mixed_r;
		linear_g = kDefaultInverseOpsinAbsorbanceMatrix[3] * mixed_r;
		linear_b = kDefaultInverseOpsinAbsorbanceMatrix[6] * mixed_r;
		linear_r = mulAdd(kDefaultInverseOpsinAbsorbanceMatrix[1], mixed_g, linear_r);
		linear_g = mulAdd(kDefaultInverseOpsinAbsorbanceMatrix[4], mixed_g, linear_g);
		linear_b = mulAdd(kDefaultInverseOpsinAbsorbanceMatrix[7], mixed_g, linear_b);
		linear_r = mulAdd(kDefaultInverseOpsinAbsorbanceMatrix[2], mixed_b, linear_r);
		linear_g = mulAdd(kDefaultInverseOpsinAbsorbanceMatrix[5], mixed_b, linear_g);
		linear_b = mulAdd(kDefaultInverseOpsinAbsorbanceMatrix[8], mixed_b, linear_b);

		return new double[] { linearToGamma(linear_r), linearToGamma(linear_g), linearToGamma(linear_b) };
	}

	/**
	 * RGB values [0...1]
	 */
	@Override
	public double[] fromRGB(double[] RGB) {

		// gamma to linear

		final double lR = gammaToLinear(RGB[0]);
		final double lG = gammaToLinear(RGB[1]);
		final double lB = gammaToLinear(RGB[2]);

		double[] mixed = OpsinAbsorbance(lR, lG, lB);

		double L = mixed[0];
		double M = mixed[1];
		double S = mixed[2];

		// should be non-negative even for wide-gamut, so clamp to zero.
		L = Math.max(0, L);
		M = Math.max(0, M);
		S = Math.max(0, S);

		L = FastMath.cbrt(L) + premul_absorb[9];
		M = FastMath.cbrt(M) + premul_absorb[10];
		S = FastMath.cbrt(S) + premul_absorb[11];

		return linearXybTransform(L, M, S);
	}

	private static final double[] OpsinAbsorbance(double r, double g, double b) {
		final double mixed0 = mulAdd(premul_absorb[0], r,
				mulAdd(premul_absorb[1], g, mulAdd(premul_absorb[2], b, kOpsinAbsorbanceBias[0])));
		final double mixed1 = mulAdd(premul_absorb[3], r,
				mulAdd(premul_absorb[4], g, mulAdd(premul_absorb[5], b, kOpsinAbsorbanceBias[1])));
		final double mixed2 = mulAdd(premul_absorb[6], r,
				mulAdd(premul_absorb[7], g, mulAdd(premul_absorb[8], b, kOpsinAbsorbanceBias[2])));
		return new double[] { mixed0, mixed1, mixed2 };
	}

	private static double[] linearXybTransform(double r, double g, double b) {
		return new double[] { 0.5 * (r - g), 0.5 * (r + g), b };
	}

	private static double mulAdd(double a, double b, double c) {
		return a * b + c;
	}

	private static double gammaToLinear(final double n) {
		if (n > 0.04045) {
			return FastMath.pow((n + 0.055) / 1.055, gamma);
		} else {
			return n / 12.92;
		}
	}

	/**
	 * Used during XYB -> RGB
	 */
	private static double linearToGamma(final double n) {
		if (n > 0.0031308) {
			return 1.055 * FastMath.pow(n, inverseGamma) - 0.055;
		} else {
			return n * 12.92;
		}
	}

}
