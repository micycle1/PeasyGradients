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
final class HUNTER_LAB implements ColourSpace {

	/**
	 * D65/2° Illuminants: Daylight, sRGB, Adobe-RGB
	 */
	private static final double illuminantX = 95.047;
	private static final double illuminantY = 100.000;
	private static final double illuminantZ = 108.883;

	private static final double ka = (175.0 / 198.04) * (illuminantY + illuminantX);
	private static final double kb = (70.0 / 218.11) * (illuminantY + illuminantZ);
	
	/**
	 * Look-up table for EOTF function (as domain is 0...1)
	 */
	private static final double[] LUT;
	private static final int LUT_SIZE = 2000; // 2500 seems more than sufficient
	
	static {
		LUT = new double[LUT_SIZE];
		for (int i = 0; i < LUT.length; i++) {
			LUT[i] = Math.sqrt((1d / LUT_SIZE) * i);
		}
	}
	
	HUNTER_LAB() {
	}

	public double[] fromRGB(double rgb[]) {
		return xyz2hlab(XYZ.rgb2xyz(rgb));
	}

	public double[] toRGB(double[] lab) {
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
				((lab[1] / ka * Math.sqrt(lab[0] / illuminantY)) + (lab[0] / illuminantY)) * illuminantX, lab[0],
				-(lab[2] / kb * Math.sqrt(lab[0] / illuminantY) - (lab[0] / illuminantY)) * illuminantZ };
	}
	
	/**
	 * If illuminantY == 100, we can remove some divisions and the sqrt easily. 
	 * @param lab
	 * @return
	 */
	private static double[] hlab2xyzQuick(double[] lab) {
		double cache = lab[0]*0.01;
		lab[0] = lab[0] * lab[0] * 0.0001;

		return new double[] { ((lab[1] / ka * cache) + (lab[0])) * illuminantX, 100 * lab[0],
				-(lab[2] / kb * cache - (lab[0])) * illuminantZ };
	}

}
