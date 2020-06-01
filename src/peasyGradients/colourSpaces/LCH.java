package peasyGradients.colourSpaces;

import net.jafama.FastMath;

/**
 * CIELCh, not CIELCH CIELCH (L*C*h°) Cylindrical representation of CIELAB
 * 
 * @author micycle1
 *
 */
public final class LCH {

	/**
	 * 
	 * @param rgb [R,G,B] 0...1
	 * @return
	 */
	public static double[] rgb2lch(double[] rgb) {
		double[] lab = CIE_LAB.rgb2lab(rgb);
		return lab2lch(lab);
	}

	private static double[] lab2lch(double[] lab) {

		final double c = FastMath.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);

		double h = FastMath.atan2(lab[2], lab[1]);

		if (h > 0) {
			h = (h / FastMath.PI) * 180;
		} else {
			h = 360 - (FastMath.abs(h) / FastMath.PI) * 180;
		}

		return new double[] { lab[0], c, h };
	}

	public static double[] lch2rgb(double[] lch) {
		double[] lab = lch2lab(lch);
		return CIE_LAB.lab2rgb(lab);

	}

	private static double[] lch2lab(double[] lch) {
		return new double[] { lch[0], lch[1] * FastMath.cos(FastMath.toRadians(lch[2])),
				lch[1] * FastMath.sin(FastMath.toRadians(lch[2])) };
	}

}
