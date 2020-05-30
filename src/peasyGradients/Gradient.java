package peasyGradients;

import java.util.ArrayList;

import peasyGradients.colourSpaces.FastLAB;
import peasyGradients.colourSpaces.HSB;
import peasyGradients.colourSpaces.LAB;
import peasyGradients.colourSpaces.RGB;

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

	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();
	float[] rsltclrF = new float[4];
	double[] rsltclrD = new double[4];

	/**
	 * Defines how gradient should interpolate between colors (by performining
	 * interpolation on different color spaces).
	 * 
	 * @author micycle1
	 *
	 */
	enum ColorSpaces {
		RGB, HSB, LAB, FAST_LAB;
		// LCH, HCL, CMYK, TODO

		private final static ColorSpaces[] vals = values();

		ColorSpaces next() {
			return vals[(ordinal() + 1) % vals.length];
		}
	} // TODO

	public void nextColSpace() {
		colorSpace = colorSpace.next();
	}

	ColorSpaces colorSpace = ColorSpaces.LAB; // TODO

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
		java.util.Collections.sort(colorStops); // sort colorstops by value
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

//		 Exit from the function early whenever possible.
		if (size == 0) {
			return 0x00000000;
		} else if (size == 1 || step < 0.0) {
			return colorStops.get(0).clr;
		} else if (step >= 1.0) {
			return colorStops.get(size - 1).clr;
		}
		

		ColorStop currStop;
		ColorStop prevStop;
		float currPercent, scaledst;
		for (int i = 0; i < size; i++) {
			currStop = colorStops.get(i);
			currPercent = currStop.percent;

			if (step < currPercent) {

				// If not at the first stop in the gradient (i == 0),
				// then get the previous.
				prevStop = colorStops.get(i - 1 < 0 ? 0 : i - 1);

				scaledst = step - currPercent;
				float denom = prevStop.percent - currPercent;
				if (denom != 0) {
					scaledst /= denom;
				}
				final float smoothStep = Functions.functStep(scaledst); // apply function to step

				switch (colorSpace) {
					// TODO CALC STEP HERE ?
					case HSB :
						HSB.interpolate(currStop.clrHSB, prevStop.clrHSB, smoothStep, rsltclrF);
						return Functions.composeclr(HSB.hsbToRgb(rsltclrF));

					case RGB :
						RGB.interpolate(currStop.clrRGB, prevStop.clrRGB, smoothStep, rsltclrF);
						return Functions.composeclr(rsltclrF);

					case LAB :
						
						LAB.interpolate(currStop.labclr, prevStop.labclr, smoothStep, rsltclrD);
						return LAB.lab2rgb(rsltclrD);

					case FAST_LAB :
						LAB.interpolate(currStop.labclr, prevStop.labclr, smoothStep, rsltclrD);
						return FastLAB.lab2rgb(rsltclrD);
					default :
						break;
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
