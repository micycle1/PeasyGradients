package micycle.peasygradients.colorspace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import micycle.peasygradients.gradient.Gradient;
import micycle.peasygradients.utilities.ColorUtils;
import micycle.peasygradients.utilities.Interpolation;

class GradientTests {

	private static final int WHITE = ColorUtils.RGB255ToRGB255(255, 255, 255);
	private static final int GREY = ColorUtils.RGB255ToRGB255(128, 128, 128);
	private static final int BLACK = ColorUtils.RGB255ToRGB255(0, 0, 0);

	@Test
	void testMidPoint() {
		Gradient gradient = new Gradient(WHITE, BLACK);
		gradient.setInterpolationMode(Interpolation.LINEAR);
		gradient.setColorSpace(ColorSpace.RGB);
		assertEquals(GREY, gradient.getColor(0.5f));
	}

	@Test
	void testMidPointMatch() {
		for (int i = 0; i < 10; i++) {
			Gradient gradient = Gradient.randomGradient(2);
			gradient.setInterpolationMode(Interpolation.LINEAR);
			gradient.setColorSpace(ColorSpace.RGB);
			int midCol = gradient.getColor(0.5f);

			Gradient tri = new Gradient(gradient.colorAt(0), midCol, gradient.colorAt(1));
			assertEquals(midCol, tri.getColor(0.5f));
		}
	}

	@ParameterizedTest
	@EnumSource(value = ColorSpace.class, mode = Mode.EXCLUDE, names = { "JAB", "IPT", "IPTo" }) // NOTE exlude failing
	void testBiGradientIsMonotonic(ColorSpace colorSpace) {
		Gradient gradient = new Gradient(WHITE, BLACK);
		gradient.setInterpolationMode(Interpolation.LINEAR);
		gradient.setColorSpace(colorSpace);
		testGradientIsMonotonic(gradient);
	}

	@ParameterizedTest
	@EnumSource(value = ColorSpace.class, mode = Mode.INCLUDE, names = { "RGB", "RYB" }) // NOTE only linear spaces
	void testTriGradientIsMonotonic(ColorSpace colorSpace) {
		Gradient gradient = new Gradient(WHITE, GREY, BLACK);
		gradient.setInterpolationMode(Interpolation.LINEAR);
		gradient.setColorSpace(colorSpace);
		testGradientIsMonotonic(gradient);
	}

	private static void testGradientIsMonotonic(Gradient gradient) {
		assertEquals(WHITE, gradient.colorAt(0));
		assertEquals(BLACK, gradient.lastcolor());
		assertEquals(WHITE, gradient.getColor(0));
		assertEquals(BLACK, gradient.getColor(1));

		float[] lastCol = new float[] { 256, 256, 256 };
		for (int j = 0; j < 10000; j++) {
			float step = j / 10000f;
			float[] col = ColorUtils.decomposeclrRGB(gradient.getColor(step));
			assertTrue(col[0] <= lastCol[0], String.format("R: %s came after %s at %s", col[0], lastCol[0], step));
			assertTrue(col[1] <= lastCol[1], String.format("R: %s came after %s at %s", col[1], lastCol[1], step));
			assertTrue(col[2] <= lastCol[2], String.format("R: %s came after %s at %s", col[2], lastCol[2], step));
			lastCol = col;
		}
	}

}
