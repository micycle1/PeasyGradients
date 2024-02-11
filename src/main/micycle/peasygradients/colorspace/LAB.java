package micycle.peasygradients.colorspace;

/**
 * A uniform (Opponent color scale) color space in which colors are located
 * within a threedimensional rectangular coordinate system; the three dimensions
 * are lightness (L*), redness/ greenness (a*) and yellowness/blueness (b*).
 * Equal distances in the space approximately represent equal color differences
 * <p>
 * The standard CIE 1976 L*a*b* color space. L* is scaled to vary from 0 to 100;
 * a* and b* are likewise scaled to roughly the range -50 to 50. Also known as
 * L*a*b* or CIE-L*ab.
 * 
 * @author Michael Carleton
 *
 */
final class LAB implements ColorSpaceTransform {

	/**
	 * D65/2° Illuminants: Daylight, sRGB, Adobe-RGB
	 */
	protected static final double illuminantX = 95.047;
	protected static final double illuminantY = 100.000;
	protected static final double illuminantZ = 108.883;

	LAB() {
	}

	@Override
	public double[] fromRGB(double[] rgb) {
		return rgb2lab(rgb);
	}

	@Override
	public double[] toRGB(double[] lab) {
		return lab2rgb(lab);
	}

	/**
	 * 
	 * @param rgb [R,G,B] where values are 0...1.0
	 * @return [L,A,B] with L in [0, 100],
	 * 
	 *         A in [-86.185, 98.254],
	 * 
	 *         B in [-107.863, 94.482]
	 */
	static double[] rgb2lab(double[] rgb) {
		double[] xyz = XYZ.rgb2xyz(rgb); // RGB --> XYZ

		/**
		 * Now convert XYZ to LAB
		 */
		double X = xyz[0] / illuminantX;
		double Y = xyz[1] / illuminantY;
		double Z = xyz[2] / illuminantZ;

		if (X > 0.008856) {
			X = StrictMath.cbrt(X);
		} else {
			X = (7.787 * X) + (16 / 116d);
		}
		if (Y > 0.008856) {
			Y = StrictMath.cbrt(Y);
		} else {
			Y = (7.787 * Y) + (16 / 116d);
		}
		if (Z > 0.008856) {
			Z = StrictMath.cbrt(Z);
		} else {
			Z = (7.787 * Z) + (16 / 116d);
		}

		return new double[] { (116 * Y) - 16, 500 * (X - Y), 200 * (Y - Z) };
	}

	/**
	 * 
	 * @param lab [L,A,B]
	 * @return [R,G,B] where values are 0...1.0
	 */
	static double[] lab2rgb(double[] lab) {
		return XYZ.xyz2rgb(lab2xyz(lab));
	}

	public static double[] lab2rgbQuick(double[] lab) {
		return XYZ.xyz2rgbQuick(lab2xyz(lab));
	}

	public static double[] lab2rgbVeryQuick(double[] lab) {
		return XYZ.xyz2rgbVeryQuick(lab2xyz(lab));
	}

	private static double[] lab2xyz(double[] lab) {
		final double ta = (lab[0] + 16d) / 116d;
		lab[0] = ta + lab[1] / 500d;
		lab[2] = ta - lab[2] / 200d;

		if (lab[0] > 0.206897) {
			lab[0] = lab[0] * lab[0] * lab[0];
		} else {
			lab[0] = 0.12841854934 * lab[0] - 0.01771290335;
		}
		if (ta > 0.206897) {
			lab[1] = ta * ta * ta;
		} else {
			lab[1] = 0.12841854934 * ta - 0.01771290335;
		}
		if (lab[2] > 0.206897) {
			lab[2] = lab[2] * lab[2] * lab[2];
		} else {
			lab[2] = 0.12841854934 * lab[2] - 0.01771290335;
		}

		lab[0] *= illuminantX;
		lab[1] *= illuminantY;
		lab[2] *= illuminantZ;
		return lab;
	}

}
