package peasyGradients.gradient;

import static peasyGradients.utilities.Functions.interpolateLinear;

import java.util.ArrayList;
import java.util.Arrays;

import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.Functions;
import processing.core.PImage;

/**
 * A gradient comprises of {@link #colorStops}, which each specify a color and a
 * percentage position that it occurs on the 1D gradient. Supports opacity.
 * Gradients also define the colour space in which colours are interpolated
 * (such as CIE_LAB) and the gradient curve function (such as sine()) -- the
 * function governing how the gradient's colour transtions.
 * 
 * @author micycle1
 *
 */
public final class Gradient {

	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();
	float[] rsltclrF = new float[4];
	float animate = 0; // animation colour offset
	double[] rsltclrD = new double[4];
	private int numStops = 0;
	ColorStop[] colorStops2; // faster index access than arraylist

	PImage cacheLastImage; // TODO, last PImage output (cache and return if args haven't changed)

	public ColourSpace colourSpace = ColourSpace.ITP; // TODO

//	Interpolation interpolation = Interpolation.PARABOLA;

	public void nextColSpace() {
		colourSpace = colourSpace.next();
	}

	public void prevColSpace() {
		colourSpace = colourSpace.prev();
	}

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

	public Gradient() {
		this(0xff000000, 0xffffffff); // default: black-->white
	}

