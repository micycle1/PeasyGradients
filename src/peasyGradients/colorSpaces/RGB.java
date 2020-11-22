package peasyGradients.colorSpaces;

/***
 * Standard RGB (1996), not to be confused with CIERGB (1931).
 * 
 * @author micycle1
 *
 */
final class RGB implements ColorSpace {
	
	RGB() {
	}
	
	/**
	 * Functionless, but satisfies interface
	 */
	public double[] toRGB(double[] rgb) {
		return rgb;
	}
	
	/*
	 * Functionless, but satisfies interface
	 */
	public double[] fromRGB(double[] rgb) {
		return rgb;
	}

}