package micycle.peasygradients.gradient;

import micycle.peasygradients.colorSpaces.ColorSpaces;
import micycle.peasygradients.utilities.ColorUtils;
import micycle.peasygradients.utilities.Functions;
import processing.core.PApplet;

/**
 * Generates random color palettes to use in gradients. Palettes are generated
 * in the HSB color space and output as sRGB integers.
 * 
 * @author micycle1
 *
 */
public final class Palette {

	/**
	 * TODO implement
	 * https://medialab.github.io/iwanthue/js/libs/chroma.palette-gen.js or
	 * https://medialab.github.io/iwanthue/ TODO preset spectrums:
	 * https://observablehq.com/@makio135/give-me-colors
	 */

	private static final float sMin = 0.75f; // min saturation
	private static final float bMin = 0.75f; // min brightness
	private static final float sVarMax = 0.1f; // max saturation variance
	private static final float bVarMax = 0.1f; // max brightness variance
	/**
	 * https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
	 */
	private static final float GRC = 0.618033988749895f; // golden ratio conjugate

	/**
	 * Generates a palette of two colors that are on opposite sides of the color
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
	 * Generate a color palette of n random colors. colors are distributed using the
	 * golden ratio.
	 * 
	 * @param ncolors number of random colors to generate
	 * @return array of colors, represented by (Processing-compatible) ARGB integers
	 */
	public static int[] randomcolors(int ncolors) {
		return generic(ncolors, GRC);
	}

	/**
	 * Generate a color palette of n random colors. colors are generated completely
	 * randomly.
	 * 
	 * @param ncolors
	 * @return
	 * @see #randomcolors(int) generates colors with similar saturation and
	 *      brightness
	 */
	public static int[] randomRandomcolors(int ncolors) {
		int[] out = new int[ncolors];
		for (int i = 0; i < ncolors; i++) {
			out[i] = -Functions.randomInt(0, 2 << 24);
		}
		return out;
	}

	/**
	 * Generate an n-color palette by cycling hue and varying saturation and
	 * brightness a little.
	 * 
	 * @param colors
	 * @param increment
	 * @return array of colors, represented by ARGB integers
	 */
	private static int[] generic(int colors, float increment) {
		int[] out = new int[colors];
		float h = Functions.randomFloat(); // 0...1
		float s = Functions.random(sMin, 1);
		float b = Functions.random(bMin, 1);

		for (int i = 0; i < colors; i++) {

			double[] HSB = new double[] { h, PApplet.constrain(s + Functions.random(-sVarMax, sVarMax), sMin, 1),
					PApplet.constrain(b + Functions.random(-bVarMax, bVarMax), bMin, 1) };
			out[i] = ColorUtils.composeclr(ColorSpaces.HSB.getColorSpace().toRGB(HSB));
			h += increment;
			h %= 1;
		}
		return out;
	}

}
