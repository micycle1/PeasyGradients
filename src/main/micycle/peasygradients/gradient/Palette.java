package micycle.peasygradients.gradient;

import micycle.peasygradients.colorspace.ColorSpace;
import micycle.peasygradients.utilities.ColorUtils;
import micycle.peasygradients.utilities.Functions;
import processing.core.PApplet;

/**
 * Provides methods for generating color palettes based on various color harmony
 * principles The palettes can be used for creating gradients and other
 * color-related visual elements.
 * 
 * @author Michael Carleton
 */
public final class Palette {

	/**
	 * TODO implement
	 * https://medialab.github.io/iwanthue/js/libs/chroma.palette-gen.js or
	 * https://medialab.github.io/iwanthue/ TODO preset spectrums:
	 * https://observablehq.com/@makio135/give-me-colors
	 */

	// Constants defining the minimum saturation, minimum brightness, and their
	// respective maximum variances.
	private static final float sMin = 0.75f; // min saturation
	private static final float bMin = 0.75f; // min brightness
	private static final float sVarMax = 0.1f; // max saturation variance
	private static final float bVarMax = 0.1f; // max brightness variance
	private static final double GRC = (Math.sqrt(5) + 1) / 2 - 1; // Golden ratio conjugate for hue distribution

	/**
	 * https://martin.ankerl.com/2009/12/09/how-to-create-random-colors-programmatically/
	 */

	/**
	 * Generates a complementary color palette consisting of two colors that are
	 * diametrically opposite on the color wheel.
	 * 
	 * @return An array of two colors, represented as ARGB integers.
	 */
	public static int[] complementary() {
		return generic(2, 1 / 2f);
	}

	/**
	 * Generates a triadic color palette consisting of three colors that are evenly
	 * spaced around the color wheel, creating a harmonious and balanced color
	 * scheme.
	 * 
	 * @return An array of three colors, represented as ARGB integers.
	 */
	public static int[] triadic() {
		return generic(3, 1 / 3f);
	}

	/**
	 * Generates a tetradic color palette consisting of four colors that are evenly
	 * spaced around the color wheel, offering a diverse range of hues.
	 * 
	 * @return An array of four colors, represented as ARGB integers.
	 */
	public static int[] tetradic() {
		return generic(4, 1 / 4f);
	}

	/**
	 * Generates a palette of randomly selected colors using the golden ratio for
	 * hue distribution, which tends to produce aesthetically pleasing results.
	 * 
	 * @param ncolors The number of random colors to generate.
	 * @return An array of colors, represented as ARGB integers.
	 */
	public static int[] randomcolors(int ncolors) {
		return generic(ncolors, GRC);
	}

	/**
	 * Generates a palette of completely random colors without any constraints on
	 * hue distribution, saturation, or brightness.
	 * 
	 * @param ncolors The number of random colors to generate.
	 * @return An array of colors, represented as ARGB integers.
	 * @see #randomcolors(int) For generating colors with similar saturation and
	 *      brightness.
	 */
	public static int[] randomRandomcolors(int ncolors) {
		int[] out = new int[ncolors];
		for (int i = 0; i < ncolors; i++) {
			out[i] = (255 << 24) | -Functions.randomInt(0, 2 << 24);
		}
		return out;
	}

	/**
	 * Generic method for generating a color palette with specified number of
	 * colors, incrementally adjusting the hue, and applying slight variations to
	 * saturation and brightness.
	 * 
	 * @param colors    The number of colors to generate in the palette.
	 * @param increment The hue increment between consecutive colors.
	 * @return An array of colors, represented as ARGB integers.
	 */
	private static int[] generic(int colors, double increment) {
		int[] out = new int[colors];
		float h = Functions.randomFloat(); // 0...1
		float s = Functions.random(sMin, 1);
		float b = Functions.random(bMin, 1);

		for (int i = 0; i < colors; i++) {
			double[] HSB = new double[] { h, PApplet.constrain(s + Functions.random(-sVarMax, sVarMax), sMin, 1),
					PApplet.constrain(b + Functions.random(-bVarMax, bVarMax), bMin, 1) };
			int color = ColorUtils.RGB1ToRGB255(ColorSpace.HSB.getColorSpace().toRGB(HSB));
			out[i] = color;
			h += increment;
			h %= 1;
		}
		return out;
	}

}
