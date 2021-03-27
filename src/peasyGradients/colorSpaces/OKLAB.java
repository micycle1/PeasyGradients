package peasyGradients.colorSpaces;

/**
 * https://bottosson.github.io/posts/oklab/
 * https://github.com/tommyettinger/colorful-gdx/blob/master/colorful/src/main/java/com/github/tommyettinger/colorful/oklab/ColorTools.java
 * https://github.com/tommyettinger/colorful-gdx/blob/master/colorful-pure/src/main/java/com/github/tommyettinger/colorful/pure/oklab/ColorTools.java
 * <p>
 * Oklab is a very new color space that builds on the same foundation as IPT,
 * but seems to be better-calibrated for uniform lightness and colorfulness,
 * instead of just the emphasis on uniform hue that IPT has. Relative to IPT,
 * Oklab has a noticeably smaller range in chromatic channels (IPT's protan and
 * tritan can range past 0.8 or as low as 0.35, but the similar A and B channels
 * in Oklab don't stray past about 0.65 at the upper end, if that), but it does
 * this so the difference between two Oklab colors is just the Euclidean
 * distance between their components. A slight difference between Oklab and IPT
 * here is that IPT shrinks the chromatic channels to store their -1 to 1 range
 * in a color float's 0 to 1 range, then offsets the shrunken range from -0.5 to
 * 0.5, to 0 to 1; Oklab does not need to shrink the range, and only offsets it
 * in the same way (both just add 0.5).
 * 
 * @author micycle1
 *
 */
final class OKLAB implements ColorSpace {

	@Override
	public double[] toRGB(double[] color) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double[] fromRGB(double[] RGB) {
		// TODO Auto-generated method stub
		return null;
	}

}
