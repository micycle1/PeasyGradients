package peasyGradients.utilities;

import peasyGradients.colourSpaces.HSB;
import processing.core.PApplet;

/**
 * Generate random color palettes to use gradients. Generates palettes in HSB
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
	private static final float sVarMax = 0.1f; // max variance
	private static final float bVarMax = 0.1f; // max variance
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

	private static int[] generic(int colours, float increment) {
		int[] out = new int[colours];
		float h = Functions.randomFloat();
		float s = Functions.random(sMin, 1);
		float b = Functions.random(bMin, 1);

		for (int i = 0; i < colours; i++) {
			out[i] = Functions
					.composeclr(HSB.hsbToRgb(h, PApplet.constrain(s + Functions.random(-sVarMax, sVarMax), sMin, 1),
							PApplet.constrain(b + Functions.random(-bVarMax, bVarMax), bMin, 1), 1));
			h += increment;
			h %= 1;
		}
		return out;
	}

}
