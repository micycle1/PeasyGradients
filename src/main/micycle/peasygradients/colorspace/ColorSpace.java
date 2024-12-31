package micycle.peasygradients.colorspace;

/**
 * Represents the different color spaces that can be used for color gradients.
 * Each color space has its own way of representing colors and this affects the
 * nature of interpolated colors within a a gradient.
 * <p>
 * Additionally, utility methods are provided to navigate through the enum
 * values, allowing one to easily cycle through the various color spaces.
 * 
 * @author Michael Carleton
 */
public enum ColorSpace {

	/**
	 * RGB color space represents colors in terms of the intensity of Red, Green,
	 * and Blue light. It is widely used in digital imaging.
	 */
	RGB(new RGB()),
	/**
	 * RYB color space is based on the primary colors Red, Yellow, and Blue. It is
	 * often used in art and design education.
	 */
	RYB(new RYB()),
	/**
	 * HSB (Hue, Saturation, Brightness) color space defines colors in terms of
	 * their shade, intensity, and brightness, making it intuitive for human
	 * understanding.
	 */
	HSB(new HSB()),
	/**
	 * XYZ color space is a linear color space based on human vision, serving as a
	 * basis for many other color spaces. It defines colors in terms of X, Y, and Z
	 * coordinates.
	 */
	XYZ(new XYZ()),
	/**
	 * LAB color space describes colors in terms of Lightness, a (from green to
	 * red), and b (from blue to yellow), aiming for perceptual uniformity.
	 */
	LAB(new LAB()),
	/**
	 * HLAB (Hunter L, a, b) color space is designed for visual uniformity, offering
	 * improvements in the representation of yellow and blue colors compared to XYZ.
	 */
	HLAB(new HUNTER_LAB()),
	/**
	 * DLAB (DIN99) color space is designed for better uniformity in color
	 * differences, based on the CIELAB model with adjustments for human vision.
	 */
	DLAB(new DLAB()),
	/**
	 * SRLAB2 color space offers a balance between CIELAB's simplicity and
	 * CIECAM02's accuracy, aiming for practicality in color difference evaluation.
	 */
	SRLAB2(new SRLAB2()),
	/**
	 * Dolby ITP (IC<sub>T</sub>C<sub>P</sub>) color space focuses on high-fidelity
	 * HDR/WCG content, with dimensions for intensity (I), chroma (Ct), and
	 * protanopia (Cp).
	 */
	ITP(new ITP()),
	/**
	 * LUV color space emphasizes perceptual uniformity in lightness and
	 * chromaticity, based on the CIE 1976 L*, u*, v* formulas.
	 */
	LUV(new LUV()),
	/**
	 * IPT color space is designed for uniformity in perceived hue, with dimensions
	 * for lightness-darkness (I), red-green (P), and yellow-blue (T).
	 */
	IPT(new IPT()),
	IPTo(new IPTo()),
	/**
	 * Oklab color space is designed for uniform lightness and colorfulness based on
	 * a perceptual model, improving upon the IPT principles.
	 */
	OKLAB(new OKLAB()),
	/**
	 * JAB (J<sub>z</sub>A<sub>z</sub>B<sub>z</sub>) color space aims for perceptual
	 * uniformity in HDR/WCG environments, with dimensions for lightness (Jz),
	 * red-green (Az), and yellow-blue (Bz).
	 */
	JAB(new JAB()),
	/**
	 * Natural color mixing by treating colors as real-life pigments using the
	 * Kubelka &amp; Munk theory to predict realistic color behavior.
	 */
	KMUNK(new K_MUNK()),
	/**
	 * XYB color space, used in JPEG XL, focuses on perceptual uniformity with
	 * dimensions for red-green (X), yellow (Y), and blue (B).
	 */
	XYB(new XYB());

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
