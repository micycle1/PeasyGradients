package peasyGradients;

import peasyGradients.colourSpaces.HSB;
import processing.core.PApplet;

/**
 * Generate random color palettes to use gradients. Generates palettes in HSB
 * colour space and outputs to sRGB integers.
 * 
 * @author micycle1
 *
 */
public final class Palette {

	private static final float sMin = 0.5f; // min saturation
	private static final float bMin = 0.5f; // min brightness
	private static final float sVarMax = 0.1f; // max variance
	private static final float bVarMax = 0.1f; // max variance
	private static final float GRC = 0.618033988749895f; // golden ratio conjugate

	/**
	 * Two colors that are on opposite sides of the color wheel.
	 * 
	 * @return
	 */
	static int[] complementary() {
		return generic(2, 1 / 2f);
	}

	/**
	 * Three colors that are evenly spaced on the color wheel.
	 * 
	 * @return
	 */
	static int[] triadic() {
		return generic(3, 1 / 3f);
	}

	/**
	 * Four colors that are evenly spaced on the color wheel.
	 * 
	 * @return
	 */
	static int[] tetradic() {
		return generic(4, 1 / 4f);
	}

	/**
	 * Generate a colour palette of n random colours. Colours are distributed using
	 * the golden ratio.
	 * 
	 * @param nColours
	 * @return
	 */
	static int[] randomColors(int nColours) {
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
