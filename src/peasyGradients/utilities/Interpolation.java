package peasyGradients.utilities;

/**
 * Interpolation functions affect the interpolation factor (0.0...1.0) between any 2 colour
 * stops and therefore the appearance (ramp) of a gradient.
 * 
 * <P>
 * For example, in linear mode, a point mid-way between two control points will
 * use 50% of the left and 50% of the right; in exponential interpolation, the
 * same point be an average of x% of left colour and y% of right colour.
 * 
 * @author micycle1
 *
 */
public enum Interpolation {

	/**
	 * No transformation; completely linear.
	 */
	LINEAR,
	/**
	 * Almost Unit Identity
	 * 
	 * This is another near-identiy function, but this one maps the unit interval to
	 * itself. But it is special in that not only remaps 0 to 0 and 1 to 1, but has
	 * a 0 derivative at the origin and a derivative of 1 at 1, making it ideal for
	 * transitioning things from being stationary to being in motion as if they had
	 * been in motion the whole time.
	 * 
	 * https://www.iquilezles.org/www/articles/functions/functions.htm
	 */
	IDENTITY,
	/*
	 * 
	 */
	SMOOTH_STEP,
	/**
	 * Ken Perlin’s smoother step, a simoid like function.
	 * 
	 * @param st step, between 0 and 1
	 * @return the new mapped step, having undergone a transformation according to a
	 *         sigmoid-like function (eg: [0.5 -> 0.5], [0.25 -> 0.104], [0.65
	 *         ->0.765])
	 */
	SMOOTHER_STEP,
	/*
	 * 
	 */
	EXPONENTIAL,
	/*
	 * 
	 */
	CUBIC,
	/**
	 * Provides a reversible parabolic bouncing easing out function. From t=0 value
	 * starts with an accelerating motion until destination reached then it bounces
	 * back in increasingly small bounces finally settling at 1 when t=1. If the
	 * <code>direction</code> parameter is negative, the direction of the function
	 * is reversed. This can be useful for oscillating animations.
	 */
	BOUNCE,
	/*
	 * 
	 */
	CIRCULAR,
	/**
	 * 
	 */
	SINE,
	/**
	 * 
	 */
	PARABOLA,
	/**
	 * Remapping the unit interval into the unit interval by expanding the sides and
	 * compressing the center, and keeping 1/2 mapped to 1/2, that can be done with
	 * the gain() function. K = 0.3
	 */
	GAIN1,
	/**
	 * As above, but with K = 3
	 */
	GAIN2,
	/**
	 * Exponential Impulse
	 * 
	 * Great for triggering behaviours or making envelopes for music or animation,
	 * and for anything that grows fast and then slowly decays. Use k to control the
	 * stretching of the function. Its maximum, which is 1, happens at exactly x =
	 * 1/k.
	 */
	EXPIMPULSE;

	private final static Interpolation[] vals = values();

	/**
	 * Switches to the next interpolation mode.
	 * 
	 * @return
	 */
	public Interpolation next() {
		return vals[(ordinal() + 1) % vals.length];
	}

	/**
	 * Switches to the previous interpolation mode.
	 * 
	 * @return
	 */
	public Interpolation prev() {
		return vals[Math.floorMod((ordinal() - 1), vals.length)];
	}

}
