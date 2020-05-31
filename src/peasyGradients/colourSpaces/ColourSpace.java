package peasyGradients.colourSpaces;

/**
 * Defines how gradient should interpolate between colors (by performining
 * interpolation on different color spaces).
 * 
 * @author micycle1
 *
 */
public enum ColourSpace {

	RGB, HSB_SHORT, HSB_LONG, LAB, FAST_LAB, LCH, HCG, TEMP, RYB;
	// LCH, HCL, CMYK, TODO

	private final static ColourSpace[] vals = values();

	public ColourSpace next() {
		return vals[(ordinal() + 1) % vals.length];
	}

}
