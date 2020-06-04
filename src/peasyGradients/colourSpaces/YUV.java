package peasyGradients.colourSpaces;

/**
 * YUV is a color space typically used for color image/video processing. It
 * encodes a color image/video taking into account properties of the human eye
 * that allow for reduced bandwidth for chroma components without perceptual
 * distortion. aka YCbCr (a scaled and offset version of the YUV color space for
 * digital)
 * https://stackoverflow.com/questions/17892346/how-to-convert-rgb-yuv-rgb-both-ways
 * https://www.vocal.com/video/rgb-and-yuv-color-space-conversion/
 * @author micycle1
 *
 */
public final class YUV {

	/**
	 * YUVA 0...1
	 * 
	 * @param yuva
	 * @return
	 */
	public static float[] yuv2rgb(float[] yuva) {
		yuva[0] -= 16;
		yuva[1] -= 128;
		yuva[2] -= 128;
		final float R = 1.164f * yuva[0] + 1.596f * yuva[2];
		final float G = 1.164f * yuva[0] - 0.392f * yuva[1] - 0.813f * yuva[2];
		final float B = 1.164f * yuva[0] + 2.017f * yuva[1];
		return new float[] { R, G, B, yuva[3] };
	}

	/**
	 * sRGB 0...1
	 * 
	 * @return
	 */
	public static float[] rgb2yuv(final float[] rgba) {
		final float Y = 0.257f * rgba[0] + 0.504f * rgba[1] + 0.098f * rgba[2] + 16;
		final float U = -0.148f * rgba[0] - 0.291f * rgba[1] + 0.439f * rgba[2] + 128;
		final float V = 0.439f * rgba[0] - 0.368f * rgba[1] - 0.071f * rgba[2] + 128;
		return new float[] { Y, U, V, rgba[3] };
	}

}
