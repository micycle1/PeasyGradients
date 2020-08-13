package peasyGradients.colourSpaces;

/**
 * Colourspaces are used to specify the colour space in which a gradient's
 * colours are interpolated.
 * 
 * @author micycle1
 *
 */
public enum ColourSpaces {

	RGB(new RGB()), XYZ(new XYZ()), LAB(new LAB()), DIN99(new DIN99()), ITP(new ITP()), HLAB(new HUNTER_LAB()), LUV(new LUV()),
	JAB(new JAB()), RYB(new RYB()), HSB(new HSB());

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
	 * Returns the underlying instance of colourspace bound to the enum.
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
