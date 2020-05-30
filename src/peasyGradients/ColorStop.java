package peasyGradients;

import peasyGradients.colourSpaces.HSB;
import peasyGradients.colourSpaces.LAB;
import peasyGradients.colourSpaces.RGB;
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
	
	final float percent; // percent at which this stop occurs (0...1.0)
	final int clr; // colour int
	float[] clrRGB; // decomposed RGB colour
	final float[] clrHSB; // decomposed HSB colour
	final double[] labclr; // decomposed LAB colour

	protected ColorStop(int colorMode, float percent, float[] arr) {
		this(colorMode, percent, arr[0], arr[1], arr[2], arr.length == 4 ? arr[3] : 1.0f);
	}

	protected ColorStop(int colorMode, float percent, float x, float y, float z, float w) {
		this(percent, colorMode == PConstants.HSB ? Functions.composeclr(HSB.hsbToRgb(x, y, z, w))
				: Functions.composeclr(x, y, z, w));
	}

	protected ColorStop(float percent, int clr) {
		this.percent = PApplet.constrain(percent, 0.0f, 1.0f);
		this.clr = clr;
		clrRGB = new float[4];
		Functions.decomposeclr(clr, clrRGB);
		clrHSB = RGB.rgbToHsb(clr);
		labclr = LAB.rgb2lab(Functions.decomposeclr(clr));
		// TODO additional color modes
	}

	boolean approxPercent(ColorStop cs, float tolerance) {
		return PApplet.abs(percent - cs.percent) < tolerance;
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