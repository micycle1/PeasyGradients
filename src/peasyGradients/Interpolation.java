package peasyGradients;

public enum Interpolation {

	/**
	 * No transformation, linear gradient
	 */
	LINEAR,
	/**
	 * Ken Perlin’s smoother step, a simoid like function.
	 * 
	 * @param st step, between 0 and 1
	 * @return the new mapped step, having undergone a transformation according to a
	 *         sigmoid-like function (eg: [0.5 -> 0.5], [0.25 -> 0.104], [0.65
	 *         ->0.765])
	 */
	KPERLIN, EXPONENTIAL, CUBIC,
	/**
	 * Provides a reversible parabolic bouncing easing out function. From t=0 value
	 * starts with an accelerating motion until destination reached then it bounces
	 * back in increasingly small bounces finally settling at 1 when t=1. If the
	 * <code>direction</code> parameter is negative, the direction of the function
	 * is reversed. This can be useful for oscillating animations.
	 */
	BOUNCE,
	/**
	 * Provides an elastic easing out function simulating an increasingly agitated
	 * elastic. From t=0 value starts at 0.5 with increasingly large perturbations
	 * ending at 1 when t=1.
	 */
	ELASTIC, CIRCULAR, SINE;

	private final static Interpolation[] vals = values();

	public Interpolation next() {
		return vals[(ordinal() + 1) % vals.length];
	}
}
