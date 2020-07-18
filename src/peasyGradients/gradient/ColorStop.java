package peasyGradients.gradient;

import java.util.Arrays;

import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.Functions;

/**
 * A container for colour (in every colour space) and the percentage position
 * that it occurs within gradient.
 * 
 * @author micycle1
 *
 */
public final class ColorStop implements Comparable<ColorStop> {

	static final float TOLERANCE = 0.05f;

//	float originalPercent; // percent at which this stop occurs (0...1.0)
	float percent; // percent, taking into account animation offset, etc.

	int clr; // colour int

	float[] clrRGB; // decomposed RGB colour
	float[] clrHSB; // decomposed HSB colour
	float[] clrHCG;
	float[] clrRYB;
	float clrTEMP;
	double[] clrLAB; // decomposed LAB colour
	double[] clrLUV;
	double[] clrHLAB;
	double[] clrXYZ;
	double[] clrXYZ_FAST;
	double[] clrJAB;
	double[] clrITP;

	/**
	 * 
	 * @param clr      color int (bit shifted ARGB)
	 * @param fraction decimal fraction between 0...1 (otherwise constrained)
	 *                 defining how far along the gradient the colour defined by
	 *                 this stop is at.
	 */
	public ColorStop(int clr, float fraction) {
		percent = fraction > 1 ? 1 : fraction < 0 ? 0 : fraction; // constrain 0...1
		setColor(clr);
	}

	void setColor(int color) {
		this.clr = color;
		clrRGB = Functions.decomposeclr(clr);

		final double[] clrRGBDouble = Functions.decomposeclrDouble(clr);

		clrHSB = RGB.rgbToHsb(clr);
		clrLAB = CIE_LAB.rgb2lab(clrRGBDouble);
		clrXYZ = XYZ.rgb2xyz(clrRGBDouble);
		clrXYZ_FAST = XYZ_FAST.rgb2xyz(clrRGBDouble);
		clrLUV = LUV.rgb2luv(clrRGBDouble);
		clrHLAB = HUNTER_LAB.rgb2hlab(clrRGBDouble);
		clrTEMP = TEMP.rgb2temp(Functions.decomposeclrRGB(clr));
		clrRYB = RYB.rgb2ryb(Functions.decomposeclrRGBA(clr));
		clrJAB = JAB.rgb2jab(clrRGBDouble);
		clrITP = ITP.rgb2itp(clrRGBDouble);
	}

	/**
	 * Mandated by the interface Comparable<ColorStop>. Permits color stops to be
	 * sorted by Collections.sort via pairwise comparison.
	 */
	public int compareTo(ColorStop cs) {
		return percent > cs.percent ? 1 : percent < cs.percent ? -1 : 0;
	}

	@Override
	public String toString() {
		return Arrays.toString(Functions.composeclrTo255(Functions.decomposeclrDouble(clr)));
	}

	static boolean approxPercent(ColorStop cs, float tolerance) {
//		return Math.abs(cs.percent - cs.percent) < tolerance;
		return false;
	}

}