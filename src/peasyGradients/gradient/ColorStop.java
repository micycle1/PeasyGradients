package peasyGradients.gradient;

import java.util.Arrays;
import java.util.HashMap;

import peasyGradients.colorSpaces.*;
import peasyGradients.utilities.Functions;
import processing.core.PApplet;

/**
 * A container for color (in every color space) and the percentage position
 * that it occurs within gradient.
 * 
 * @author micycle1
 *
 */
public final class ColorStop implements Comparable<ColorStop> {

	static final float TOLERANCE = 0.05f;

//	float originalPercent; // percent at which this stop occurs (0...1.0)
	float position; // percent, taking into account animation offset, etc.

	int clr; // 32bit ARGB color int
	int alpha; // 0-255 alpha

	private HashMap<ColorSpaces, double[]> colorsMap;
	double[] colorOut;

	private ColorSpaces colorSpace; // used to update colorOut when color is changed

	/**
	 * 
	 * @param clr      color int (32bit ARGB)
	 * @param fraction decimal fraction between 0...1 (otherwise constrained) that
	 *                 defines how far along the gradient the color defined by this
	 *                 stop is at.
	 */
	public ColorStop(int clr, float fraction) {
		position = fraction > 1 ? 1 : fraction < 0 ? 0 : fraction; // constrain 0...1
		colorsMap = new HashMap<>(ColorSpaces.size);
		setcolor(clr);
	}

	/**
	 * Compute the color value for all color spaces given a ARGB integer.
	 * 
	 * @param color 32bit ARGB
	 */
	void setcolor(int color) {
		colorsMap.clear();
		this.clr = color;
		this.alpha = (color >> 24) & 0xff;

		final double[] clrRGBDouble = Functions.decomposeclrDouble(color);

		for (int i = 0; i < ColorSpaces.size; i++) {
			colorsMap.put(ColorSpaces.get(i), ColorSpaces.get(i).getColorSpace().fromRGB(clrRGBDouble));
		}

		colorOut = colorsMap.get(colorSpace);
	}
	
	void setPosition(float position) {
		if (position < 0) {
			position += 1; // equivalent to floormod function
		}
		if (position > 1) { // 1 % 1 == 0, which we want to avoid
			position %= 1;
		}
		this.position = position;
	}

	/**
	 * Return the value of the colorstop in a given colorspace
	 * 
	 * @param colorSpace
	 * @return double[a, b, c] representing color in given colorspace
	 */
	double[] getcolor(ColorSpaces colorSpace) {
		return colorsMap.get(colorSpace);
	}

	/**
	 * Sets colorStop colorOut to value from given colorSpace (parent gradient).
	 * 
	 * @param colorSpace
	 */
	void setcolorSpace(ColorSpaces colorSpace) {
		this.colorSpace = colorSpace;
		colorOut = colorsMap.get(colorSpace);
	}

	protected void mutate(float amt) {
		// TODO fix with amt < 1
		// TODO use noise function for better / more natural variance
		float[] decomposed = Functions.decomposeclrRGB(clr);
		for (int i = 0; i < decomposed.length - 1; i++) {
			decomposed[i] = PApplet.constrain(decomposed[i] + (Functions.randomFloat() < 0.5 ? -1 : 1) * amt, 0, 255);
		}
		setcolor(Functions.composeclr255(decomposed));
	}

	/**
	 * Enables color stops to be sorted by Collections.sort via pairwise comparison
	 * on the percent of each stop.
	 */
	public int compareTo(ColorStop cs) {
		return position > cs.position ? 1 : position < cs.position ? -1 : 0;
	}

	@Override
	public String toString() {
		return Arrays.toString(Functions.composeclrTo255(Functions.decomposeclrDouble(clr)));
	}

}