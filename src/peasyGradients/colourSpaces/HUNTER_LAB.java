package peasyGradients.colourSpaces;

import net.jafama.FastMath;

/**
 * The Hunter L, a, b color scale is more visually uniform than the CIE XYZ
 * color scale. The Hunter L, a, b scale contracts in the yellow region of color
 * space and expands in the blue region.
 * 
 * https://www.easyrgb.com/en/math.php
 * 
 * @author micycle1
 *
 */
public final class HUNTER_LAB {

	/**
	 * D65/2° Illuminants: Daylight, sRGB, Adobe-RGB
	 */
	private static final double illuminantX = 95.047;
	private static final double illuminantY = 100.000;
	private static final double illuminantZ = 108.883;

	private static final double ka = (175.0 / 198.04) * (illuminantY + illuminantX);
	private static final double kb = (70.0 / 218.11) * (illuminantY + illuminantZ);

	public static double[] rgb2hlab(double rgb[]) {
		return xyz2hlab(XYZ.rgb2xyz(rgb));
	}

	public static double[] hlab2rgb(double[] lab) {
		return XYZ.xyz2rgb(hlab2xyz(lab));
	}

	public static double[] hlab2rgbQuick(double[] lab) {
		return XYZ.xyz2rgbQuick(hlab2xyzQuick(lab));
	}

	private static double[] xyz2hlab(double[] xyz) {

		double L = 100.0 * FastMath.sqrt(xyz[1] / illuminantY);
		double a = ka * ((xyz[0] / illuminantX - xyz[1] / illuminantY) / FastMath.sqrt(xyz[1] / illuminantY));
		double b = kb * ((xyz[1] / illuminantY - xyz[2] / illuminantZ) / FastMath.sqrt(xyz[1] / illuminantY));

		if (Double.isNaN(a)) {
			a = 0;
		}

		if (Double.isNaN(b)) {
			b = 0;
		}

		return new double[] { L, a, b };
	}

	private static double[] hlab2xyz(double[] lab) {
		lab[0] = lab[0] * lab[0] * 0.01;
		return new double[] {
				((lab[1] / ka * FastMath.sqrt(lab[0] / illuminantY)) + (lab[0] / illuminantY)) * illuminantX, lab[0],
				-(lab[2] / kb * FastMath.sqrt(lab[0] / illuminantY) - (lab[0] / illuminantY)) * illuminantZ };
	}

	private static double[] hlab2xyzQuick(double[] lab) {
		lab[0] = lab[0] * lab[0] * 0.01;
		return new double[] {
				((lab[1] / ka * FastMath.sqrtQuick(lab[0] / illuminantY)) + (lab[0] / illuminantY)) * illuminantX,
				lab[0],
				-(lab[2] / kb * FastMath.sqrtQuick(lab[0] / illuminantY) - (lab[0] / illuminantY)) * illuminantZ };
	}

}
