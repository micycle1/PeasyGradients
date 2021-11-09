package micycle.peasygradients.colorspace;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests forward-backward color conversion from RGB across all color spaces.
 * 
 * @author Michael Carleton
 *
 */
class ColorSpaceTests {

	private static final double DELTA = 0.05;

	@Test
	void test_DIN99() {
		test(new DIN99());
	}

	@Test
	void test_HSB() {
		test(new HSB());
	}

	@Test
	void test_HUNTERLAB() {
		test(new HUNTER_LAB());
	}

	@Test
	void test_IPT() {
		test(new IPT());
	}

	@Test
	void test_ITP() {
		test(new ITP());
	}

	@Test
	void test_JAB() {
		test(new JAB());
	}

	@Test
	void test_LAB() {
		test(new LAB());
	}

	@Test
	void test_LUV() {
		test(new LUV());
	}

	@Test
	void test_RGB() {
		test(new RGB());
	}

//	@Test
//	void test_RYB() {
//		test(new RYB()); // NOTE failing
//	}

	@Test
	void test_SRLAB2() {
		test(new SRLAB2());
	}

	@Test
	void test_XYB() {
		test(new XYB());
	}

	@Test
	void test_XYZ() {
		test(new XYZ());
	}

	private static void test(ColorSpace space) {
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
