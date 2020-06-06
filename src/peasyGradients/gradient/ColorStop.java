package peasyGradients.gradient;

import peasyGradients.colourSpaces.CIE_LAB;
import peasyGradients.colourSpaces.HCG;
import peasyGradients.colourSpaces.HSB;
import peasyGradients.colourSpaces.HUNTER_LAB;
import peasyGradients.colourSpaces.ITP;
import peasyGradients.colourSpaces.JAB;
import peasyGradients.colourSpaces.LUV;
import peasyGradients.colourSpaces.RGB;
import peasyGradients.colourSpaces.RYB;
import peasyGradients.colourSpaces.TEMP;
import peasyGradients.colourSpaces.XYZ;
import peasyGradients.colourSpaces.YCoCg;
import peasyGradients.colourSpaces.YUV;

import peasyGradients.utilities.Functions;

import processing.core.PApplet;
import processing.core.PConstants;

/**
 * A container for colour (in every colour space) and the percentage position
 * that it occurs within gradient.
 * 
 * @author micycle1
 *
 */
final class ColorStop implements Comparable<ColorStop> {

	static final float TOLERANCE = 0.05f;

	static boolean approxPercent(ColorStop cs, float tolerance) {
		return PApplet.abs(cs.percent - cs.percent) < tolerance;
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
	double[] clrJAB;
	double[] clrYCoCg;
	double[] clrITP;

	/**
	 * 
	 * @param percent
	 * @param clr     color int (bit shifted ARGB)
	 */
	protected ColorStop(float percent, int clr) {
		this.originalPercent = PApplet.constrain(percent, 0.0f, 1.0f);
		this.percent = originalPercent;
		setColor(clr);
	}

	protected ColorStop(int colorMode, float percent, float x, float y, float z, float w) {
		this(percent, colorMode == PConstants.HSB ? Functions.composeclr(HSB.hsbToRgb(x, y, z, w))
				: Functions.composeclr(x, y, z, w));
	}

	protected ColorStop(int colorMode, float percent, float[] arr) {
		this(colorMode, percent, arr[0], arr[1], arr[2], arr.length == 4 ? arr[3] : 1.0f);
	}

	void setColor(int color) {
		this.clr = color;
		clrRGB = Functions.decomposeclr(clr);

		double[] clrRGBDouble = Functions.decomposeclrDouble(clr);

		clrHSB = RGB.rgbToHsb(clr);
		clrHCG = HCG.rgb2hcg(Functions.decomposeclrRGB(clr));
		clrLAB = CIE_LAB.rgb2lab(clrRGBDouble);
		clrXYZ = XYZ.rgb2xyz(clrRGBDouble);
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
		return clr + " at " + percent;
	}

}