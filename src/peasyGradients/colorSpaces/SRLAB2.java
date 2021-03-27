package peasyGradients.colorSpaces;

import net.jafama.FastMath;

/**
 * The SRLAB2 color model is a compromise between the simplicity of CIELAB and
 * the correctness of CIECAM02.
 * <p>
 * In contrast to CIECAM02 or RLAB there are no parameters like the adapting
 * luminance or the relative luminance of the surround; only the white point
 * (which is assumed to be completely adapted) has to be known. Compared to RLAB
 * the SRLAB2 model uses the more recent chromatic adaption model of CIECAM02.
 * <p>
 * For a fixed white point, successive linear transformations can be
 * precalulated to form a single matrix, which reduces the complexity of the
 * calculations to two matrix vector multiplications and the componentwise
 * application of the compression function. The SRLAB2 model is almost as easy
 * to use as CIELAB, while overcoming its errors especially visible in
 * hue-changes of blue colors. *
 * <p>
 * Implementation of 'Deficiencies of the CIE-L*a*b* color space and
 * introduction of the SRLAB2 color model'. See
 * https://www.magnetkern.de/srlab2.html
 * 
 * @author micycle1
 *
 */
final class SRLAB2 implements ColorSpace {

	// https://gist.github.com/jjrv/b27d0840b4438502f9cad2a0f9edeabc

	private static final double constA = 1 / 2.4d; // 1/ sRGB gamma

	@Override
	public double[] toRGB(double[] color) {
		return srlab2rgb(color);
	}

	@Override
	public double[] fromRGB(double[] RGB) {
		return rgb2srlab(RGB);
	}

	private static double[] srlab2rgb(double[] lab) {
		
//		lab[0] *= 100; // L
	
		final double x = cube(0.01 * lab[0] + 0.000904127 * lab[1] + 4.56344 * lab[2]);
		final double y = cube(0.01 * lab[0] - 5.33159 * lab[1] - 2.69178 * lab[2]);
		final double z = cube(0.01 * lab[0] - 58 * lab[2]);
	
//		out[0] = delinearise(5.435679 * x - 4.599131 * y + 0.163593 * z);
//		out[1] = delinearise(-1.16809 * x + 2.327977 * y - 0.159798 * z);
//		out[2] = delinearise(0.03784 * x - 0.198564 * y + 1.160644 * z);
	
		return delinearise(new double[] { 5.435679 * x - 4.599131 * y + 0.163593 * z,
				-1.16809 * x + 2.327977 * y - 0.159798 * z, 0.03784 * x - 0.198564 * y + 1.160644 * z });
	
	}

	private static double[] rgb2srlab(double[] rgb) {
		
//		r = linearize(r);
//		g = linearize(g);
//		b = linearize(b);
		
		rgb = linearise(rgb);

		final double x = root(0.32053 * rgb[0] + 0.63692 * rgb[1] + 0.04256 * rgb[2]);
		final double y = root(0.161987 * rgb[0] + 0.756636 * rgb[1] + 0.081376 * rgb[2]);
		final double z = root(0.017228 * rgb[0] + 0.10866 * rgb[1] + 0.874112 * rgb[2]);

//		out[0] = 0.37095 * x + 0.629054 * y - 0.000008 * z;
//		out[1] = 6.634684 * x - 7.505078 * y + 0.870328 * z;
//		out[2] = 0.639569 * x + 1.084576 * y - 1.724152 * z;

		return new double[] { 37.095 * x + 62.9054 * y - 000.0008 * z, 663.4684 * x - 750.5078 * y + 87.0328 * z,
				63.9569 * x + 108.4576 * y - 172.4152 * z };

	}

	private static double[] delinearise(double[] rgb) {
//		double r = rgb[0] * 0.01;
//		double g = rgb[1] * 0.01;
//		double b = rgb[2] * 0.01;
		double r = rgb[0];
		double g = rgb[1];
		double b = rgb[2];
	
		if (r > 0.0031308) {
			r = 1.055 * FastMath.powQuick(r, constA) - 0.055; // powQuick(n, 1/2.4) max error is 1E-5 over range of n =
																// [0...1]
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
	
		return new double[] { r, g, b };
	
	}

	private static double[] linearise(double[] srgb) {
		double x = srgb[0];
		double y = srgb[1];
		double z = srgb[2];

		// convert sRGB (gamma-adjusted) to linear RGB (linear space for XYZ)
		if (x > 0.03928) {
			x = FastMath.pow((x + 0.055) / 1.055, 2.4);
		} else {
			x /= 12.92;
		}
		if (y > 0.03928) {
			y = FastMath.pow((y + 0.055) / 1.055, 2.4);
		} else {
			y /= 12.92;
		}
		if (z > 0.03928) {
			z = FastMath.pow((z + 0.055) / 1.055, 2.4);
		} else {
			z /= 12.92;
		}

//		x *= 100; // TODO
//		y *= 100; // TODO
//		z *= 100; // TODO

		return new double[] { x, y, z };
	}

	private static double cube(double x) {
		if (x <= 8) {
			return x * 0.00110705645; // 27/24389
		}

		x = (x + 16) * 0.00862068965; // /116
		return x * x * x;
	}

	private static double root(double x) {
		return x <= 0.00885645167 ? x * (24389 / 2700) : 1.16 * Math.pow(x, 1 / 3) - 0.16; // 216 / 24389
	}

}
