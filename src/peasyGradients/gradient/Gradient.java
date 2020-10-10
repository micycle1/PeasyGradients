package peasyGradients.gradient;

import java.util.ArrayList;
import java.util.Iterator;

import net.jafama.FastMath;
import peasyGradients.colorSpaces.*;
import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;
import peasyGradients.utilities.Interpolation;

/**
 * A gradient comprises of {@link #colorStops}, which each specify a color and
 * a percentage position that the color occurs on a 1D gradient. Supports
 * opacity. Gradients define the gradient curve function (such as sine()) -- the
 * function governing how the gradient's color transtions (the step used during
 * interpolation).
 * 
 * <p>
 * Use a {@link #peasyGradients.PeasyGradients} instance to render Gradients.
 * 
 * @author micycle1
 * 
 *         TODO export as JSON
 *
 */
public final class Gradient {

	private ArrayList<ColorStop> colorStops = new ArrayList<ColorStop>();

	private double[] interpolatedcolorOUT = new double[4]; // define once here

	private float offset = 0; // animation color offset 0...1

	private int lastCurrStopIndex;
	private ColorStop currStop, prevStop;
	private float denom;

//	double[] colorOut; // TODO color (in Gradient's current colorspace)

	public ColorSpaces colorSpace = ColorSpaces.LUV; // TODO public for testing
	ColorSpace colorSpaceInstance = colorSpace.getColorSpace(); // call toRGB on this instance
	public Interpolation interpolationMode = Interpolation.SMOOTH_STEP;

	/**
	 * Constructs a new gradient consisting of 2 equidistant complementary colors.
	 */
	public Gradient() {
		this(Palette.complementary()); // random 2 colors
	}
	
