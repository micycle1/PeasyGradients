package peasyGradients;

import static processing.core.PConstants.RGB;
import static processing.core.PConstants.HSB;

import java.util.ArrayList;

/**
 * A gradient contains color, and the position (percentage) at which that color
 * occurs within the gradient. multi-position color stops Stops may be
 * equidistant A gradient comprises of {@link #colorStops}, which each specify a
 * color and location.
 * 
 * TODO change colorspace interpolation: CIE-LCh, HSB
 * http://www.brucelindbloom.com/index.html?Math.html
 * 
 * @author micycle1
 *
 */
public final class Gradient {

	static final int DEFAULT_COLOR_MODE = RGB;
	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();
	private int COLOR_SPACE = RGB;

	/**
	 * Defines how gradient should interpolate between colors (by performining
	 * interpolation on different color spaces).
	 * 
	 * @author micycle1
	 *
	 */
	enum ColorSpaces {
		RGB, HSB, LCH, HCL, CMYK, LAB
	} // TODO
	
	ColorSpaces colorSpace = ColorSpaces.HSB; // TODO

	/**
	 * Return randomised gradient (random colors and stop positions).
	 * 
	 * @param numColors
	 * @return
	 */
	public static Gradient randomGradientWithStops(int numColors) {
		ColorStop[] temp = new ColorStop[numColors];
		float percent;
		for (int i = 0; i < numColors; ++i) {
			percent = i == 0 ? 0 : i == numColors - 1 ? 1 : (float) Math.random();
			temp[i] = new ColorStop(percent,
					Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1));
		}
		return new Gradient(temp);
	}

	/**
	 * Return randomised gradient (random colors; equidistant stops).
	 * 
	 * @param numColors
	 * @return
	 */
	public static Gradient randomGradient(int numColors) {
		int[] temp = new int[numColors];
		for (int i = 0; i < numColors; ++i) {
			temp[i] = Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1);
		}
		return new Gradient(temp);
	}

	Gradient() {
		this(0xff000000, 0xffffffff); // default: black-->white
//		this(0xfffc0398,0xffff7f00, 0xff007fff);
	}

	// Creates equidistant color stops.
	Gradient(int... colors) {
		int sz = colors.length;
		float szf = (sz <= 1.0f ? 1.0f : sz - 1.0f);
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(i / szf, colors[i]));
		}
	}

	// Creates equidistant color stops.
	Gradient(int colorMode, float[]... colors) {
		int sz = colors.length;
		float szf = sz <= 1.0f ? 1.0f : sz - 1.0f;
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(colorMode, i / szf, colors[i]));
		}
	}

	Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			this.colorStops.add(colorStops[i]);
		}
		java.util.Collections.sort(this.colorStops);
		remove();
	}

	Gradient(ArrayList<ColorStop> colorStops) {
		this.colorStops = colorStops;
		java.util.Collections.sort(this.colorStops);
		remove();
	}

	void add(int colorMode, float percent, float[] arr) {
		add(new ColorStop(colorMode, percent, arr));
	}

	void add(int colorMode, float percent, float x, float y, float z, float w) {
		add(new ColorStop(colorMode, percent, x, y, z, w));
	}

	void add(final float percent, final int clr) {
		add(new ColorStop(percent, clr));
	}

	void add(final ColorStop colorStop) {
		for (int sz = colorStops.size(), i = sz - 1; i > 0; --i) {
			ColorStop current = colorStops.get(i);
			if (current.approxPercent(colorStop, ColorStop.TOLERANCE)) {
				System.out.println(current.toString() + " will be replaced by " + colorStop.toString()); // TODO
				colorStops.remove(current);
			}
		}
		colorStops.add(colorStop);
		java.util.Collections.sort(colorStops);
	}

	/**
	 * Main method of gradient class.
	 * www.andrewnoske.com/wiki/Code_-_heatmaps_and_color_gradients
	 * 
	 * https://github.com/gka/chroma.js/tree/master/src/interpolator // TODO
	 * 
	 * https://github.com/d3/d3-interpolate // TODO
	 * 
	 * @param step
	 * @param colorMode
	 * @return
	 */
	int eval(final float step) {
		final int size = colorStops.size();

		// Exit from the function early whenever possible.
//		if (size == 0) {
//			return 0x00000000;
//		} else if (size == 1 || step < 0.0) {
//			return colorStops.get(0).clr;
//		} else if (step >= 1.0) {
//			return colorStops.get(size - 1).clr;
//		}

		ColorStop currStop;
		ColorStop prevStop;
		float currPercent, scaledst;
		for (int i = 0; i < size; i++) {
			currStop = colorStops.get(i);
			currPercent = currStop.percent;

			if (step < currPercent) {

				// These can be declared within the for-loop because
				// if step < currPercent, the function will return
				// and no more iterations will be executed.
				float[] originclr = new float[4];
				float[] destclr = new float[4];
				float[] rsltclr = new float[4];

				// If not at the first stop in the gradient (i == 0),
				// then get the previous.
				prevStop = colorStops.get(i - 1 < 0 ? 0 : i - 1);

				scaledst = step - currPercent;
				float denom = prevStop.percent - currPercent;
				if (denom != 0) {
					scaledst /= denom;
				}

				// Assumes that color stops' colors are ints. They could
				// also be float[] arrays, in which case they wouldn't
				// need to be decomposed.
				switch (colorSpace) {
					// TODO CALC STEP HERE
					case HSB :
						Functions.rgbToHsb(currStop.clr, originclr);
						Functions.rgbToHsb(prevStop.clr, destclr);
						Functions.smootherStepHsb(originclr, destclr, scaledst, rsltclr);
						return Functions.composeclr(Functions.hsbToRgb(rsltclr));
						
					case LAB : 
						float[] labA = LAB.rgb2lab(Functions.decomposeclr(currStop.clr)); // RGB->LAB
						float[] labB = LAB.rgb2lab(Functions.decomposeclr(prevStop.clr));
						LAB.interpolate(labA, labB, scaledst, rsltclr); // stil in LAB
						float[] inter = LAB.lab2rgb2(rsltclr);
						return Functions.composeclr(inter[0], inter[1], inter[2]);
						
//						float[] col = Functions.decomposeclr(currStop.clr);
//						float[] labA = LAB.rgb2xyz(col[0],col[1], col[2]); // RGB->xyz
//						col = Functions.decomposeclr(prevStop.clr);
//						float[] labB = LAB.rgb2xyz(col[0],col[1], col[2]); // RGB->xyz
//						LAB.interpolate(labA, labB, scaledst, rsltclr); // stil in LAB
//						float[] inter = LAB.XYZ2sRGB(rsltclr);
//						return Functions.composeclr(inter[0], inter[1], inter[2]);
						
					case RGB :
						Functions.decomposeclr(currStop.clr, originclr); // decompose int color to [R,G,B,A] (0-1)
						Functions.decomposeclr(prevStop.clr, destclr); // decompose int color to [R,G,B,A] (0-1)
						Functions.smootherStepRgb(originclr, destclr, scaledst, rsltclr); // lerp between colors?
//						return Functions.composeclr(rsltclr[0], rsltclr[1], rsltclr[2]);
						return Functions.composeclr(rsltclr);
				}
			}
		}
		return colorStops.get(size - 1).clr;
	}

	boolean remove(ColorStop colorStop) {
		return colorStops.remove(colorStop);
	}

	ColorStop remove(int i) {
		return colorStops.remove(i);
	}

	int remove() {
		int removed = 0;
		for (int sz = colorStops.size(), i = sz - 1; i > 0; --i) {
			ColorStop current = colorStops.get(i);
			ColorStop prev = colorStops.get(i - 1);
			if (current.approxPercent(prev, ColorStop.TOLERANCE)) {
				System.out.println(current + " removed, as it was too close to " + prev);
				colorStops.remove(current);
				removed++;
			}
		}
		return removed;
	}
}
