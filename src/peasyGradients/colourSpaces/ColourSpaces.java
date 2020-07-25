package peasyGradients.colourSpaces;

/**
 * Defines how gradient should interpolate between colors (by performing
 * interpolation on different color spaces).
 * 
 * https://www.easyrgb.com/en/math.php TODO
 * https://github.com/tompazourek/Colourful
 * 
 * @author micycle1
 *
 */
public enum ColourSpaces {

	RGB(new RGB()), XYZ(new XYZ()), LAB(new LAB()), DIN99(new DIN99()), ITP(new ITP()), HLAB(new HUNTER_LAB()), LUV(new LUV()),
	JAB(new JAB()), RYB(new RYB()), HSB(new HSB());

//	XYZ_FAST, , FAST_LAB,, VERY_FAST_LAB, , ITP_FAST, , HUNTER_LAB_FAST, , FAST_LUV, , JAB_FAST, HSB_LONG, TEMP;

	public static final int size = values().length;

	private final static ColourSpaces[] vals = values();

	private ColourSpace instance;

	ColourSpaces(ColourSpace instance) {
		this.instance = instance;
	}

	public static ColourSpaces get(int index) {
		return vals[index];
	}

	/**
	 * Returns underlying instance of colourspace bound to the enum.
	 * 
	 * @return
	 */
	public ColourSpace getColourSpace() {
		return instance;
	}

	public ColourSpaces next() {
		return vals[(ordinal() + 1) % vals.length];
	}

	public ColourSpaces prev() {
		return vals[Math.floorMod((ordinal() - 1), vals.length)];
	}
}
