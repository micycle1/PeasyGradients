package peasyGradients.colourSpaces;

/**
 * Defines how gradient should interpolate between colors (by performining
 * interpolation on different color spaces).
 * 
 * https://www.easyrgb.com/en/math.php
 * TODO https://github.com/tompazourek/Colourful
 * 
 * @author micycle1
 *
 */
public enum ColourSpace {

	RGB, HSB_SHORT, HSB_LONG, LUV, FAST_LUV, JAB, LAB, FAST_LAB, VERY_FAST_LAB, HUNTER_LAB, LCH, HCG, TEMP, RYB, XYZ, YUV;
	// LCH, HCL, xyz, CMYK, TODO

	private final static ColourSpace[] vals = values();

	public ColourSpace next() {
		return vals[(ordinal() + 1) % vals.length];
	}

}
