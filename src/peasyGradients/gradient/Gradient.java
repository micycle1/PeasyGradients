package peasyGradients.gradient;

import static peasyGradients.utilities.Functions.interpolateLinear;

import java.util.ArrayList;

import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.Functions;
import processing.core.PImage;

/**
 * A gradient comprises of {@link #colorStops}, which each specify a colour and
 * a percentage position that the colour occurs on a 1D gradient. Supports
 * opacity. Gradients also define the colour space in which colours are
 * interpolated (such as CIE_LAB) and the gradient curve function (such as
 * sine()) -- the function governing how the gradient's colour transtions (the
 * step used during interpolation).
 * 
 * <p>
 * Use a {@link #peasyGradients.PeasyGradients} instance to render Gradients.
 * 
 * @author micycle1
 *
 */
public final class Gradient {

	public ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();

	private float[] rsltclrF = new float[4];
	private double[] rsltclrD = new double[4];

	public float animate = 0; // animation colour offset
	private int numStops = 0;

	private int lastCurrStopIndex;
	private ColorStop currStop, prevStop;
	private float denom;

	PImage cacheLastImage; // TODO, last PImage output (cache and return if args haven't changed)

	public ColourSpace colourSpace = ColourSpace.XYZ_FAST; // TODO

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
			temp[i] = new ColorStop(Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1), percent);
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
			colorStops.add(new ColorStop(colors[i], i / szf));
		}
		numStops = sz;
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

	/**
	 * Set a given color stop colour
	 * 
	 * @param stopIndex
	 * @param col ARGB colour integer representation
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
			newAmt %= 1;
			colorStop.percent = newAmt;
//			float x_min = 0;
//			float x_max = 1;
//
//			colorStop.percent = (((newAmt - x_min) % (x_max - x_min)) + (x_max - x_min)) % (x_max - x_min) + x_min;
		}
//		java.util.Collections.sort(colorStops);
	}

	void add(final float percent, final int clr) {
		add(new ColorStop(clr, percent));
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

	public void prime() {
		lastCurrStopIndex = 1;
		currStop = colorStops.get(lastCurrStopIndex);
		prevStop = colorStops.get(0);
	}

	/**
	 * Main method of gradient class. Computes the RGB value at a given percentage
	 * of the gradient. Internally, the step input undergoes a function set by the
	 * user. www.andrewnoske.com/wiki/Code_-_heatmaps_and_color_gradients
	 * 
	 * @param step a linear step (percentage) through the gradient 0...1
	 * @return ARGB integer for Processing pixel array.
	 */
	public int evalRGB(float step) {

		/**
		 * First calculate whether the current step has gone beyond the existing
		 * colourstop boundary (either above or below).
		 */
		if (step > currStop.percent) {
			do {
				currStop = colorStops.get(++lastCurrStopIndex); // increment
			} while (step > currStop.percent); // sometimes step might jump more than 1 colour

			prevStop = colorStops.get(lastCurrStopIndex - 1);

			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
		} else if (step < prevStop.percent) {
			do {
				prevStop = colorStops.get(--lastCurrStopIndex - 1); // decrement
			} while (step < prevStop.percent); // sometimes step might jump back more than 1 colour

			currStop = colorStops.get(lastCurrStopIndex);

			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
		}

		final float smoothStep = Functions.functStep((step - currStop.percent) * denom); // apply function to scaled
																							// step
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
			case XYZ_FAST :
				interpolateLinear(currStop.clrXYZ_FAST, prevStop.clrXYZ_FAST, smoothStep, rsltclrD);
				return Functions.composeclr(XYZ_FAST.xyz2rgbVeryQuick(rsltclrD));
			case ITP :
				interpolateLinear(currStop.clrITP, prevStop.clrITP, smoothStep, rsltclrD);
				return Functions.composeclr(ITP.itp2rgb(rsltclrD));
			case ITP_FAST :
				interpolateLinear(currStop.clrITP, prevStop.clrITP, smoothStep, rsltclrD);
				return Functions.composeclr(ITP.itp2rgbQuick(rsltclrD));
			default :
				return colorStops.get(numStops - 1).clr;
		}
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
