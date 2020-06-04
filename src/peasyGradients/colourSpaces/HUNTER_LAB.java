package peasyGradients.colourSpaces;

import net.jafama.FastMath;
import peasyGradients.utilities.FastPow;

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
		return XYZ.xyz2rgbQuick(hlab2xyz(lab));
	}

	private static double[] xyz2hlab(double[] xyz) {
		return new double[] { 100.0 * FastMath.sqrt(xyz[1] / illuminantY),
				(((xyz[0] / illuminantX) - (xyz[1] / illuminantY)) / FastMath.sqrt(xyz[1] / illuminantY)),
				kb * (((xyz[1] / illuminantY) - (xyz[2] / illuminantZ)) / FastMath.sqrt(xyz[1] / illuminantY)) };
	}

	private static double[] hlab2xyz(double[] lab) {
		return new double[] { lab[0] = ((lab[0] * lab[0]) / (illuminantY * illuminantY)) * 100,
				((lab[1] / ka * FastMath.sqrtQuick(lab[0] / illuminantY)) + (lab[0] / illuminantY)) * illuminantX,
				-(lab[2] / kb * FastMath.sqrtQuick(lab[0] / illuminantY) - (lab[0] / illuminantY)) * illuminantZ };
	}

}
