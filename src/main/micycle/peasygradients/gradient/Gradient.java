package micycle.peasygradients.gradient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import micycle.peasygradients.colorspace.ColorSpaceTransform;
import micycle.peasygradients.colorspace.ColorSpace;
import micycle.peasygradients.utilities.ColorUtils;
import micycle.peasygradients.utilities.FastPow;
import micycle.peasygradients.utilities.Functions;
import micycle.peasygradients.utilities.Interpolation;
import net.jafama.FastMath;

/**
 * Represents a one-dimensional color gradient specification, defined by a
 * sequence of {@link micycle.peasygradients.gradient.ColorStop color stops}
 * along a normalized axis [0,1]. Each color stop defines both a color value and
 * its position on this axis.
 *
 * <p>
 * The gradient specification consists of two key components:
 * <ul>
 * <li>An interpolation function (e.g. linear, sine) that determines how colors
 * transition between stops during sampling
 * <li>A color space (e.g. RGB, LAB) in which the color interpolation is
 * performed
 * </ul>
 *
 * <p>
 * This class defines only the mathematical gradient function - it does not
 * handle the actual rendering or rasterization. To generate a two-dimensional
 * visualization of this gradient, use a {@link micycle.peasygradients.PeasyGradients PeasyGradients}
 * instance, which will sample this function to create a 2D spectrum.
 *
 * @author Michael Carleton
 *
 */
public final class Gradient {

	// TODO export as JSON / load from JSON

	private List<ColorStop> colorStops = new ArrayList<>(); // TODO add get method

	private double[] interpolatedcolorOUT = new double[4]; // define once here

	private double offset = 0; // animation color offset 0...1

	private int lastCurrStopIndex;
	private ColorStop currStop, prevStop;
	private double denom;

	public ColorSpace colorSpace = ColorSpace.OKLAB; // TODO public for testing
	private ColorSpaceTransform colorSpaceInstance = colorSpace.getColorSpace();
	public Interpolation interpolationMode = Interpolation.SMOOTH_STEP; // TODO public for testing

	/**
	 * Creates a new gradient consisting of 2 random equidistant complementary
	 * colors.
	 */
	public Gradient() {
		this(Palette.complementary()); // random 2 colors
	}

	/**
	 * Creates a gradient with equidistant color stops, using the colors provided.
	 * 
	 * @param colors ARGB color integers (the kind returned by Processing's color()
	 *               method)
	 */
	public Gradient(int... colors) {
		int sz = colors.length;
		double szf = (sz <= 1.0 ? 1.0 : sz - 1.0);
		for (int i = 0; i < sz; i++) {
			colorStops.add(new ColorStop(colors[i], i / szf));
		}
		this.colorStops.forEach(c -> c.setColorSpace(colorSpace));

		currStop = colorStops.get(0);
		prevStop = colorStops.get(0);
		lastCurrStopIndex = 0;
	}

	/**
	 * Creates a gradient using the colorstops provided.
	 * 
	 * @param colorStops varargs of colorstops
	 */
	public Gradient(ColorStop... colorStops) {
		int sz = colorStops.length;
		for (int i = 0; i < sz; i++) {
			add(colorStops[i]);
		}

		Collections.sort(this.colorStops);
		this.colorStops.forEach(c -> c.setColorSpace(colorSpace));

		currStop = this.colorStops.get(0);
		prevStop = this.colorStops.get(0);
		lastCurrStopIndex = 0;
	}

	/**
	 * Creates a gradient using the colorstops provided.
	 * 
	 * @param colorStops a list of colorstops
	 */
	public Gradient(List<ColorStop> colorStops) {
		this.colorStops = new ArrayList<>(colorStops);
		Collections.sort(this.colorStops);
		this.colorStops.forEach(c -> c.setColorSpace(colorSpace));

		currStop = colorStops.get(0);
		prevStop = colorStops.get(0);
		lastCurrStopIndex = 0;
	}

