package peasyGradients.gradient;

import peasyGradients.colourSpaces.ColourSpaces;
import peasyGradients.utilities.Functions;

import processing.core.PApplet;

/**
 * Generate random color palettes to use in gradients. Generates palettes in HSB
 * colour space and outputs to sRGB integers.
 * 
 * // TODO implement
 * https://medialab.github.io/iwanthue/js/libs/chroma.palette-gen.js or
 * https://medialab.github.io/iwanthue/
 * 
 * @author micycle1
 *
 */
public final class Palette {

	private static final float sMin = 0.75f; // min saturation
	private static final float bMin = 0.75f; // min brightness
	private static final float sVarMax = 0.1f; // max saturation variance
	private static final float bVarMax = 0.1f; // max brightness variance
	/**
	 * https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
	 */
	private static final float GRC = 0.618033988749895f; // golden ratio conjugate

	/**
	 * Generates a palette of two colours that are on opposite sides of the color
	 * wheel.
	 * 
	 * @return
	 */
	public static int[] complementary() {
		return generic(2, 1 / 2f);
	}

	/**
	 * Generates a palette of three colors that are evenly spaced on the color
	 * wheel.
	 * 
	 * @return
	 */
	public static int[] triadic() {
		return generic(3, 1 / 3f);
	}

	/**
	 * Generates a palette of four colors that are evenly spaced on the color wheel.
	 * 
	 * @return
	 */
	public static int[] tetradic() {
		return generic(4, 1 / 4f);
	}

	/**
	 * Generate a colour palette of n random colours. Colours are distributed using
	 * the golden ratio.
	 * 
	 * @param nColours
	 * @return
	 */
	public static int[] randomColors(int nColours) {
		return generic(nColours, GRC);
	}

	/**
	 * Generate a colour palette of n random colours. Colours are generated
	 * completely randomly.
	 * 
	 * @param nColours
	 * @return
	 * @see #randomColors(int) generates colours with similar saturation and
	 *      brightness
	 */
	public static int[] randomRandomColors(int nColours) {
		int[] out = new int[nColours];
		for (int i = 0; i < nColours; i++) {
			out[i] = -Functions.randomInt(0, 2 << 24);
		}
		return out;
	}

	/**
	 * Generate an n-colour palette by cycling hue and varying saturation and
	 * brightness a little.
	 * 
	 * @param colours
	 * @param increment
	 * @return array of colours, represented by ARGB integers
	 */
	private static int[] generic(int colours, float increment) {
		int[] out = new int[colours];
		float h = Functions.randomFloat(); // 0...1
		float s = Functions.random(sMin, 1);
		float b = Functions.random(bMin, 1);

		for (int i = 0; i < colours; i++) {

			double[] HSB = new double[] { h, PApplet.constrain(s + Functions.random(-sVarMax, sVarMax), sMin, 1),
					PApplet.constrain(b + Functions.random(-bVarMax, bVarMax), bMin, 1) };
			out[i] = Functions.composeclr(ColourSpaces.HSB.getColourSpace().toRGB(HSB));
			h += increment;
			h %= 1;
		}
		return out;
	}

}
