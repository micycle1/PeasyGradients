package peasyGradients.gradient;

import java.util.Arrays;
import java.util.HashMap;

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

	int clr; // 32bit ARGB colour int
	int alpha; // 0-255 alpha

	private HashMap<ColourSpaces, double[]> coloursMap;
	double[] colorOut; // TODO current color space's raw colour to save hashmap lookup each time.

	/**
	 * 
	 * @param clr      color int (bit shifted ARGB)
	 * @param fraction decimal fraction between 0...1 (otherwise constrained)
	 *                 defining how far along the gradient the colour defined by
	 *                 this stop is at.
	 */
	public ColorStop(int clr, float fraction) {
		percent = fraction > 1 ? 1 : fraction < 0 ? 0 : fraction; // constrain 0...1
		coloursMap = new HashMap<>(ColourSpaces.size);
		setColor(clr);
	}

	/**
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
	 * @param colourSpace
	 */
	void setColourSpace(ColourSpaces colourSpace) {
		colorOut = coloursMap.get(colourSpace);
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