	/**
	 * Evalutes the ARGB (Processing) color value of the gradient at the given step
	 * through its 1D color axis.
	 * 
	 * <p>
	 * This is the main method of Gradient class. Internally, the the position input
	 * undergoes is transformed by the current interpolation function.
	 * 
	 * @param position a linear position expressed as a decimal between 0 and 1.
	 *                 Numbers outside the range of 0...1 will wrap back into the
	 *                 gradient
	 * @return ARGB integer for Processing pixel array.
	 */
	public int getColor(double position) {

		// TODO a version which writes to PeasyGradients int[] array (i.e. does all at
		// once)

		position += offset;
		if (position < 0) { // (if animation offset negative)
			position += 1; // equivalent to floormod function
		}
		if (position > 1) { // 1 % 1 == 0, which we want to avoid
			position %= 1;
		}

//		position = functStep(position); // apply interpolation function globally

		/*
		 * Calculate whether the current step has gone beyond the existing color stop
		 * boundary (either above or below). If the first color stop is at a position >
		 * 0 or last color stop at a position < 1, then when step > currStop.percent or
		 * step < currStop.percent is true, we don't want to inc/decrement currStop.
		 */
		if (position > currStop.position) { // if at end, stay, otherwise next
			if (lastCurrStopIndex == (colorStops.size() - 1)) {
				prevStop = colorStops.get(lastCurrStopIndex);
				denom = 1;
			} else {
				do {
					lastCurrStopIndex++; // increment
					currStop = colorStops.get(lastCurrStopIndex);
					// sometimes step might jump more than 1 color, hence while()
				} while (position > currStop.position && lastCurrStopIndex < (colorStops.size() - 1));
				prevStop = colorStops.get(lastCurrStopIndex - 1);

				denom = 1 / (prevStop.position - currStop.position); // compute denominator inverse
			}

		} else if (position < prevStop.position) {
			if (lastCurrStopIndex == 0) { // if at zero stay, otherwise prev
				denom = 1;
				currStop = colorStops.get(0);
			} else {
				do {
					lastCurrStopIndex--; // decrement
					prevStop = colorStops.get(Math.max(lastCurrStopIndex - 1, 0));
				} while (position < prevStop.position); // sometimes step might jump back more than 1 color

				currStop = colorStops.get(lastCurrStopIndex);

				denom = 1 / (prevStop.position - currStop.position); // compute denominator inverse
			}
		}

		/*
		 * When the getColor() method is called with a monotonically increasing
		 * position/step (which happens when we pre-compute interpolated color results
		 * into a LUT within PeasyGradients methods) this simpler approach below works
		 * to find adjacent colorstops. HOWEVER, this approach doesn't work when
		 * animating the gradient, since the position value is no longer monotonic,
		 * HENCE it's commented out and we need the longer approach above.
		 */
//		if (step > currStop.percent && lastCurrStopIndex != (colorStops.size() - 1) ) {
//			prevStop = colorStops.get(lastCurrStopIndex);
//			lastCurrStopIndex++; // increment
//			currStop = colorStops.get(lastCurrStopIndex);
//			denom = 1 / (prevStop.percent - currStop.percent); // compute denominator inverse
//		}

		/*
		 * NOTE this approach applies the easing function between adjacent color stops,
		 * and not globally. TODO apply easing function to the raw position, not the
		 * color-stop-dependent position?
		 */
		double smoothStep = functStep((position - currStop.position) * denom); // apply interpolation function between colorstops
//		double smoothStep = (position - currStop.position) * denom; // applicable when applying interpolation function globally

		/*
		 * Calculate the interpolated color in the given colorspace by using the two
		 * colorstops adjacent to the position, and the (eased) step between the two
		 * colorstops as the weighting.
		 */
		interpolatedcolorOUT = colorSpaceInstance.interpolateLinear(currStop.colorOut, prevStop.colorOut, smoothStep);
		/*
		 * Treat alpha separately (to simplify colorspace classes)
		 */
		int alpha = (int) Math.floor((prevStop.alpha + (position * (currStop.alpha - prevStop.alpha))) + 0.5d); // TODO sometimes
																												// 254?

		/*
		 * Finally convert the given colorspace value to sARGB int to eventually write
		 * to Processing's pixels[] array
		 */
		return ColorUtils.RGB1ToRGBA255Clamp(colorSpaceInstance.toRGB(interpolatedcolorOUT), alpha);
	}