	// Creates equidistant color stops.
	public Gradient(int... colors) {
		int sz = colors.length;
		float szf = (sz <= 1.0f ? 1.0f : sz - 1.0f);
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(i / szf, colors[i]));
		}
		numStops += sz;
	}

	// Creates equidistant color stops.
	public Gradient(int colorMode, float[]... colors) {
		int sz = colors.length;
		float szf = sz <= 1.0f ? 1.0f : sz - 1.0f;
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(colorMode, i / szf, colors[i]));
		}
		numStops += sz;
	}

	public Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			this.colorStops.add(colorStops[i]);
		}
		java.util.Collections.sort(this.colorStops);
		remove();
		numStops += sz;
	}

	public Gradient(ArrayList<ColorStop> colorStops) {
		this.colorStops = colorStops;
		java.util.Collections.sort(this.colorStops);
		remove();
	}

	/*
	 * Set a given color stop colour
	 */
	public void setColorStopCol(int stopIndex, int col) {
		if (stopIndex < colorStops.size()) {
			colorStops.get(stopIndex).setColor(col);
		}
	}

	/**
	 * 
	 * @param amt 0...1 (smaller faster) recommended: 0.01
	 */
	public void animate(float amt) {
//		amt = (amt < -1) ? -1 : (amt > 1 ? 1 : amt); // clamp between -1...1
		animate = amt;
		for (ColorStop colorStop : colorStops) {
			float newAmt = colorStop.originalPercent + amt;
			float x_min = 0;
			float x_max = 1;

			colorStop.percent = (((newAmt - x_min) % (x_max - x_min)) + (x_max - x_min)) % (x_max - x_min) + x_min;
		}
//		java.util.Collections.sort(colorStops);
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
			if (ColorStop.approxPercent(colorStop, ColorStop.TOLERANCE)) {
				System.out.println(current.toString() + " will be replaced by " + colorStop.toString()); // TODO
				colorStops.remove(current);
			}
		}
		colorStops.add(colorStop);
		java.util.Collections.sort(colorStops); // sort colorstops by value
		numStops++;
	}

	/**
	 * Main method of gradient class.
	 * www.andrewnoske.com/wiki/Code_-_heatmaps_and_color_gradients
	 * 
	 * https://github.com/gka/chroma.js/tree/master/src/interpolator // TODO
	 * 
	 * https://github.com/d3/d3-interpolate // TODO
	 * 
	 * returns the RGB value at given index
	 * 
	 * @param step
	 * @param colorMode
	 * @return
	 */
	public int evalRGB(float step) {

//		 Exit from the function early whenever possible.
		if (numStops == 0) {
			return 0;
		} else if (numStops == 1 || step < 0.0) {
			return colorStops.get(0).clr;
		} else if (step >= 1.0) {
			return colorStops.get(numStops - 1).clr;
		}

		/*
		 * TODO: track last currStop use current step to decide if need to use next one.
		 */
		ColorStop currStop;
		ColorStop prevStop;
		float currPercent, scaledst;
		for (int i = 0; i < numStops; i++) {
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
				float smoothStep = Functions.functStep(scaledst); // apply function to step

				switch (colourSpace) {
					case HSB_SHORT :
						HSB.interpolateShort(currStop.clrHSB, prevStop.clrHSB, smoothStep, rsltclrF);
						return Functions.composeclr(HSB.hsbToRgb(rsltclrF));
					case HSB_LONG :
						HSB.interpolateLong(currStop.clrHSB, prevStop.clrHSB, smoothStep, rsltclrF);
						return Functions.composeclr(HSB.hsbToRgb(rsltclrF));
					case RGB :
						interpolateLinear(currStop.clrRGB, prevStop.clrRGB, smoothStep, rsltclrF);
						return Functions.composeclr(rsltclrF);
					case LAB :
						interpolateLinear(currStop.clrLAB, prevStop.clrLAB, smoothStep, rsltclrD);
						return Functions.composeclr(CIE_LAB.lab2rgb(rsltclrD));
					case FAST_LAB :
						interpolateLinear(currStop.clrLAB, prevStop.clrLAB, smoothStep, rsltclrD);
						return Functions.composeclr(CIE_LAB.lab2rgbQuick(rsltclrD));
					case VERY_FAST_LAB :
						interpolateLinear(currStop.clrLAB, prevStop.clrLAB, smoothStep, rsltclrD);
						return Functions.composeclr(CIE_LAB.lab2rgbVeryQuick(rsltclrD));
					case HUNTER_LAB :
						interpolateLinear(currStop.clrHLAB, prevStop.clrHLAB, smoothStep, rsltclrD);
						return Functions.composeclr(HUNTER_LAB.hlab2rgb(rsltclrD));
					case HUNTER_LAB_FAST :
						interpolateLinear(currStop.clrHLAB, prevStop.clrHLAB, smoothStep, rsltclrD);
						return Functions.composeclr(HUNTER_LAB.hlab2rgbQuick(rsltclrD));
					case YCoCg :
						interpolateLinear(currStop.clrYCoCg, prevStop.clrYCoCg, smoothStep, rsltclrD);
						return Functions.composeclr(YCoCg.YCoCg2rgb(rsltclrD));
					case LUV :
						interpolateLinear(currStop.clrLUV, prevStop.clrLUV, smoothStep, rsltclrD);
						return Functions.composeclr(LUV.luv2rgb(rsltclrD));
					case FAST_LUV :
						interpolateLinear(currStop.clrLUV, prevStop.clrLUV, smoothStep, rsltclrD);
						return Functions.composeclr(LUV.luv2rgbQuick(rsltclrD));
					case JAB :
						interpolateLinear(currStop.clrJAB, prevStop.clrJAB, smoothStep, rsltclrD);
						return Functions.composeclr(JAB.jab2rgb(rsltclrD));
					case JAB_FAST :
						interpolateLinear(currStop.clrJAB, prevStop.clrJAB, smoothStep, rsltclrD);
						return Functions.composeclr(JAB.jab2rgbQuick(rsltclrD));
					case HCG :
						HCG.interpolate(currStop.clrHCG, prevStop.clrHCG, smoothStep, rsltclrF);
						return Functions.composeclr(HCG.hcg2rgb(rsltclrF));
					case TEMP :
						float kelvin = TEMP.interpolate(currStop.tempclr, prevStop.tempclr, smoothStep);
						return Functions.composeclr(TEMP.temp2rgb(kelvin));
					case RYB :
						interpolateLinear(currStop.clrRYB, prevStop.clrRYB, smoothStep, rsltclrF);
						return Functions.composeclr(RYB.ryb2rgb(rsltclrF));
					case YUV :
						interpolateLinear(currStop.clrYUV, prevStop.clrYUV, smoothStep, rsltclrF);
						return Functions.composeclr(YUV.yuv2rgb(rsltclrF));
					case XYZ :
						interpolateLinear(currStop.clrXYZ, prevStop.clrXYZ, smoothStep, rsltclrD);
						return Functions.composeclr(XYZ.xyz2rgb(rsltclrD));
					case ITP :
						interpolateLinear(currStop.clrITP, prevStop.clrITP, smoothStep, rsltclrD);
						return Functions.composeclr(ITP.itp2rgb(rsltclrD));
					default :
						break;
				}
			}
		}
		return colorStops.get(numStops - 1).clr;
	}

	/**
	 * TODO Return raw colour value (don't convert to rgb). Used for quad gradients
	 * / second pass
	 * 
	 * @return
	 */
	int evalRaw() {
		return 0;
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
			if (ColorStop.approxPercent(prev, ColorStop.TOLERANCE)) {
				System.out.println(current + " removed, as it was too close to " + prev);
				colorStops.remove(current);
				removed++;
			}
		}
		numStops--;
		return removed;
	}

}
