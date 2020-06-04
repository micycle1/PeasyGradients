package peasyGradients.colourSpaces;

/**
 * aka ICtCp
 * https://www.dolby.com/us/en/technologies/dolby-vision/ictcp-white-paper.pdf
 * https://www.dolby.com/us/en/technologies/dolby-vision/measuring-perceptual-color-volume-v7.1.pdf
 * 
 * @author micycle1
 *
 */
public final class ITP {

	private static double m1 = 2610d / 16384;
	private static double m2 = 2523d / 4096 * 128;
	private static double c1 = 3424d / 4096;
	private static double c2 = 2413d / 4096;
	private static double c3 = 2392d / 4096;

	public static double[] rgb2itp(double[] rgb) {
		double L = 1688 * rgb[0] + 2146 * rgb[1] + 262 * rgb[2];
		double M = 683 * rgb[0] + 2951 * rgb[1] + 462 * rgb[2];
		double S = 99 * rgb[0] + 309 * rgb[1] + 3688 * rgb[2];

		L /= 4096;
		M /= 4096;
		S /= 4096;
		
		double Y = 
	}

	public static double[] itp2rgb(double[] itp) {

	}

}
