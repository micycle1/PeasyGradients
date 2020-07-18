package peasyGradients.gradient;

import static peasyGradients.utilities.Functions.interpolateLinear;

import java.util.ArrayList;
import java.util.Arrays;

import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.Functions;

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

	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();

	private float[] rsltclrF = new float[4]; // define once here
	private double[] rsltclrD = new double[4]; // define once here

	private float offset = 0; // animation colour offset

	private int lastCurrStopIndex;
	private ColorStop currStop, prevStop;
	private float denom;

	double[] colorOut; // TODO color (in Gradient's current colourspace)

	public ColourSpace colourSpace = ColourSpace.XYZ_FAST; // TODO public for testing

	/**
	 * 
	 */
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
	}

	public Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			this.colorStops.add(colorStops[i]);
		}
		java.util.Collections.sort(this.colorStops);
		remove();
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
	 * @param col       ARGB colour integer representation
	 */
	public void setColorStopCol(int stopIndex, int col) {
		if (stopIndex < colorStops.size()) {
			colorStops.get(stopIndex).setColor(col);
		}
	}

	/**
	 * Increases the gradient offset (for animation) by the amount given.
	 * 
	 * @param amt 0...1 smaller is less change
	 */
	public void animate(float amt) {
		offset += amt;
	}
	
	public void setOffset(float offset) {
		offset = offset;
	}

	public void mutateColour(float amt) {

	}

	/**
	 * Prime for animation
	 */
	public void primeAnimation() {
		float offset = colorStops.get(1).percent; // get where first colour stop ends
		offset /= 2;

		for (int i = 1; i < colorStops.size(); i++) { // shift all stops down
			colorStops.get(i).percent -= offset;
		}
		add(1, colourAt(0));
	}

	/**
	 * Push a new colorstop to the end (and shuffle the rest proportional to where
	 * they were before).
	 * 
	 * @param colour
	 * @return
	 * @see #removeLast()
	 */
	public void push(int colour) {
		for (ColorStop colorStop : colorStops) {
			colorStop.percent *= ((colorStops.size() - 1) / (float) colorStops.size()); // scale down existing stop positions
		}
		add(1, colour);
	}

	/**
	 * Remove last colour stop from the gradient and scale the rest to fill
	 * gradient.
	 * 
	 * @see #push(int)
	 */
	public void removeLast() {
		colorStops.remove(colorStops.size() - 1);
		// scale up remaining stop positions
		for (ColorStop colorStop : colorStops) {
			colorStop.percent *= ((colorStops.size()) / (float) (colorStops.size() - 1f)); // scale down existing stop positions
		}
	}

	/**
	 * Returns colour of the colourstop at a given index.
	 * 
	 * @param colorStopIndex
	 * @return
	 */
	public int colourAt(int colorStopIndex) {
		return colorStops.get(colorStopIndex).clr;
	}

	public int lastColor() {
		return colorStops.get(colorStops.size() - 1).clr;
	}

	/**
	 * Add a specific colourstop at a given percentage
	 * 
	 * @param percent 0...1
	 * @param clr
	 */
	void add(final float percent, final int clr) {
		add(new ColorStop(clr, percent));
	}

	void add(final ColorStop colorStop) {
//		for (int sz = colorStops.size(), i = sz - 1; i > 0; --i) {
//			ColorStop current = colorStops.get(i);
//			if (ColorStop.approxPercent(colorStop, ColorStop.TOLERANCE)) {
//				System.out.println(current.toString() + " will be replaced by " + colorStop.toString()); // TODO
//				colorStops.remove(current);
//			}
//		}
		colorStops.add(colorStop);
		java.util.Collections.sort(colorStops); // sort colorstops by value
	}

	public void prime() {
		lastCurrStopIndex = 0;
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

		step += offset;
		if (step != 1) {
			step %= 1;
			if (step < 0) {
				step += 1;
			}
		}

		/**
		 * First calculate whether the current step has gone beyond the existing
		 * colourstop boundary (either above or below). If the first colour stop is at a
		 * position > 0 or last colour stop at a position < 1, then when step >
		 * currStop.percent or step < currStop.percent is true, we don't want to
		 * inc/decrement currStop.
		 */
		if (step > currStop.percent) { // if at end, stay, otherwise next
			if (lastCurrStopIndex == (colorStops.size() - 1)) {
				prevStop = colorStops.get(lastCurrStopIndex);
				denom = 1;
			} else {
				do {
					lastCurrStopIndex++; // increment
					currStop = colorStops.get(lastCurrStopIndex);
				} while (step > currStop.percent && lastCurrStopIndex < (colorStops.size() - 1)); // sometimes step might jump more than 1
																									// colour
				prevStop = colorStops.get(lastCurrStopIndex - 1);

				denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
			}

		} else if (step < prevStop.percent) {
			if (lastCurrStopIndex == 0) { // if at zero stay, otherwise prev
				denom = 1;
				currStop = colorStops.get(0);
			} else {
				do {
					lastCurrStopIndex--; // decrement
					prevStop = colorStops.get(Math.max(lastCurrStopIndex - 1, 0));
				} while (step < prevStop.percent); // sometimes step might jump back more than 1 colour

				currStop = colorStops.get(lastCurrStopIndex);

				denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
			}
		}

		float smoothStep = Functions.functStep((step - currStop.percent) * denom); // apply interpolation function

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
			case TEMP :
				float kelvin = TEMP.interpolate(currStop.clrTEMP, prevStop.clrTEMP, smoothStep);
				return Functions.composeclr(TEMP.temp2rgb(kelvin));
			case RYB :
				interpolateLinear(currStop.clrRYB, prevStop.clrRYB, smoothStep, rsltclrF);
				return Functions.composeclr(RYB.ryb2rgb(rsltclrF));
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
				return colorStops.get(colorStops.size() - 1).clr;
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

	public void nextColSpace() {
		colourSpace = colourSpace.next();
	}

	public void prevColSpace() {
		colourSpace = colourSpace.prev();
	}

	boolean remove(ColorStop colorStop) {
		return colorStops.remove(colorStop);
	}

	ColorStop remove(int i) {
		return colorStops.remove(i);
	}

	/**
	 * Remove points if they are within a certain tolerance of each other.
	 * 
	 * @return number of removed points
	 */
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
		return removed;
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
	 * Return randomised gradient (random colors; equidistant stops). TODO use
	 * palette instead?
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

	/**
	 * Returns colorStop info for the Gradient.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Gradient\n");
		sb.append("Offset: " + offset + "\n");
		sb.append("Colour Stops (" + colorStops.size() + "):\n");
		sb.append(String.format("    " + "%-10s%-25s%-15s%-20s\n", "Percent", "RGB", "clrInteger", "clrXYZ"));
		for (ColorStop colorStop : colorStops) {
			sb.append(String.format("    " + "%-10s%-25s%-15s%-20s\n", colorStop.percent,
					Arrays.toString(Functions.decomposeclrRGB(colorStop.clr)), colorStop.clr, Arrays.toString(colorStop.clrXYZ)));
		}
		return sb.toString();
	}

}
