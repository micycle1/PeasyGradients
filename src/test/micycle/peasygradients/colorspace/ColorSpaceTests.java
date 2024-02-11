package micycle.peasygradients.colorspace;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

/**
 * Tests round-trip/forward-backward color conversion from RGB across all color
 * spaces.
 * 
 * @author Michael Carleton
 *
 */
class ColorSpaceTests {

	private static final double DELTA = 0.05;

	@ParameterizedTest
	@EnumSource(value = ColorSpace.class, mode = Mode.EXCLUDE, names = { "OKLAB", "RYB" })
	void testColorSpace(ColorSpace colorSpace) {
		test(colorSpace.getColorSpace());
	}

	/**
	 * Tests forward<->backward conversion for a given colorspace across many random
	 * colors.
	 */
	private static void test(ColorSpaceTransform space) {
		for (int i = 0; i <= 255; i++) { // test ascending greyscale
			final double[] rgb = new double[] { i / 255d, i / 255d, i / 255d };
			assertArrayEquals(rgb, space.toRGB(space.fromRGB(rgb)), DELTA);
		}
		for (int i = 0; i < 10000; i++) { // test random colors
			final double[] rgb = new double[] { Math.random(), Math.random(), Math.random() };
			assertArrayEquals(rgb, space.toRGB(space.fromRGB(rgb)), DELTA);
		}
	}
}