	/**
	 * Creates a gradient with equidistant color stops.
	 * @param colors ARGB color integers (the kind returned by Processing's  color() method)
	 */
	public Gradient(int... colors) {
		int sz = colors.length;
		float szf = (sz <= 1.0f ? 1.0f : sz - 1.0f);
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(colors[i], i / szf));
		}
		this.colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			this.colorStops.add(colorStops[i]);
		}
		java.util.Collections.sort(this.colorStops);
		this.colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public Gradient(ArrayList<ColorStop> colorStops) {
		this.colorStops = colorStops;
		java.util.Collections.sort(this.colorStops);
		this.colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	/**
	 * Set a given color stop a color
	 * 
	 * @param stopIndex
	 * @param col       ARGB color integer representation
	 */
	public void setcolorStopCol(int stopIndex, int col) {
		if (stopIndex > -1 && stopIndex < colorStops.size()) {
			colorStops.get(stopIndex).setcolor(col);
		} else {
			System.err.println("Index out of bounds.");
		}
	}

	/**
	 * Increases the offset of all color stops by the amount given (call this each frame to animate a gradient).
	 * 
	 * @param amt 0...1 smaller is less change
	 * @see #setOffset(float)
	 */
	public void animate(float amt) {
		offset += amt;
	}

	/**
	 * Sets the offset of all color stops to a specific value.
	 * 
	 * @param offset
	 * @see #animate(float)
	 */
	public void setOffset(float offset) {
		this.offset = offset;
	}

	/**
	 * Mutates the color of all color stops in the RGB255 space by the amount
	 * given. Mutation randomises between adding or subtracting the mutation amount
	 * from each of the R,G,B channels.
	 * 
	 * @param amt magnitude of mutation [0...255]
	 */
	public void mutatecolor(float amt) {
		colorStops.forEach(c -> c.mutate(amt));
	}

	/**
	 * Primes the gradient for animation (pushes the a copy of the first color of
	 * the gradient to the end and scales the rest).
	 */
	public void primeAnimation() {
		push(colorAt(0));
	}

	/**
	 * Pushes a new colorstop to the end of the gradient (and shuffle the rest proportional to where
	 * they were before).
	 * 
	 * @param color
	 * @return
	 * @see #removeLast()
	 */
	public void push(int color) {
		for (ColorStop colorStop : colorStops) {
			colorStop.percent *= ((colorStops.size() - 1) / (float) colorStops.size()); // scale down existing stop positions
		}
		add(1, color);
	}

	/**
	 * Removes the last color stop from the gradient and scale the rest to fill the
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
	 * Returns the ARGB color of the colorstop at a given index.
	 * 
	 * @param colorStopIndex
	 * @return 32bit ARGB color int
	 */
	public int colorAt(int colorStopIndex) {
		return colorStops.get(colorStopIndex).clr;
	}

	/**
	 * Returns this gradient's last color.
	 * 
	 * @return 32bit ARGB color int
	 */
	public int lastcolor() {
		return colorStops.get(colorStops.size() - 1).clr;
	}

	/**
	 * Adds a specific color to the gradient at a given percentage.
	 * 
	 * @param percent 0...1
	 * @param clr
	 */
	public void add(final float percent, final int clr) {
		add(new ColorStop(clr, percent));
	}

	/**
	 * Adds a color stop to the gradient.
	 * @param colorStop
	 */
	public void add(final ColorStop colorStop) {
		colorStops.add(colorStop);
		java.util.Collections.sort(colorStops); // sort colorstops by value
	}

	/**
	 * Prime for rendering (TODO protected?)
	 */
	public void prime() {
		lastCurrStopIndex = 0;
		currStop = colorStops.get(lastCurrStopIndex);
		prevStop = colorStops.get(0);
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	/**
	 * Evalutes the ARGB color of the gradient at the given position (0.0...1.0).
	 * 
	 * <p>
	 * This is the main method of Gradient class. Computes the RGB value at a given
	 * percentage of the gradient. Internally, the the position input undergoes is
	 * transformed by the current interpolation function.
	 * 
	 * @param position a linear position expressed as a decimal between 0...1
	 * @return ARGB integer for Processing pixel array.
	 */
	public int getcolor(float position) {

		position += offset;
		if (position < 0) { // (if animation offset negative)
			position += 1; // equivalent to floormod function
		}
		if (position > 1) { // 1 % 1 == 0, which we want to avoid
			position %= 1;
		}

		/**
		 * Calculate whether the current step has gone beyond the existing colorstop
		 * boundary (either above or below). If the first color stop is at a position >
		 * 0 or last color stop at a position < 1, then when step > currStop.percent or
		 * step < currStop.percent is true, we don't want to inc/decrement currStop.
		 */
		if (position > currStop.percent) { // if at end, stay, otherwise next
			if (lastCurrStopIndex == (colorStops.size() - 1)) {
				prevStop = colorStops.get(lastCurrStopIndex);
				denom = 1;
			} else {
				do {
					lastCurrStopIndex++; // increment
					currStop = colorStops.get(lastCurrStopIndex);
					// sometimes step might jump more than 1 color, hence while()
				} while (position > currStop.percent && lastCurrStopIndex < (colorStops.size() - 1));
				prevStop = colorStops.get(lastCurrStopIndex - 1);

				denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
			}

		} else if (position <= prevStop.percent) {
			if (lastCurrStopIndex == 0) { // if at zero stay, otherwise prev
				denom = 1;
				currStop = colorStops.get(0);
			} else {
				do {
					lastCurrStopIndex--; // decrement
					prevStop = colorStops.get(Math.max(lastCurrStopIndex - 1, 0));
				} while (position < prevStop.percent); // sometimes step might jump back more than 1 color

				currStop = colorStops.get(lastCurrStopIndex);

				denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
			}
		}

		/**
		 * Since we pre-compute results into a LUT, this function works with
		 * monotonically increasing step. HOWEVER, doesn't work if animating, hence
		 * commented out.
		 */
//		if (step > currStop.percent && lastCurrStopIndex != (colorStops.size() - 1) ) {
//			prevStop = colorStops.get(lastCurrStopIndex);
//			lastCurrStopIndex++; // increment
//			currStop = colorStops.get(lastCurrStopIndex);
//			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
//		}

		double smoothStep = functStep((position - currStop.percent) * denom); // apply interpolation function

		// interpolate within given colorspace
		colorSpaceInstance.interpolateLinear(currStop.colorOut, prevStop.colorOut, smoothStep, interpolatedcolorOUT);
		int alpha = (int) Math.floor((currStop.alpha + (position * (prevStop.alpha - currStop.alpha))) + 0.5d);

		// convert current colorspace value to ARGB int and return
		return Functions.composeclr(colorSpaceInstance.toRGB(interpolatedcolorOUT), alpha);
	}

	/**
	 * color space is defined for user at peasyGradients level, not gradient (1D)
	 * 
	 * @param colorSpace
	 */
	public void setColSpace(ColorSpaces colorSpace) {
		this.colorSpace = colorSpace;
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public void nextColSpace() {
		colorSpace = colorSpace.next();
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}

	public void prevColSpace() {
		colorSpace = colorSpace.prev();
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setcolorSpace(colorSpace));
	}
	
	public void setInterpolationMode(Interpolation interpolationMode) {
		this.interpolationMode = interpolationMode;
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
	 * Return randomised gradient (random colors and stop positions).
	 * 
	 * @param numcolors
	 * @return
	 */
	public static Gradient randomGradientWithStops(int numcolors) {
		ColorStop[] temp = new ColorStop[numcolors];
		float percent;
		for (int i = 0; i < numcolors; ++i) {
			percent = i == 0 ? 0 : i == numcolors - 1 ? 1 : (float) Math.random();
			temp[i] = new ColorStop(Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1), percent);
		}
		return new Gradient(temp);
	}

	/**
	 * Return randomised gradient (random colors; equidistant stops). TODO use
	 * palette instead?
	 * 
	 * @param numcolors
	 * @return
	 */
	public static Gradient randomGradient(int numcolors) {
		int[] temp = new int[numcolors];
		for (int i = 0; i < numcolors; ++i) {
			temp[i] = Functions.composeclr((float) Math.random(), (float) Math.random(), (float) Math.random(), 1);
		}
		return new Gradient(temp);
	}

	/**
	 * Returns detailed information about the gradient. For each color stop,
	 * returns:
	 * <p>
	 * <ul>
	 * <li>Position
	 * <li>RGBA representation
	 * <li>Integer representation
	 * <li>[Current color Space] Representation
	 * </ul>
	 * <p>
	 * 
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Gradient\n");
		sb.append("Offset: " + offset + "\n");
		sb.append("color Stops (" + colorStops.size() + "):\n");
		sb.append(String.format("    " + "%-10s%-25s%-12s%-20s\n", "Position", "RGBA", "clrInteger", "clrCurrentColSpace"));
		for (ColorStop colorStop : colorStops) {
			sb.append(String.format("    " + "%-10s%-25s%-12s%-20s\n", colorStop.percent,
					Functions.formatArray(Functions.decomposeclrRGBDouble(colorStop.clr, colorStop.alpha), 0, 2), colorStop.clr,
					Functions.formatArray(colorStop.getcolor(colorSpace), 2, 3)));
		}
		return sb.toString();
	}

	/**
	 * Java source to paste. Use this if a randomly generated gradient is pleasant.
	 * Export ready to construct the gradient using Processing color().
	 * 
	 * @return eg "Gradient(color(0, 0, 50), color(125, 55, 25));"
	 */
	public String toJavaConstructor() {
		StringBuilder sb = new StringBuilder();
		sb.append("Gradient(");

		Iterator<ColorStop> iterator = colorStops.iterator();
		ColorStop c = iterator.next();
		sb.append("color" + Functions.formatArray(Functions.decomposeclrRGBDouble(c.clr, c.alpha), 0, 0, '(', ')'));

		while (iterator.hasNext()) {
			c = iterator.next();
			sb.append(", ");
			sb.append("color" + Functions.formatArray(Functions.decomposeclrRGBDouble(c.clr, c.alpha), 0, 0, '(', ')'));
		}

		sb.append(");");
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
				return 3 * step * step - 2 * step * step * step; // polynomial approximation of (0.5-cos(PI*step)/2)
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