	/**
	 * Sets the color of a color stop (given by its index).
	 * 
	 * @param stopIndex
	 * @param col       ARGB color integer representation
	 */
	public void setStopColor(int stopIndex, int col) {
		if (stopIndex > -1 && stopIndex < colorStops.size()) {
			colorStops.get(stopIndex).setColor(col);
		} else {
			System.err.println("Color stop index out of bounds.");
		}
	}

	/**
	 * Sets the 1D position of a color stop (given by its index) to a certain
	 * position on the 1D gradient axis. Positions &lt; 0 or &gt; 1 will wrap around the
	 * gradient.
	 * 
	 * @param index
	 * @param position
	 */
	public void setStopPosition(int index, double position) {
		if (index > -1 && index < colorStops.size()) {
			if (position < 0) { // (if animation offset negative)
				position = Math.abs(position); // make positive
			}
			if (position > 1) { // 1 % 1 == 0, which we want to avoid
				position %= 1;
			}
			colorStops.get(index).setPosition(position);
			Collections.sort(colorStops);
		} else {
			System.err.println("Color stop index out of bounds.");
		}
	}

	/**
	 * Reverses the order of colors in this gradient. This mutates the gradient's
	 * colorstops' positions.
	 */
	public void reverse() {
		colorStops.forEach(s -> s.position = 1 - s.position);
		Collections.sort(colorStops);
	}

	/**
	 * Increases the positional offset of all color stops by the amount given (call
	 * this each frame (within draw() for example) to animate a gradient).
	 * 
	 * @param amt 0...1 smaller is less change
	 * @see #setOffset(double)
	 * @see #primeAnimation() primeAnimation() -- consider calling this method
	 *      before animate() to prevent a color seam
	 */
	public void animate(double amt) {
		offset += amt;
		offset %= 1;
	}

	/**
	 * Sets the offset of <b>all color stops</b> to a specific value.
	 * 
	 * @param offset
	 * @see #animate(double)
	 */
	public void setOffset(double offset) {
		this.offset = offset;
	}

	/**
	 * Mutates the color of all color stops in the RGB255 space by the amount given.
	 * Mutation randomises between adding or subtracting the mutation amount from
	 * each of the R,G,B channels.
	 * 
	 * @param amt magnitude of mutation [0...255]
	 */
	public void mutatecolor(double amt) {
		colorStops.forEach(c -> c.mutate(amt));
	}

	/**
	 * Primes the gradient for animation (pushes copy of the first color in the
	 * gradient to the end, and repositions all other color stops proportionally to
	 * where they were before), to ensure a seamless gradient spectrum, regardless
	 * of offset.
	 * <p>
	 * Animating a gradient without calling {@link #primeAnimation()} may lead to an
	 * ugly and undesirable seam in the gradient where the first and last color
	 * stops (at positions 0.00 and 1.00 respectively) bump right up against each
	 * other.
	 */
	public void primeAnimation() {
		pushColor(colorAt(0));
	}

	/**
	 * Pushes a new color stop to the end of the gradient (position = 1.00), and
	 * repositions all other color stops proportionally to where they were before.
	 * 
	 * @param color
	 * @see #removeLast()
	 */
	public void pushColor(int color) {
		for (ColorStop colorStop : colorStops) {
			colorStop.position *= ((colorStops.size() - 1) / (double) colorStops.size()); // scale down existing stop positions
		}
		add(color, 1);
	}

	/**
	 * Removes the last color stop from the gradient and scales the position of the
	 * remaining stops such that the position of the previous second-to-last color
	 * stop is equal to the position of the removed stop.
	 * 
	 * @see #pushColor(int)
	 */
	public void removeLast() { // TODO rename pop and return colorstop?
		if (colorStops.size() > 2) { // don't go below a 2 color gradient
			ColorStop last = colorStops.get(colorStops.size() - 1);
			colorStops.remove(colorStops.size() - 1);

			final double scaleFactor = last.position / colorStops.get(colorStops.size() - 1).position;

			for (ColorStop colorStop : colorStops) {
				colorStop.position *= scaleFactor; // scale up remaining stop positions
			}
		} else {
			System.err.println("This gradient has only 2 colors. No more colors can be removed from this gradient.");
		}
	}

