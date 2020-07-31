package peasyGradients.gradient;

import java.util.ArrayList;
import net.jafama.FastMath;
import peasyGradients.colourSpaces.*;
import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;
import peasyGradients.utilities.Interpolation;

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
 * TODO export as JSON
 *
 */
public final class Gradient {

	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();

	private double[] interpolatedColourOUT = new double[4]; // define once here

	private float offset = 0; // animation colour offset 0...1

	private int lastCurrStopIndex;
	private ColorStop currStop, prevStop;
	private float denom;

	double[] colorOut; // TODO color (in Gradient's current colourspace)

	public ColourSpaces colourSpace = ColourSpaces.XYZ; // TODO public for testing
	ColourSpace colourSpaceInstance = colourSpace.getColourSpace(); // call toRGB on this instance
	public Interpolation interpolationMode = Interpolation.SMOOTH_STEP;
	
	/**
	 * 
	 */
	public Gradient() {
//		this(0xff000000, 0xffffffff); // default: black-->white
		this(Palette.complementary());
	}

	// Creates equidistant color stops.
	public Gradient(int... colors) {
		int sz = colors.length;
		float szf = (sz <= 1.0f ? 1.0f : sz - 1.0f);
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(colors[i], i / szf));
		}
		this.colorStops.forEach(c -> c.setColourSpace(colourSpace));
	}

	public Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			this.colorStops.add(colorStops[i]);
		}
		java.util.Collections.sort(this.colorStops);
		remove();
		this.colorStops.forEach(c -> c.setColourSpace(colourSpace));
	}

	public Gradient(ArrayList<ColorStop> colorStops) {
		this.colorStops = colorStops;
		java.util.Collections.sort(this.colorStops);
		remove();
		this.colorStops.forEach(c -> c.setColourSpace(colourSpace));
	}

	/**
	 * Set a given color stop colour
	 * 
	 * @param stopIndex
	 * @param col       ARGB colour integer representation
	 */
	public void setColorStopCol(int stopIndex, int col) {
		if (stopIndex > -1 && stopIndex < colorStops.size()) {
			System.out.println("set colo");
			colorStops.get(stopIndex).setColor(col);
		}
	}

	/**
	 * Increases the gradient offset (for animation) by the amount given.
	 * 
	 * @param amt 0...1 smaller is less change
	 * @see #setOffset(float)
	 */
	public void animate(float amt) {
		offset += amt;
	}

	/**
	 * 
	 * @param offset
	 * @see #animate(float)
	 */
	public void setOffset(float offset) {
		this.offset = offset;
	}

	public void mutateColour(float amt) {
		// TODO mutate all colour stops
	}

	/**
	 * Prime the gradient for animation (pushes the a copy of the first colour of
	 * the gradient to the end and scales the rest).
	 */
	public void primeAnimation() {
		push(colourAt(0));
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
	 * the gradient.
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
		colorStops.forEach(c-> c.setColourSpace(colourSpace));
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
		if (step != 1) { // 1 % 1 == 0, which we want to avoid
			step %= 1;
			if (step < 0) { // (if animation offset negative)
				step += 1; // equivalent to floormod function
			}
		}

		/**
		 * Calculate whether the current step has gone beyond the existing colourstop
		 * boundary (either above or below). If the first colour stop is at a position >
		 * 0 or last colour stop at a position < 1, then when step > currStop.percent or
		 * step < currStop.percent is true, we don't want to inc/decrement currStop.
		 * Deprecated now, since we pre-compute results into a LUT, so this function is
		 * now called with monotonically increasing step.
		 */
//		if (step > currStop.percent) { // if at end, stay, otherwise next
//			if (lastCurrStopIndex == (colorStops.size() - 1)) {
//				prevStop = colorStops.get(lastCurrStopIndex);
//				denom = 1;
//			} else {
//				do {
//					lastCurrStopIndex++; // increment
//					currStop = colorStops.get(lastCurrStopIndex);
//				} while (step > currStop.percent && lastCurrStopIndex < (colorStops.size() - 1)); // sometimes step might jump more than 1
//																									// colour
//				prevStop = colorStops.get(lastCurrStopIndex - 1);
//
//				denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
//			}
//
//		}
//		else if (step <= prevStop.percent) {
//		if (lastCurrStopIndex == 0) { // if at zero stay, otherwise prev
//			denom = 1;
//			currStop = colorStops.get(0);
//		} else {
//			do {
//				lastCurrStopIndex--; // decrement
//				prevStop = colorStops.get(Math.max(lastCurrStopIndex - 1, 0));
//			} while (step < prevStop.percent); // sometimes step might jump back more than 1 colour
//
//			currStop = colorStops.get(lastCurrStopIndex);
//
//			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
//		}
//	}
		if (step > currStop.percent && lastCurrStopIndex != (colorStops.size() - 1) ) {
			prevStop = colorStops.get(lastCurrStopIndex);
			lastCurrStopIndex++; // increment
			currStop = colorStops.get(lastCurrStopIndex);
			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
		}

		double smoothStep = functStep((step - currStop.percent) * denom); // apply interpolation function

		// interpolate within given colourspace
		colourSpaceInstance.interpolateLinear(currStop.colorOut, prevStop.colorOut, smoothStep, interpolatedColourOUT);
		int alpha = (int) Math.floor( (currStop.alpha + (step * (prevStop.alpha - currStop.alpha))) + 0.5d);

		// convert current colourspace value to ARGB int and return
		return Functions.composeclr(colourSpaceInstance.toRGB(interpolatedColourOUT), alpha);
	}
	
	/**
	 * Colour space is defined for user at peasyGradients level, not gradient (1D)
	 * @param colourSpace
	 */
	public void setColSpace(ColourSpaces colourSpace) {
		this.colourSpace = colourSpace;
		colourSpaceInstance = colourSpace.getColourSpace();
		colorStops.forEach(c -> c.setColourSpace(colourSpace));
	}

	public void nextColSpace() {
		colourSpace = colourSpace.next();
		colourSpaceInstance = colourSpace.getColourSpace();
		colorStops.forEach(c -> c.setColourSpace(colourSpace));
	}

	public void prevColSpace() {
		colourSpace = colourSpace.prev();
		colourSpaceInstance = colourSpace.getColourSpace();
		colorStops.forEach(c -> c.setColourSpace(colourSpace));
	}
	
	public void nextInterpolationMode() {
		interpolationMode = interpolationMode.next();
	}

	public void prevInterpolationMode() {
		interpolationMode = interpolationMode.prev();
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
		sb.append(String.format("    " + "%-10s%-25s%-12s%-20s\n", "Percent", "RGBA", "clrInteger", "clrCurrentColSpace"));
		for (ColorStop colorStop : colorStops) {
			sb.append(String.format("    " + "%-10s%-25s%-12s%-20s\n", colorStop.percent,
					Functions.formatArray(Functions.decomposeclrRGBDouble(colorStop.clr, colorStop.alpha), 0, 2), colorStop.clr,
					Functions.formatArray(colorStop.getColor(colourSpace), 2, 3)));
		}
		return sb.toString();
	}
	
	/**
	 * Calculate the step by passing it to the selected smoothing function. Allows
	 * gradient renderer to easily change how the gradient is smoothed.
	 * 
	 * @param step 0...1
	 * @return the new step
	 */
	private double functStep(float step) {
		switch (interpolationMode) {
			case LINEAR :
				return step;
			case IDENTITY :
				return step * step * (2.0f - step);
			case SMOOTH_STEP :
				return 3 * step * step - 2 * step * step * step; // polynomial approximation of (0.5-FastMath.cos(PI*step)/2)
			case SMOOTHER_STEP :
				return step * step * step * (step * (step * 6 - 15) + 10);
			case EXPONENTIAL :
				return step == 1.0f ? step : 1.0f - FastPow.fastPow(2, -10 * step);
			case CUBIC :
				return step * step * step;
			case BOUNCE :
				float sPrime = step;

				if (sPrime < 0.36364) { // 1/2.75
					return 7.5625f * sPrime * sPrime;
				}
				if (sPrime < 0.72727) // 2/2.75
				{
					return 7.5625f * (sPrime -= 0.545454f) * sPrime + 0.75f;
				}
				if (sPrime < 0.90909) // 2.5/2.75
				{
					return 7.5625f * (sPrime -= 0.81818f) * sPrime + 0.9375f;
				}
				return 7.5625f * (sPrime -= 0.95455f) * sPrime + 0.984375f;
			case CIRCULAR :
				return Math.sqrt((2.0 - step) * step);
			case SINE :
				return FastMath.sinQuick(step);
			case PARABOLA :
				return Math.sqrt(4.0 * step * (1.0 - step));
			case GAIN1 :
				if (step < 0.5f) {
					return 0.5f * FastPow.fastPow(2.0f * step, 0.3f);
				} else {
					return 1 - 0.5f * FastPow.fastPow(2.0f * (1 - step), 0.3f);
				}
			case GAIN2 :
				if (step < 0.5f) {
					return 0.5f * FastPow.fastPow(2.0f * step, 3.3333f);
				} else {
					return 1 - 0.5f * FastPow.fastPow(2.0f * (1 - step), 3.3333f);
				}
			case EXPIMPULSE :
				return (2 * step * FastMath.expQuick(1.0 - (2 * step)));
			default :
				return step;
		}
	}

}
