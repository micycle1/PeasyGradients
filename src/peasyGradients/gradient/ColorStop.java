package peasyGradients.gradient;

import java.util.Arrays;

import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.Functions;

/**
 * A container for colour (in every colour space) and the percentage position
 * that it occurs within gradient.
 * 
 * TODO what happens if first stop not at zero?
 * 
 * @author micycle1
 *
 */
final class ColorStop implements Comparable<ColorStop> {

	static final float TOLERANCE = 0.05f;

	static boolean approxPercent(ColorStop cs, float tolerance) {
		return Math.abs(cs.percent - cs.percent) < tolerance;
	}

	float originalPercent; // percent at which this stop occurs (0...1.0)
	float percent; // percent, taking into account animation offset

	int clr; // colour int

	float[] clrRGB; // decomposed RGB colour
	float[] clrHSB; // decomposed HSB colour
	double[] clrLAB; // decomposed LAB colour
	double[] clrLUV;
	double[] clrHLAB;
	float[] clrHCG;
	float tempclr;
	float[] clrRYB;
	float[] clrYUV;
	double[] clrXYZ;
	double[] clrXYZ_FAST;
	double[] clrJAB;
	double[] clrYCoCg;
	double[] clrITP;

	/**
	 * 
	 * @param clr     color int (bit shifted ARGB)
	 * @param percent
	 */
	protected ColorStop(int clr, float percent) {
		this.originalPercent = percent > 1 ? 1 : percent < 0 ? 0 : percent; // constrain 0...1
		this.percent = originalPercent;
		setColor(clr);
	}

	void setColor(int color) {
		this.clr = color;
		clrRGB = Functions.decomposeclr(clr);

		double[] clrRGBDouble = Functions.decomposeclrDouble(clr);

		clrHSB = RGB.rgbToHsb(clr);
		clrHCG = HCG.rgb2hcg(Functions.decomposeclrRGB(clr));
		clrLAB = CIE_LAB.rgb2lab(clrRGBDouble);
		clrXYZ = XYZ.rgb2xyz(clrRGBDouble);
		clrXYZ_FAST = XYZ_FAST.rgb2xyz(clrRGBDouble);
		clrLUV = LUV.rgb2luv(clrRGBDouble);
		clrHLAB = HUNTER_LAB.rgb2hlab(clrRGBDouble);
		tempclr = TEMP.rgb2temp(Functions.decomposeclrRGB(clr));
		clrRYB = RYB.rgb2ryb(Functions.decomposeclrRGBA(clr));
		clrYUV = YUV.rgb2yuv(Functions.decomposeclrRGBA(clr));
		clrJAB = JAB.rgb2jab(clrRGBDouble);
		clrYCoCg = YCoCg.rgb2YCoCg(clrRGBDouble);
		clrITP = ITP.rgb2itp(clrRGBDouble);
	}

	// Mandated by the interface Comparable<ColorStop>.
	// Permits color stops to be sorted by Collections.sort via pairwise comparison.
	public int compareTo(ColorStop cs) {
		return percent > cs.percent ? 1 : percent < cs.percent ? -1 : 0;
	}

	@Override
	public String toString() {
		return Arrays.toString(Functions.composeclrTo255(Functions.decomposeclrDouble(clr)));
	}

}