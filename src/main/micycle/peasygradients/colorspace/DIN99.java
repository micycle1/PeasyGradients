package micycle.peasygradients.colorspace;

import net.jafama.FastMath;

/**
 * DIN99 is a further development of the CIELAB color space system developed by
 * the Working Committee FNF / FNL 2 colorimetry.
 * 
 * @author Michael Carleton
 *
 */
final class DIN99 implements ColorSpace {
	
	// https://github.com/frickels/colorspace/blob/master/src/main/java/info/kuechler/frickels/colorspace/DIN99.java

	private static final double SIN_16DEG = Math.sin(Math.toRadians(16.));
	private static final double COS_16DEG = Math.cos(Math.toRadians(16.));
	private static final double FAC_1 = 100. / Math.log(129. / 50.); // = 105.51
	private static final double kCH = 1.;
	private static final double kE = 1.;

	DIN99() {
	}

	/**
	 * 
	 * @param rgb [R,G,B] 0...1
	 * @return
	 */
	@Override
	public double[] fromRGB(double[] rgb) {
		return lab2din(LAB.rgb2lab(rgb));
	}

	@Override
	public double[] toRGB(double[] din) {
		return LAB.lab2rgb(din2lab(din));
	}

	private static double[] lab2din(final double[] lab) {
		final double L = lab[0];
		final double a = lab[1];
		final double b = lab[2];

		final double L99 = kE * FAC_1 * Math.log(1. + 0.0158 * L);
		double a99 = 0.;
		double b99 = 0.;
		if (a != 0. || b != 0.) {
			final double e = a * COS_16DEG + b * SIN_16DEG;
			final double f = 0.7 * (b * COS_16DEG - a * SIN_16DEG);
			final double G = Math.sqrt(e * e + f * f);
			if (G != 0.) {
				// opt: "/ G"
				final double k = Math.log(1. + 0.045 * G) / (0.045 * kCH * kE * G);
				a99 = k * e;
				b99 = k * f;
			}
		}
		return new double[] { L99, a99, b99 };
	}

	private static double[] din2lab(double[] DIN) {
		final double L = DIN[0];
		final double a = DIN[1];
		final double b = DIN[2];

		final double hef = FastMath.atan2(b, a);
		final double C = Math.sqrt(a * a + b * b);
		final double G = (FastMath.exp(0.045 * C * kCH * kE) - 1.) / 0.045;
		final double e = G * FastMath.cos(hef);
		final double f = G * FastMath.sin(hef) / 0.7; // opt: "/ 0 .7"

		final double Ln = (FastMath.exp((L * kE) / FAC_1) - 1.) / 0.0158;
		final double an = e * COS_16DEG - f * SIN_16DEG;
		final double bn = e * SIN_16DEG + f * COS_16DEG;
		return new double[] { Ln, an, bn };
	}
}
