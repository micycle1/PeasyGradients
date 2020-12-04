package peasyGradients.colorSpaces;

/**
 * Colorspaces are used to specify the color space in which a gradient's
 * colors are represented and affects the results of color interpolation.
 * 
 * @author micycle1
 *
 */
public enum ColorSpaces {

	RGB(new RGB()), XYZ(new XYZ()), LAB(new LAB()), DIN99(new DIN99()), ITP(new ITP()), HLAB(new HUNTER_LAB()), LUV(new LUV()),
	JAB(new JAB()), XYB(new XYB()), RYB(new RYB()), HSB(new HSB());

	public static final int size = values().length;

	private final static ColorSpaces[] vals = values();

	private ColorSpace instance;

	ColorSpaces(ColorSpace instance) {
		this.instance = instance;
	}

	public static ColorSpaces get(int index) {
		return vals[index];
	}

	/**
	 * Returns the underlying instance of colorspace bound to the enum.
	 */
	public ColorSpace getColorSpace() {
		return instance;
	}

	public ColorSpaces next() {
		return vals[(ordinal() + 1) % vals.length];
	}

	public ColorSpaces prev() {
		return vals[Math.floorMod((ordinal() - 1), vals.length)];
	}
}
