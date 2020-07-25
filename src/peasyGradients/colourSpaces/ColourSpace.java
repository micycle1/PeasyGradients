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

	RGB(), XYZ, XYZ_FAST, LAB, FAST_LAB, DIN99, VERY_FAST_LAB, ITP, ITP_FAST, HUNTER_LAB, HUNTER_LAB_FAST, LUV, FAST_LUV, JAB, JAB_FAST, RYB,
	HSB_SHORT, HSB_LONG, TEMP;

	public static final int size = values().length;
	
	private final static ColourSpace[] vals = values();
	
//	private colorspaceclass class;
	
//	ColourSpace(Class<?> clazz) {
//		// TODO Auto-generated constructor stub
	// RGB(RGB.class),
	// this.class = clazz
//	return clazz
//	}

	public static ColourSpace get(int index) {
		return vals[index];
	}

	public ColourSpace next() {
		return vals[(ordinal() + 1) % vals.length];
	}

	public ColourSpace prev() {
		return vals[Math.floorMod((ordinal() - 1), vals.length)];
	}
}
