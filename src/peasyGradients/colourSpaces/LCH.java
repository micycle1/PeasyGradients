package peasyGradients.colourSpaces;

import net.jafama.FastMath;

 /**
  * CIELCh, not CIELCH
  * @author micycle1
  *
  */
public final class LCH {

	private static double RAD2DEG = 180 / FastMath.PI;
	private static double DEG2RAD = FastMath.PI / 180;

	public static double[] rgb2lch(float[] rgb) {
		double[] lab = LAB.rgb2lab(rgb);
		return lab2lch(lab);
	}

	private static double[] lab2lch(double[] lab) {
		final double c = FastMath.sqrt(lab[1] * lab[1] + lab[2] * lab[2]);
		double h = (FastMath.atan2(lab[2], lab[1]) * RAD2DEG + 360) % 360;

		if (FastMath.roundEven(c) * 10000 == 0) {
			h = 0; // todo?
		}
		return new double[] { lab[0], c, h };
	}

	public static int lch2rgb(double[] lch) {
		double[] lab = lch2lab(lch);
		return LAB.lab2rgb(lab);

	}

	private static double[] lch2lab(double[] lch) {
		final double h = lch[2] * DEG2RAD;
		return new double[] { lch[0], FastMath.cos(h) * lch[1], FastMath.sin(h) * lch[1] };
	}

}
