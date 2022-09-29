package micycle.peasygradients.colorspace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

import micycle.peasygradients.gradient.Gradient;
import micycle.peasygradients.utilities.ColorUtils;
import micycle.peasygradients.utilities.Interpolation;

class GradientTests {

	private static final int BLACK = ColorUtils.composeclr(0, 0, 0);
	private static final int WHITE = ColorUtils.composeclr(255, 255, 255);

	@ParameterizedTest
	@EnumSource(value = ColorSpaces.class, mode = Mode.EXCLUDE, names = { "JAB", "IPT" })
	void testGradientIsMonotonic(ColorSpaces colorSpace) {
		Gradient gradient = new Gradient(BLACK, WHITE);
		gradient.setInterpolationMode(Interpolation.LINEAR);
		gradient.setColorSpace(colorSpace);
		testGradientIsMonotonic(gradient);
	}

	private static void testGradientIsMonotonic(Gradient gradient) {
		assertEquals(BLACK, gradient.getColor(0));
		assertEquals(WHITE, gradient.getColor(1));
		assertEquals(BLACK, gradient.colorAt(0));
		assertEquals(WHITE, gradient.colorAt(1));

		float[] lastCol = new float[] { -1, -1, -1 };
		for (int j = 0; j < 10000; j++) {
			float step = j / 10000f;
			float[] col = ColorUtils.decomposeclrRGB(gradient.getColor(step));
			assertTrue(col[0] >= lastCol[0], String.format("R: %s came after %s at %s", col[0], lastCol[0], step));
			assertTrue(col[1] >= lastCol[1], String.format("R: %s came after %s at %s", col[1], lastCol[1], step));
			assertTrue(col[2] >= lastCol[2], String.format("R: %s came after %s at %s", col[2], lastCol[2], step));
			lastCol = col;
		}
	}

}
