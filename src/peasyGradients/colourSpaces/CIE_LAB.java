package peasyGradients.colourSpaces;

/**
 * A uniform (Opponent color scale) color space in which colors are located
 * within a threedimensional rectangular coordinate system; the three dimensions
 * are lightness (L*), redness/ greenness (a*) and yellowness/blueness (b*).
 * Equal distances in the space approximately represent equal color differences
 * Also known as L*a*b* or CIE-L*ab.
 * 
 * TODO
 * https://community.khronos.org/t/converting-to-and-from-color-space-in-glsl-only-works-for-few-colors/72107
 * 
 * @author micycle1
 *
 */
public class CIE_LAB {

	/**
	 * D65/2° Illuminants: Daylight, sRGB, Adobe-RGB
	 */
	protected static final double illuminantX = 95.047;
	protected static final double illuminantY = 100.000;
	protected static final double illuminantZ = 108.883;

	private static final double third = 1 / 3d;

	/**
	 * 
	 * @param rgb [R,G,B] where values are 0...1.0
	 * @return [L,A,B]
	 */
	public static double[] rgb2lab(double[] rgb) {
		double[] xyz = XYZ.rgb2xyz(rgb); // RGB --> XYZ

		/**
		 * Now convert XYZ to LAB
		 */
		double X = xyz[0] / illuminantX;
		double Y = xyz[1] / illuminantY;
		double Z = xyz[2] / illuminantZ;

		if (X > 0.008856)
			X = Math.pow(X, third);
		else
			X = (7.787 * X) + (16 / 116);
		if (Y > 0.008856)
			Y = Math.pow(Y, third);
		else
			Y = (7.787 * Y) + (16 / 116);
		if (Z > 0.008856)
			Z = Math.pow(Z, third);
		else
			Z = (7.787 * Z) + (16 / 116);

		return new double[] { (116 * Y) - 16, 500 * (X - Y), 200 * (Y - Z) };
	}

	/**
	 * 
	 * @param lab [L,A,B]
	 * @return [R,G,B] where values are 0...1.0
	 */
	public static double[] lab2rgb(double[] lab) {
		return XYZ.xyz2rgb(lab2xyz(lab));
	}

	public static double[] lab2rgbQuick(double[] lab) {
		return XYZ.xyz2rgbQuick(lab2xyz(lab));
	}

	public static double[] lab2rgbVeryQuick(double[] lab) {
		return XYZ.xyz2rgbVeryQuick(lab2xyz(lab));
	}

	private static double[] lab2xyz(double[] lab) {
		double cache = lab[1];

		lab[0] = cache * 0.002 + (lab[1] = (lab[0] + 16) * 0.0086207);
		lab[2] = lab[1] - lab[2] * 0.005;

		if (lab[1] > 0.2069) { // 6/29
			lab[1] = lab[1] * lab[1] * lab[1];
		} else {
			lab[1] = (lab[1] - 16) * 14.8966;
		}

		if (lab[0] > 0.2069) {
			lab[0] = lab[0] * lab[0] * lab[0];
		} else {
			lab[0] = (lab[0] - 16) * 14.8966;
		}

		if (lab[2] > 0.2069) {
			lab[2] = lab[2] * lab[2] * lab[2];
		} else {
			lab[2] = (lab[2] - 16) * 14.8966;
		}

		lab[0] *= illuminantX;
		lab[1] *= illuminantY;
		lab[2] *= illuminantZ;
		return lab;
	}

}
