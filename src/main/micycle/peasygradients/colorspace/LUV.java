package micycle.peasygradients.colorspace;

import net.jafama.FastMath;

/**
 * The CIE LUV system describes a color based on three qualities: L* is CIE
 * lightness, similar to luminance, the amount of light, but adjusted for human
 * perception. 0 <= L* <= 100. u* is the amount of red chrominance, with
 * negative values indicating green. v* is the amount of yellow chrominance,
 * with negative values indicating blue.
 * 
 * <p>
 * CIELUV has many of the same properties as CIELAB (e.g., stimulus and source
 * chromaticities as input and lightness, chroma, and hue predictors as output),
 * but incorporates a different form of chromatic adaptation transform.
 * 
 * @author Michael Carleton
 *
 */
final class LUV implements ColorSpaceTransform {

	private static final double refU = 0.19783000664283;
	private static final double refV = 0.46831999493879;
	private static final double refY = 1.0;

	private static final double kappa = 903.2962962;
	private static final double epsilon = 0.0088564516;

	LUV() {
	}

	/**
	 * 
	 * @param rgb [R,G,B] 0...1
	 * @return
	 */
	@Override
	public double[] fromRGB(double[] rgb) {
		return xyz2luv(XYZ.rgb2xyz(rgb));
	}

	@Override
	public double[] toRGB(double[] luv) {
		return XYZ.xyz2rgb(luv2xyz(luv));
	}

	private static double[] luv2rgbQuick(double luv[]) {
		return XYZ.xyz2rgbQuick(luv2xyzQuick(luv));
	}

	private static double[] xyz2luv(double[] tuple) {
		double X = tuple[0];
		double Y = tuple[1];
		double Z = tuple[2];

		double varU = (4 * X) / (X + (15 * Y) + (3 * Z));
		double varV = (9 * Y) / (X + (15 * Y) + (3 * Z));

		double L = yToL(Y);

		if (L == 0) {
			return new double[] { 0, 0, 0 };
		}

		double U = 13 * L * (varU - refU);
		double V = 13 * L * (varV - refV);

		return new double[] { L, U, V };
	}

	private static double[] luv2xyz(double[] tuple) {

		if (tuple[0] == 0) {
			return new double[] { 0, 0, 0 };
		}

		double varU = tuple[1] / (13 * tuple[0]) + refU;
		double varV = tuple[2] / (13 * tuple[0]) + refV;

		double Y = (tuple[0] + 16) * 0.008621;
		if (Y > 0.206893034) {
			Y = Y * Y * Y;
		} else {
			Y = (Y - 0.13793) * 0.12842;
		}

		final double X = 0 - (9 * Y * varU) / ((varU - 4) * varV - varU * varV);
		final double Z = (9 * Y - (15 * varV * Y) - (varV * X)) / (3 * varV);

		return new double[] { X, Y, Z };
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	private static double[] luv2xyzQuick(double[] tuple) {

		double varU = tuple[1] / (13 * tuple[0]) + refU;
		double varV = tuple[2] / (13 * tuple[0]) + refV;

		double Y = (tuple[0] + 16) * 0.008621;
		if (Y > 0.206893034) {
			Y = Y * Y * Y;
		} else {
			Y = (Y - 0.13793) * 0.12842;
		}

		double X = 0 - (9 * Y * varU) / ((varU - 4) * varV - varU * varV);
		double Z = (9 * Y - (15 * varV * Y) - (varV * X)) / (3 * varV);

		return new double[] { X, Y, Z };
	}

	private static double yToL(double Y) {
		if (Y <= epsilon) {
			return (Y / refY) * kappa;
		} else {
			return 116 * FastMath.pow(Y / refY, 1.0 / 3.0) - 16;
		}
	}

}
