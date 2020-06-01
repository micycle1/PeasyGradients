package peasyGradients;

import peasyGradients.colourSpaces.HCG;
import peasyGradients.colourSpaces.HSB;
import peasyGradients.colourSpaces.CIE_LAB;
import peasyGradients.colourSpaces.LCH;
import peasyGradients.colourSpaces.RGB;
import peasyGradients.colourSpaces.RYB;
import peasyGradients.colourSpaces.TEMP;
import peasyGradients.colourSpaces.XYZ;
import peasyGradients.colourSpaces.YUV;
import peasyGradients.utilities.Functions;
import processing.core.PApplet;
import processing.core.PConstants;

/**
 * A Gradient class is little more than a glorified list of ColorStops.
 * 
 * @author micycle1
 *
 */
final class ColorStop implements Comparable<ColorStop> {

	static final float TOLERANCE = 0.05f;

	final float originalPercent; // percent at which this stop occurs (0...1.0)
	float percent; // percent, taking into account animation offset
	final int clr; // colour int
	float[] clrRGB; // decomposed RGB colour
	final float[] clrHSB; // decomposed HSB colour
	final double[] clrLAB; // decomposed LAB colour
	final double[] clrLCH;
	final float[] ckrHCG;
	final float tempclr;
	final float[] clrRYB;
	final float[] clrYUV;
	final double[] clrXYZ;

	protected ColorStop(int colorMode, float percent, float[] arr) {
		this(colorMode, percent, arr[0], arr[1], arr[2], arr.length == 4 ? arr[3] : 1.0f);
	}

	protected ColorStop(int colorMode, float percent, float x, float y, float z, float w) {
		this(percent, colorMode == PConstants.HSB ? Functions.composeclr(HSB.hsbToRgb(x, y, z, w))
				: Functions.composeclr(x, y, z, w));
	}

	protected ColorStop(float percent, int clr) {
		this.originalPercent = PApplet.constrain(percent, 0.0f, 1.0f);
		this.clr = clr;
		clrRGB = new float[4];
		Functions.decomposeclr(clr, clrRGB);
		clrHSB = RGB.rgbToHsb(clr);
		ckrHCG = HCG.rgb2hcg(Functions.decomposeclrRGB(clr));
		clrLAB = CIE_LAB.rgb2lab(Functions.decomposeclrDouble(clr));
		clrLCH = LCH.rgb2lch(Functions.decomposeclrDouble(clr));
		tempclr = TEMP.rgb2temp(Functions.decomposeclrRGB(clr));
		clrRYB = RYB.rgb2ryb(Functions.decomposeclrRGBA(clr));
		clrYUV = YUV.rgb2yuv(Functions.decomposeclrRGBA(clr));
		clrXYZ = XYZ.rgb2xyz(Functions.decomposeclrDouble(clr));
		this.percent = originalPercent;
	}

	static boolean approxPercent(ColorStop cs, float tolerance) {
		return PApplet.abs(cs.percent - cs.percent) < tolerance;
	}

	// Mandated by the interface Comparable<ColorStop>.
	// Permits color stops to be sorted by Collections.sort via pairwise comparison.
	public int compareTo(ColorStop cs) {
		return percent > cs.percent ? 1 : percent < cs.percent ? -1 : 0;
	}

	@Override
	public String toString() {
		return clr + " at " + percent;
	}

}