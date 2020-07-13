package peasyGradients.colourSpaces;

import peasyGradients.utilities.fastLog.FFastLog;

/**
 * Based on implementation by Neil Bartlett
 * https://github.com/neilbartlett/color-temperature
 * https://github.com/d3/d3-interpolate
 * https://github.com/gka/chroma.js/tree/master/src/io
 * 
 * @author micycle1
 *
 */
public final class TEMP {

	private static final FFastLog fastLog = new FFastLog(9);

	public static float rgb2temp(float[] rgba) {
		final float r = rgba[0], b = rgba[2];

		float minTemp = 1000;
		float maxTemp = 40000;
		float eps = 0.4f;

		float temp = 0;
		while (maxTemp - minTemp > eps) {
			temp = (maxTemp + minTemp) * 0.5f;
			final float[] rgb = temp2rgb(temp);
			if ((rgb[2] / rgb[0]) >= (b / r)) {
				maxTemp = temp;
			} else {
				minTemp = temp;
			}
		}
		return temp;
	}

	/**
	 * Outputs to sRGB 0...255
	 * 
	 * @param kelvin
	 * @return
	 */
	public static float[] temp2rgb(float kelvin) {
		final float temp = kelvin / 100;
		float g;
		if (temp < 66) {
			g = (-155.25485562709179f - 0.44596950469579133f * (g = temp - 2) + 104.49216199393888f * fastLog.fastLog(g));
			float b = (temp < 20 ? 0
					: -254.76935184120902f + 0.8274096064007395f * (b = temp - 10) + 115.67994401066147f * fastLog.fastLog(b));
			return new float[] { 1, g / 255f, b / 255f, 1 };
		} else {
			float r = (351.97690566805693f + 0.114206453784165f * (r = temp - 55) - 40.25366309332127f * fastLog.fastLog(r));
			g = (325.4494125711974f + 0.07943456536662342f * (g = temp - 50) - 28.0852963507957f * fastLog.fastLog(g));
			return new float[] { r / 255f, g / 255f, 1, 1 };
		}

	}

	public static float interpolate(float temp1, float temp2, float percent) {
		return temp1 + percent * (temp2 - temp1);
	}

}
