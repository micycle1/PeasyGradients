package micycle.peasygradients.colorspace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import micycle.peasygradients.PeasyGradients;
import micycle.peasygradients.gradient.Gradient;
import micycle.peasygradients.utilities.ColorUtils;
import processing.core.PConstants;
import processing.core.PImage;

/**
 * Tests to ensure 2D gradients are rendered as expected.
 */
class PeasyGradientsTests {

	private static final int WHITE = ColorUtils.RGB255ToRGB255(255, 255, 255);
	private static final int GREY = ColorUtils.RGB255ToRGB255(128, 128, 128);
	private static final int BLACK = ColorUtils.RGB255ToRGB255(0, 0, 0);

	@Test
	void testLinearGradient() {
		PImage g = new PImage(100, 1);
		PeasyGradients pg = new PeasyGradients(g);

		// test all values same
		Gradient gradient = new Gradient(GREY, GREY);
		pg.linearGradient(gradient, 0);
		for (int i = 0; i < g.pixels.length; i++) {
			assertEquals(GREY, g.pixels[i]);
		}

		// test gradient rendered monotonically
		gradient = new Gradient(WHITE, BLACK);
		pg.linearGradient(gradient, PConstants.TWO_PI); // also tests ange=two_pi == angle=0
		float[] lastCol = new float[] { 256, 256, 256 };
		for (int i = 0; i < g.pixels.length; i++) {
			float[] col = ColorUtils.decomposeclrRGB(g.pixels[i]);
			assertTrue(col[0] <= lastCol[0], String.format("R: %s came after %s at %s", col[0], lastCol[0], i));
			assertTrue(col[1] <= lastCol[1], String.format("R: %s came after %s at %s", col[1], lastCol[1], i));
			assertTrue(col[2] <= lastCol[2], String.format("R: %s came after %s at %s", col[2], lastCol[2], i));
		}

		// test all columns equivalent in horizontal gradient
		g = new PImage(100, 100);
		pg.setRenderTarget(g); // test set render target
		for (int y = 0; y < g.height; y++) {
			int startingIndex = y * g.width;
			int columnValue = g.pixels[startingIndex];
			for (int x = 1; x < g.width; x++) {
				assertEquals(columnValue, g.pixels[startingIndex + x]);
			}
		}
	}

}
