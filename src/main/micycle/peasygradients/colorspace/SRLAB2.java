package micycle.peasygradients.colorspace;

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
 * The SRLAB2 model is almost as easy to use as CIELAB, while overcoming its
 * errors especially visible in hue-changes of blue colors.
 * <p>
 * A Java implementation of 'Deficiencies of the CIE-L*a*b* color space and
 * introduction of the SRLAB2 color model'.
 */
class SRLAB2 implements ColorSpaceTransform {

	// https://www.magnetkern.de/srlab2.html

	@Override
	public double[] toRGB(double[] color) {

		double lightness = color[0];
		double a = color[1];
		double b = color[2];

		double x, y, z, rd, gn, bl;
		x = 0.01 * lightness + 0.000904127 * a + 0.000456344 * b;
		y = 0.01 * lightness - 0.000533159 * a - 0.000269178 * b;
		z = 0.01 * lightness - 0.005800000 * b;

		if (x <= 0.08) {
			x *= 2700.0 / 24389.0;
		} else {
			x = cube((x + 0.16) / 1.16);
		}
		if (y <= 0.08) {
			y *= 2700.0 / 24389.0;
		} else {
			y = cube((y + 0.16) / 1.16);
		}
		if (z <= 0.08) {
			z *= 2700.0 / 24389.0;
		} else {
			z = cube((z + 0.16) / 1.16);
		}

		rd = 5.435679 * x - 4.599131 * y + 0.163593 * z;
		gn = -1.168090 * x + 2.327977 * y - 0.159798 * z;
		bl = 0.037840 * x - 0.198564 * y + 1.160644 * z;
		double red, green, blue;

		if (rd <= 0.00304) {
			red = rd * 12.92;
		} else {
			red = 1.055 * FastMath.powQuick(rd, 1.0 / 2.4) - 0.055;
		}
		if (gn <= 0.00304) {
			green = gn * 12.92;
		} else {
			green = 1.055 * FastMath.powQuick(gn, 1.0 / 2.4) - 0.055;
		}
		if (bl <= 0.00304) {
			blue = bl * 12.92;
		} else {
			blue = 1.055 * FastMath.powQuick(bl, 1.0 / 2.4) - 0.055;
		}

		return new double[] { red, blue, green }; // NOTE order swapped
	}

	@Override
	public double[] fromRGB(double[] RGB) {

		double red = RGB[0];
		double blue = RGB[1];
		double green = RGB[2];
		double x, y, z;

		if (red <= 0.03928) {
			red /= 12.92;
		} else {
			red = FastMath.pow((red + 0.055) / 1.055, 2.4);
		}
		if (green <= 0.03928) {
			green /= 12.92;
		} else {
			green = FastMath.pow((green + 0.055) / 1.055, 2.4);
		}
		if (blue <= 0.03928) {
			blue /= 12.92;
		} else {
			blue = FastMath.pow((blue + 0.055) / 1.055, 2.4);
		}

		x = 0.320530 * red + 0.636920 * green + 0.042560 * blue;
		y = 0.161987 * red + 0.756636 * green + 0.081376 * blue;
		z = 0.017228 * red + 0.108660 * green + 0.874112 * blue;

		if (x <= 216.0 / 24389.0) {
			x *= 24389.0 / 2700.0;
		} else {
			x = 1.16 * FastMath.pow(x, 1.0 / 3.0) - 0.16;
		}
		if (y <= 216.0 / 24389.0) {
			y *= 24389.0 / 2700.0;
		} else {
			y = 1.16 * FastMath.pow(y, 1.0 / 3.0) - 0.16;
		}
		if (z <= 216.0 / 24389.0) {
			z *= 24389.0 / 2700.0;
		} else {
			z = 1.16 * FastMath.pow(z, 1.0 / 3.0) - 0.16;
		}

		double lightness = 37.0950 * x + 62.9054 * y - 0.0008 * z;
		double a = 663.4684 * x - 750.5078 * y + 87.0328 * z;
		double b = 63.9569 * x + 108.4576 * y - 172.4152 * z;

		return new double[] { lightness, a, b };
	}

	private static double cube(final double x) {
		return x * x * x;
	}

}
