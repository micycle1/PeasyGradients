package micycle.peasygradients.gradient;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import micycle.peasygradients.colorspace.ColorSpace;
import micycle.peasygradients.utilities.ColorUtils;
import micycle.peasygradients.utilities.Functions;
import processing.core.PApplet;

/**
 * A container for color (in every color space) and the percentage position that
 * it occurs within a gradient. Gradients comprise multiple color stops.
 * 
 * @author Michael Carleton
 *
 */
public final class ColorStop implements Comparable<ColorStop> {

	static final float TOLERANCE = 0.05f;

//	float originalPercent; // percent at which this stop occurs (0...1.0)
	float position; // percent, taking into account animation offset, etc.

	int clr; // 32bit ARGB color int
	int alpha; // 0-255 alpha

	private Map<ColorSpace, double[]> colorsMap;
	double[] colorOut;

	private ColorSpace colorSpace; // used to update colorOut when color is changed

	/**
	 * 
	 * @param clr      color int (32bit ARGB)
	 * @param fraction decimal fraction between 0...1 (otherwise constrained) that
	 *                 defines how far along the gradient the color defined by this
	 *                 stop is at.
	 */
	public ColorStop(int clr, float fraction) {
		position = fraction > 1 ? 1 : fraction < 0 ? 0 : fraction; // constrain 0...1
		colorsMap = new EnumMap<>(ColorSpace.class);
		setColor(clr);
	}

	/**
	 * Computes this color stop's value in every color space.
	 * 
	 * @param color 32bit ARGB
	 */
	void setColor(int color) {
		colorsMap.clear();
		this.clr = color;
		this.alpha = (color >> 24) & 0xff;

		final double[] clrRGBDouble = ColorUtils.decomposeclrDouble(color);

		for (int i = 0; i < ColorSpace.SIZE; i++) {
			// clone passed array just in case color space implementation mutates the array
			colorsMap.put(ColorSpace.get(i), ColorSpace.get(i).getColorSpace().fromRGB(clrRGBDouble.clone()));
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
	public double[] getColor(ColorSpace colorSpace) {
		return colorsMap.get(colorSpace);
	}

	/**
	 * Sets colorStop colorOut to value from given colorSpace (parent gradient).
	 * 
	 * @param colorSpace
	 */
	void setColorSpace(ColorSpace colorSpace) {
		this.colorSpace = colorSpace;
		colorOut = colorsMap.get(colorSpace);
	}

	protected void mutate(float amt) {
		// TODO fix with amt < 1
		// TODO use noise function for better / more natural variance
		float[] decomposed = ColorUtils.decomposeclrRGB(clr);
		for (int i = 0; i < decomposed.length - 1; i++) {
			decomposed[i] = PApplet.constrain(decomposed[i] + (Functions.randomFloat() < 0.5 ? -1 : 1) * amt, 0, 255);
		}
		setColor(ColorUtils.composeclr255(decomposed));
	}

	/**
	 * Enables color stops to be sorted by Collections.sort via pairwise comparison
	 * on the percent of each stop.
	 */
	@Override
	public int compareTo(ColorStop other) {
		return Float.compare(this.position, other.position);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof ColorStop) {
			ColorStop other = (ColorStop) obj;
			return (other.position == position && Arrays.equals(other.colorOut, colorOut));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Arrays.hashCode(colorOut);
	}

	@Override
	public String toString() {
		return Arrays.toString(ColorUtils.composeclrTo255(ColorUtils.decomposeclrDouble(clr)));
	}

}