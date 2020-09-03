package peasyGradients.gradient;

import java.util.Arrays;
import java.util.HashMap;

import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.Functions;
import processing.core.PApplet;

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

	int clr; // 32bit ARGB colour int
	int alpha; // 0-255 alpha

	private HashMap<ColourSpaces, double[]> coloursMap;
	double[] colorOut;

	private ColourSpaces colourSpace; // used to update colorOut when colour is changed

	/**
	 * 
	 * @param clr      color int (32bit ARGB)
	 * @param fraction decimal fraction between 0...1 (otherwise constrained) that
	 *                 defines how far along the gradient the colour defined by this
	 *                 stop is at.
	 */
	public ColorStop(int clr, float fraction) {
		percent = fraction > 1 ? 1 : fraction < 0 ? 0 : fraction; // constrain 0...1
		coloursMap = new HashMap<>(ColourSpaces.size);
		setColor(clr);
	}

	/**
	 * Compute the colour value for all colour spaces given a ARGB integer.
	 * 
	 * @param color 32bit ARGB
	 */
	void setColor(int color) {
		coloursMap.clear();
		this.clr = color;
		this.alpha = (color >> 24) & 0xff;

		final double[] clrRGBDouble = Functions.decomposeclrDouble(color);

		for (int i = 0; i < ColourSpaces.size; i++) {
			coloursMap.put(ColourSpaces.get(i), ColourSpaces.get(i).getColourSpace().fromRGB(clrRGBDouble));
		}

		colorOut = coloursMap.get(colourSpace);
	}

	/**
	 * Return the value of the colourstop in a given colourspace
	 * 
	 * @param colourSpace
	 * @return double[a, b, c] representing colour in given colourspace
	 */
	double[] getColor(ColourSpaces colourSpace) {
		return coloursMap.get(colourSpace);
	}

	/**
	 * Sets colourStop colourOut to value from given colourSpace (parent gradient).
	 * 
	 * @param colourSpace
	 */
	void setColourSpace(ColourSpaces colourSpace) {
		this.colourSpace = colourSpace;
		colorOut = coloursMap.get(colourSpace);
	}

	protected void mutate(float amt) {
		// TODO fix with amt < 1
		float[] decomposed = Functions.decomposeclrRGB(clr);
		for (int i = 0; i < decomposed.length - 1; i++) {
			decomposed[i] = PApplet.constrain(decomposed[i] + (Functions.randomFloat() < 0.5 ? -1 : 1) * amt, 0, 255);
		}
		setColor(Functions.composeclr255(decomposed));
	}

	/**
	 * Permits color stops to be sorted by Collections.sort via pairwise comparison
	 * on the percent of each stop.
	 */
	public int compareTo(ColorStop cs) {
		return percent > cs.percent ? 1 : percent < cs.percent ? -1 : 0;
	}

	@Override
	public String toString() {
		return Arrays.toString(Functions.composeclrTo255(Functions.decomposeclrDouble(clr)));
	}

}