package micycle.peasygradients.colorspace;

/**
 * Represents the different color spaces that can be used for color gradients.
 * Each color space has its own way of representing colors and this affects how
 * colors are interpolated within the gradient.
 * <p>
 * Additionally, utility methods are provided to navigate through the enum
 * values, allowing one to easily cycle through the various color spaces.
 * 
 * @author Michael Carleton
 */
public enum ColorSpace {

	RGB(new RGB()), XYZ(new XYZ()), LAB(new LAB()), DIN99(new DIN99()), ITP(new ITP()), HLAB(new HUNTER_LAB()),
	SRLAB2(new SRLAB2()), LUV(new LUV()), OKLAB(new OKLAB()), JAB(new JAB()), XYB(new XYB()), IPT(new IPT()), RYB(new RYB()),
	HSB(new HSB());

	public static final int SIZE = values().length;

	private static final ColorSpace[] vals = values();

	private ColorSpaceTransform instance;

	ColorSpace(ColorSpaceTransform instance) {
		this.instance = instance;
	}

	/**
	 * Retrieves a {@code ColorSpace} based on its ordinal index.
	 * 
	 * @param index the ordinal index of the color space
	 * @return the {@code ColorSpace} at the specified index
	 */
	public static ColorSpace get(int index) {
		return vals[index];
	}

	/**
	 * Returns the instance of {@link ColorSpaceTransform} associated with the color
	 * space. This instance provides the methods for converting to and from the RGB
	 * color space.
	 * 
	 * @return the {@link ColorSpaceTransform} instance bound to this color space
	 */
	public ColorSpaceTransform getColorSpace() {
		return instance;
	}

	/**
	 * Returns the next color space in the sequence. This method wraps around to the
	 * first element after the last one.
	 * 
	 * @return the next {@code ColorSpace} in the sequence
	 */
	public ColorSpace next() {
		return vals[(ordinal() + 1) % vals.length];
	}

	/**
	 * Returns the previous color space in the sequence. This method wraps around to
	 * the last element after the first one.
	 * 
	 * @return the previous {@code ColorSpace} in the sequence
	 */
	public ColorSpace prev() {
		return vals[Math.floorMod((ordinal() - 1), vals.length)];
	}
}