	/**
	 * Returns the ARGB color of the color stop at a given index.
	 * 
	 * @param colorStopIndex
	 * @return 32bit ARGB color int
	 */
	public int colorAt(int colorStopIndex) {
		return colorStops.get(colorStopIndex).clr;
	}

	/**
	 * Returns the color of the this gradient's last color stop.
	 * 
	 * @return 32bit ARGB color int
	 */
	public int lastcolor() {
		return colorStops.get(colorStops.size() - 1).clr;
	}

	/**
	 * Adds a specific color to the gradient at a given percentage.
	 * 
	 * @param clr
	 * @param percent 0...1
	 */
	public void add(final int clr, final double percent) {
		add(new ColorStop(clr, percent));
	}

	/**
	 * Adds a color stop to the gradient.
	 * 
	 * @param colorStop
	 */
	public void add(final ColorStop colorStop) {
		colorStops.add(colorStop);
		Collections.sort(colorStops); // sort color stops by position
		colorStop.setColorSpace(colorSpace);
	}

	/**
	 * Sets the color space this gradient uses to represent colors. This affects the
	 * appearance of the gradient when it is rendered as a 2D spectrum.
	 * 
	 * @param colorSpace
	 */
	public void setColorSpace(ColorSpace colorSpace) {
		// TODO color space is defined for user at peasyGradients level, not gradient?
		this.colorSpace = colorSpace;
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setColorSpace(colorSpace));
	}

	public void nextColSpace() {
		colorSpace = colorSpace.next();
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setColorSpace(colorSpace));
	}

	public void prevColSpace() {
		colorSpace = colorSpace.prev();
		colorSpaceInstance = colorSpace.getColorSpace();
		colorStops.forEach(c -> c.setColorSpace(colorSpace));
	}

	/**
	 * Sets the interpolation mode this gradient uses to generate colors between
	 * adjacent color stops. This affects the appearance of the gradient when it is
	 * rendered as a 2D spectrum.
	 * 
	 * @param interpolation
	 */
	public void setInterpolationMode(Interpolation interpolation) {
		this.interpolationMode = interpolation;
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
	 * Returns a randomised gradient (comprised using random colors and random
	 * positions).
	 * 
	 * @param numColors the number of colors the gradient is comprised of
	 * @return a randomised gradient
	 * @see #randomGradient(int)
	 */
	public static Gradient randomGradientWithStops(int numColors) {
		ColorStop[] temp = new ColorStop[numColors];
		int[] colors = Palette.randomcolors(numColors);
		double percent;
		for (int i = 0; i < numColors; ++i) {
			percent = i == 0 ? 0 : i == numColors - 1 ? 1 : Math.random();
			temp[i] = new ColorStop(colors[i], percent);
		}
		return new Gradient(temp);
	}

	/**
	 * Return randomised gradient (comprised using random colors but at equidistant
	 * positions).
	 * 
	 * @param numColors the number of colors the gradient is comprised of
	 * @return a randomised gradient
	 * @see #randomGradientWithStops(int)
	 */
	public static Gradient randomGradient(int numColors) {
		return new Gradient(Palette.randomcolors(numColors));
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
		//@formatter:off
		StringBuilder sb = new StringBuilder();
		// info
	    sb.append("Gradient ").append(super.toString()).append('\n')
	      .append("Offset: ").append(offset).append('\n')
	      .append("Current Color Space: ").append(colorSpace).append('\n')
	      .append("Color Stops (n=").append(colorStops.size()).append("):\n");

	    String tableFormat = "  %-10s%-25s%-12s%-20s%n";
	    
	    // color stops header
	    sb.append(String.format(tableFormat,
	        "Position",
	        "RGBA",
	        "clrInteger",
	        "clrCurrentColSpace"));
	    // add color stop info
	    for (ColorStop stop : colorStops) {
	        double[] rgbaValues = ColorUtils.decomposeclrRGBDouble(stop.clr, stop.alpha);
	        double[] colorSpaceValues = stop.getColor(colorSpace);
	        
	        sb.append(String.format(tableFormat,
	            stop.position,
	            Functions.formatArray(rgbaValues, 0, 2),
	            stop.clr,
	            Functions.formatArray(colorSpaceValues, 2, 3)));
	    }
	    //@formatter:on	    
		return sb.toString();
	}

	/**
	 * Returns Java source to paste. Call this method if a randomly generated
	 * gradient is pleasant. Export ready to construct the gradient using Processing
	 * color().
	 * 
	 * @return a constructor for this gradient, e.g. "Gradient(color(0, 0, 50),
	 *         color(125, 55, 25));"
	 */
	public String toJavaConstructor() {
		StringBuilder sb = new StringBuilder();
		sb.append("Gradient(");

		Iterator<ColorStop> iterator = colorStops.iterator();
		ColorStop c = iterator.next();
		sb.append("color" + Functions.formatArray(ColorUtils.decomposeclrRGBDouble(c.clr, c.alpha), 0, 0, '(', ')'));

		while (iterator.hasNext()) {
			c = iterator.next();
			sb.append(", ");
			sb.append("color" + Functions.formatArray(ColorUtils.decomposeclrRGBDouble(c.clr, c.alpha), 0, 0, '(', ')'));
		}

		sb.append(");");
		return sb.toString();
	}

	/**
	 * Calculates the eased step by passing the original (linear) step to the
	 * Gradient's current interpolation function. Allows gradient renderer to easily
	 * change how the gradient is smoothed.
	 * 
	 * @param step 0...1
	 * @return the eased/transformed step (0...1)
	 */
	private double functStep(final double step) {
		switch (interpolationMode) {
			case LINEAR:
				return step;
			case IDENTITY:
				return step * step * (2.0 - step);
			case SMOOTH_STEP:
				return 3 * step * step - 2 * step * step * step; // polynomial approximation o (0.5-cos(PI*step)/2)
			case SMOOTHER_STEP:
				return step * step * step * (step * (step * 6 - 15) + 10);
			case EXPONENTIAL:
				return step == 1.0 ? step : 1.0 - FastPow.fastPow(2, -10 * step);
			case CUBIC:
				return step * step * step;
			case BOUNCE:
				double sPrime = step;

				if (sPrime < 0.36364) { // 1/2.75
					return 7.5625 * sPrime * sPrime;
				}
				if (sPrime < 0.72727) // 2/2.75
				{
					return 7.5625 * (sPrime -= 0.545454) * sPrime + 0.75;
				}
				if (sPrime < 0.90909) // 2.5/2.75
				{
					return 7.5625 * (sPrime -= 0.81818) * sPrime + 0.9375;
				}
				return 7.5625 * (sPrime -= 0.95455) * sPrime + 0.984375;
			case CIRCULAR:
				return Math.sqrt((2.0 - step) * step);
			case SINE:
				return FastMath.sinQuick(step);
			case PARABOLA:
				return Math.sqrt(4.0 * step * (1.0 - step));
			case GAIN1:
				if (step < 0.5) {
					return 0.5 * FastPow.fastPow(2.0 * step, 0.3);
				} else {
					return 1 - 0.5 * FastPow.fastPow(2.0 * (1 - step), 0.3);
				}
			case GAIN2:
				if (step < 0.5) {
					return 0.5 * FastPow.fastPow(2.0 * step, 3.3333);
				} else {
					return 1 - 0.5 * FastPow.fastPow(2.0 * (1 - step), 3.3333);
				}
			case EXPIMPULSE:
				return (2 * step * FastMath.expQuick(1.0 - (2 * step)));
			case HEARTBEAT:
				final double v = FastMath.atan(FastMath.sinQuick(step * Math.PI * 1) * 6); // requency = 1; intensity = 6
				return (v + Math.PI / 2) / Math.PI;
			default:
				return step;
		}
	}

}
