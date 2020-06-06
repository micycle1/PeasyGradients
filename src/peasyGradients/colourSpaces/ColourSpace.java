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
public enum ColourSpace {

	RGB, YCoCg, HCG, HSB_SHORT, HSB_LONG, LUV, FAST_LUV, JAB, JAB_FAST, XYZ, ITP, ITP_FAST, LAB, FAST_LAB,
	VERY_FAST_LAB, HUNTER_LAB, HUNTER_LAB_FAST, TEMP, RYB, YUV;

	private final static ColourSpace[] vals = values();

	public ColourSpace next() {
		return vals[(ordinal() + 1) % vals.length];
	}

	public ColourSpace prev() {
		return vals[Math.floorMod((ordinal() - 1), vals.length)];
	}

}
