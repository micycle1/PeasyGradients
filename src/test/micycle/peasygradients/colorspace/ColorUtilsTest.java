package micycle.peasygradients.colorspace;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import micycle.peasygradients.utilities.ColorUtils;

class ColorUtilsTest {

	@Test
	void testComposeDecomposeClr() {
		float[] RGBA = { 0.5f, 0.4f, 0.3f, 0.2f };
		int color = ColorUtils.RGBA1ToRGBA255(RGBA); // to 255 ARGB
		float[] decomposed = ColorUtils.decomposeclr(color);
		assertArrayEquals(RGBA, decomposed, 1 / 255f);
	}

	@Test
	void testComposeDecomposeClr255() {
		float[] RGBA = { 128f, 100f, 80f, 50f };
		int color = ColorUtils.RGBA255ToRGBA255(RGBA);
		float[] decomposed = ColorUtils.decomposeclrRGB(color);
		assertArrayEquals(RGBA, decomposed, 1 / 255f);
	}

	@Test
	void testComposeDecomposeClrTo255() {
		double[] RGB = { 0.5, 0.4, 0.3 };
		int[] composed = ColorUtils.RGB1ToInt255(RGB);
		assertArrayEquals(new int[] { 128, 102, 77 }, composed);
	}

	@Test
	void testComposeDecomposeClrRGBA() {
		int color = ColorUtils.RGBA1ToRGBA255(0.5f, 0.4f, 0.3f, 0.2f); // 255 ARGB
		float[] decomposed = ColorUtils.decomposeclrRGBA(color);
		assertArrayEquals(new float[] { 0.5f, 0.4f, 0.3f, 0.2f }, decomposed, 1 / 255f);
	}

	@Test
	void testFloatVsDouble() {
		double[] d = { 0.0705882353, 0.2039215686, 0.3372549019 };
		float[] f = { 0.0705882353f, 0.2039215686f, 0.3372549019f };

		assertEquals(ColorUtils.RGB1ToRGB255(d), ColorUtils.RGB1ToRGBA255(f));
	}

	@Test
	public void testComposeclrClamp() {
		// Test case with normal values
		assertEquals(0xFF123456, ColorUtils.RGB1ToRGBA255Clamp(new double[] { 0.0705882353, 0.2039215686, 0.3372549019 }, 255));
		// Test case with values requiring clamping
		assertEquals(0xFF0000FF, ColorUtils.RGB1ToRGBA255Clamp(new double[] { -1.0, 0.0, 2.0 }, 255));
		// Test case with all zeros
		assertEquals(0x00000000, ColorUtils.RGB1ToRGBA255Clamp(new double[] { 0.0, 0.0, 0.0 }, 0));
	}
}