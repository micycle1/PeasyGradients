package peasyGradients.colourSpaces;

import net.jafama.FastMath;

/**
 * Based on implementation by Neil Bartlett
 * https://github.com/neilbartlett/color-temperature
 * https://github.com/d3/d3-interpolate
 * https://github.com/gka/chroma.js/tree/master/src/io
 * @author micycle1
 *
 */
public final class TEMP {

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
		return FastMath.round(temp);
	}

	/**
	 * Outputs to sRGB 0...255
	 * 
	 * @param kelvin
	 * @return
	 */
	public static float[] temp2rgb(float kelvin) {
		final float temp = kelvin / 100;
		float r, g, b;
		if (temp < 66) {
			r = 255;
			g = (float) (-155.25485562709179 - 0.44596950469579133 * (g = temp - 2)
					+ 104.49216199393888 * FastMath.log(g));
			b = (float) (temp < 20 ? 0
					: -254.76935184120902 + 0.8274096064007395 * (b = temp - 10)
							+ 115.67994401066147 * FastMath.log(b));
		} else {
			r = (float) (351.97690566805693 + 0.114206453784165 * (r = temp - 55)
					- 40.25366309332127 * FastMath.log(r));
			g = (float) (325.4494125711974 + 0.07943456536662342 * (g = temp - 50)
					- 28.0852963507957 * FastMath.log(g));
			b = 255;
		}
		return new float[] { r/255, g/255, b/255, 1 };
	}

	public static float interpolate(float temp1, float temp2, float percent) {
		return temp1 + percent * (temp2 - temp1);
	}

}
