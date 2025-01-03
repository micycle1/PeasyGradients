package micycle.peasygradients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import micycle.peasygradients.PeasyGradients;
import micycle.peasygradients.gradient.Gradient;
import micycle.peasygradients.gradient.Palette;
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
		for (int pixel : g.pixels) {
			assertEquals(GREY, pixel);
		}

		// test gradient rendered monotonically
		gradient = new Gradient(WHITE, BLACK);
		pg.linearGradient(gradient, PConstants.TWO_PI); // also tests that angle=two_pi == angle=0
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

	@ParameterizedTest
	@ValueSource(doubles = { 0, 2 * Math.PI, PConstants.TWO_PI })
	void testLinearGradientVerticalDivision(double angle) {
		/*
		 * Test the linear gradient division. Each half (with posterisation=2) should be
		 * each respective color.
		 */
		int w = 100;
		int h = 100;
		PImage g = new PImage(w, h);
		PeasyGradients pg = new PeasyGradients(g);
		pg.posterise(2);

		int colA = WHITE;
		int colB = BLACK;
		Gradient gradient = new Gradient(colA, colB);
		pg.linearGradient(gradient, angle);

		for (int x = 0; x < w / 2; x++) {
			for (int y = 0; y < h; y++) {
				assertEquals(colA, g.pixels[y * w + x], "Failed at x=" + x + ", y=" + y);
			}
		}
		for (int x = w / 2; x < w; x++) {
			for (int y = 0; y < h; y++) {
				assertEquals(colB, g.pixels[y * w + x], "Failed at x=" + x + ", y=" + y);
			}
		}

	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 2, 3, 5, 50 })
	void testPosterise(int n) {
		PImage g = new PImage(1000, 1000);
		PeasyGradients pg = new PeasyGradients(g);
		pg.posterise(n);

		Gradient gradient = new Gradient(Palette.tetradic());
		pg.linearGradient(gradient, 0);

		assertEquals(n, makeUnique(g.pixels).length);
	}

	private static int[] makeUnique(int... values) {
		return Arrays.stream(values).distinct().toArray();
	}

}
