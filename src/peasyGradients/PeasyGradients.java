package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.lang.reflect.Constructor;
import net.jafama.FastMath;

import peasyGradients.gradient.Gradient;
import peasyGradients.utilities.FastNoiseLite;
import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;
import peasyGradients.utilities.FastNoiseLite.CellularDistanceFunction;
import peasyGradients.utilities.FastNoiseLite.CellularReturnType;
import peasyGradients.utilities.FastNoiseLite.FractalType;
import peasyGradients.utilities.FastNoiseLite.NoiseType;

/**
 * Renders 1D {@link Gradient gradients} as 2D spectrums in your Processing
 * sketch.
 * 
 * <p>
 * The class offers both quick constructors for more simple gradients (such as 2
 * color horizontal gradients) and more powerful constructors for more involved
 * gradients (centre-offset, angled, n-color gradient with color stops).
 * 
 * <p>
 * By default, a PeasyGradients instance draws directly into the Processing
 * sketch; you can give it a specific <code>PGraphics</code> pane to draw into
 * with the <code>.setRenderTarget()</code> method).
 * 
 * <p>
 * TODO set color interpolation method shape, TODO interpolation mode in this
 * class!
 * 
 * <p>
 * https://helpx.adobe.com/illustrator/using/gradients.html#create_apply_freeform_gradient
 * for examples
 * 
 * <p>
 * Algorithms for linear, radial & conic gradients are based on <a href=
 * "https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2">this</a>
 * work by Jeremy Behreandt; all others are mostly my own derivation.
 * 
 * @author micycle1
 *
 */
public final class PeasyGradients {

	private static final double TWO_PI = (2 * Math.PI);
	private static final float TWO_PIf = (float) (2 * Math.PI);
	private static final float PIf = (float) Math.PI;
	private static final double HALF_PI = (0.5f * Math.PI);
	private static final float INV_TWO_PI = 1f / PConstants.TWO_PI;
	private static final float THREE_QRTR_PI = (float) (0.75f * Math.PI);

	private static final ExecutorService THREAD_POOL;

	private static final int cpuThreads = Runtime.getRuntime().availableProcessors();

	static {
		/**
		 * Initalise FastPow LUT. Lower numbers are better due to cache hits;
		 * JAB+ITP_fast requires high value
		 */
		FastPow.init(11);

		/**
		 * Create a static thread pool (shared across all PeasyGradient instances), with
		 * at most #systemCores threads.
		 */
		THREAD_POOL = Executors.newFixedThreadPool(cpuThreads);
	}

	private final FastNoiseLite fastNoiseLite = new FastNoiseLite(0); // create noise generator using a fixed default seed (0)

	private final PApplet p; // reference to host Processing sketch (PApplet)
//	int colorMode = PConstants.RGB; // TODO colour mode in this class?
	private PImage gradientPG; // reference to the PGraphics object to render gradients into

	private int[] gradientCache; // a cache to store gradient colors (ARGB ints)
	private int gradientCacheSize; // size of cache

	private int renderHeight, renderWidth; // gradient region dimensions (usually the dimensions of gradientPG)
	private int renderOffsetX, renderOffsetY; // gradient region offsets (usually 0, 0)
	private float scaleY, scaleX;

	private int renderPartitionsX = Math.max(cpuThreads / 4, 1); // number of thread render partitions in the x (horizontal) direction
	private int renderPartitionsY = Math.max(cpuThreads / 4, 1); // number of thread render partitions in the y (vertical) direction

	/**
	 * Constructs a new PeasyGradients renderer from a running Processing sketch.
	 * 
	 * @param p the Processing sketch. You'll usually refer to it as {@code this}
	 */
	public PeasyGradients(PApplet p) {
		this.p = p;

		renderIntoSketch(); // render into parent sketch (full size) by default

		fastNoiseLite.SetCellularReturnType(CellularReturnType.Distance2Div);
		fastNoiseLite.SetCellularDistanceFunction(CellularDistanceFunction.EuclideanSq);
		fastNoiseLite.SetFractalPingPongStrength(1);
	}

	/**
	 * Tells this PeasyGradients renderer to render 2D gradients into the Processing
	 * sketch (spanning the full size of the sketch).
	 * 
	 * @see #renderIntoSketch(int, int, int, int)
	 * @see #setRenderTarget(PImage)
	 */
	public void renderIntoSketch() {
		renderIntoSketch(0, 0, p.width, p.height);
	}

	/**
	 * Tells this PeasyGradients renderer to render 2D gradients into the Processing
	 * sketch, within a certain region specified by input arguments.
	 * 
	 * @param offSetX x-axis offset of the region to render gradients into (where 0
	 *                is top-left corner)
	 * @param offSetY y-axis offset of the region to render gradients into (where 0
	 *                is top-left corner)
	 * @param width   width of region to render gradients into
	 * @param height  height of region to render gradients into
	 * @see #renderIntoSketch()
	 */
	public void renderIntoSketch(int offSetX, int offSetY, int width, int height) {
		if (offSetY < 0 || offSetY < 0 || (width + offSetX) > p.width || (offSetY + height) > p.height) {
			System.err.println("Invalid parameters.");
			return;
		}
		setRenderTarget(p.getGraphics(), offSetX, offSetY, width, height);
	}

	/**
	 * Tells this PeasyGradients renderer to render 2D gradients into the PImage or
	 * PGraphics object provided by the user.
	 * 
	 * @param g PImage or PGraphics object to render gradients into
	 * @see #renderIntoSketch()
	 */
	public void setRenderTarget(PImage g) {
		setRenderTarget(g, 0, 0, g.width, g.height);
	}

	private void setRenderTarget(PImage g, int offSetX, int offSetY, int width, int height) {

		final int actualWidth = width - offSetX;
		final int actualHeight = height - offSetY;

		scaleX = g.width / (float) width; // used for correct rendering increment for some gradients
		scaleY = g.height / (float) height; // used for correct rendering increment for some gradients

		renderWidth = width;
		renderHeight = height;
		renderOffsetX = offSetX;
		renderOffsetY = offSetY;

		if (!g.isLoaded()) { // load pixel array if not already done
			if (g instanceof PGraphics) {
				((PGraphics) g).beginDraw();
			}
			g.loadPixels(); // only needs to be called once
//			g.endDraw(); // commented out -- if called on PApplet during draw loop, will prevent being drawn to until next draw
		}

		gradientPG = g;

		gradientCacheSize = (int) (2 * Math.ceil(Math.max(actualWidth, actualHeight)));
		gradientCache = new int[gradientCacheSize + 1];
	}

	/**
	 * Changes the noise seed used by noise gradients.
	 * 
	 * @param seed any integer; PeasyGradients default is 0.
	 */
	public void setNoiseSeed(int seed) {
		fastNoiseLite.SetSeed(seed);
	}

	/**
	 * Restricts any and all rendered gradients to render in at most n colors
	 * (a.k.a. posterisation).
	 * 
	 * @param n max number of colors
	 * @see #clearPosterisation()
	 */
	public void posterise(int n) {
		if (n != gradientCacheSize) {
			gradientCacheSize = n;
			gradientCacheSize = n;
			gradientCache = new int[n + 1];
			gradientCache = new int[n + 1];
		}
	}

	/**
	 * Clears any user-defined color posterisation setting.
	 * 
	 * @see #posterise(int)
	 */
	public void clearPosterisation() {
		setRenderTarget(gradientPG, renderOffsetX, renderOffsetY, renderWidth, renderHeight);
	}

	/**
	 * Renders a linear gradient (with its midpoint at the centre of the
	 * sketch/render target).
	 * 
	 * <p>
	 * It's called 'linear' because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient 1D {@link Gradient gradient} to use as the basis for the
	 *                 linear gradient
	 * @param angle    radians. East is 0 (meaning gradient will change color from
	 *                 west to east, meaning each line of color is drawn parallel to
	 *                 the angle); moves clockwise
	 */
	public void linearGradient(Gradient gradient, float angle) {

		PVector centerPoint = new PVector(gradientPG.width / 2, gradientPG.height / 2);
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);

		float modAngle = angle % TWO_PIf;

		if (modAngle > PConstants.HALF_PI && modAngle <= PConstants.HALF_PI * 3) {
			linearGradient(gradient, o[0], o[1]);
		} else {
			linearGradient(gradient, o[1], o[0]);
		}
	}

	/**
	 * Renders a linear gradient with a given gradient midpoint. The start and end
	 * points (first and last colours) will be automatically constrained to the
	 * edges of the sketch boundary.
	 * 
	 * <p>
	 * It's called 'linear' because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    linear gradient
	 * @param centerPoint
	 * @param angle       in radians
	 */
	public void linearGradient(Gradient gradient, PVector centerPoint, float angle) {

		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);

		float modAngle = angle % TWO_PIf;

		if (modAngle > PConstants.HALF_PI && modAngle <= PConstants.HALF_PI * 3) {
			linearGradient(gradient, o[0], o[1]);
		} else {
			linearGradient(gradient, o[1], o[0]);
		}
	}

	/**
	 * Renders a linear gradient using a given gradient centerpoint, angle and
	 * length.
	 * 
	 * <p>
	 * It's called 'linear' because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    linear gradient
	 * @param centerPoint gradient midpoint
	 * @param angle       in radians
	 * @param length      coefficient to lerp from centrepoint to edges (that
	 *                    intersect with angle). default = 1: (first and last colors
	 *                    will be exactly on edge); <1: colors will be sqashed; >1
	 *                    colors spread out (outermost colors will leave the view).
	 */
	public void linearGradient(Gradient gradient, PVector centerPoint, float angle, float length) {
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		o[0].lerp(centerPoint, 1 - length); // mutate length
		o[1].lerp(centerPoint, 1 - length); // mutate length

		float modAngle = angle % TWO_PIf;

		if (modAngle > PConstants.HALF_PI && modAngle <= PConstants.HALF_PI * 3) {
			linearGradient(gradient, o[0], o[1]);
		} else {
			linearGradient(gradient, o[1], o[0]);
		}
	}

	/**
	 * Renders a linear gradient using two user-defined control points, specifying
	 * the position of the first and last colors (the angle of the gradient is the
	 * angle between the two control points).
	 * 
	 * @param gradient      1D {@link Gradient gradient} to use as the basis for the
	 *                      linear gradient
	 * @param controlPoint1 the location for the start point, using X (horizontal)
	 *                      and Y (vertical) values (can extend past the coordinates
	 *                      of the sketch)
	 * @param controlPoint2 location of the end point of the gradient (can extend
	 *                      past the coordinates of the sketch)
	 */
	public void linearGradient(Gradient gradient, PVector controlPoint1, PVector controlPoint2) {

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		/**
		 * Pre-compute vals for linearprojection
		 */
		float odX = controlPoint2.x - controlPoint1.x; // Rise and run of line.
		float odY = controlPoint2.y - controlPoint1.y; // Rise and run of line.
		final float odSqInverse = 1 / (odX * odX + odY * odY); // Distance-squared of line.
		float opXod = -controlPoint1.x * odX + -controlPoint1.y * odY;

		makeThreadPool(renderPartitionsX, renderPartitionsY, LinearThread.class, odX, odY, odSqInverse, opXod);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a radial gradient.
	 * 
	 * <p>
	 * A radial gradient starts at a single point (the source point for color stops)
	 * and radiates outwards.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    radial gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param zoom        default = 1
	 */
	public void radialGradient(Gradient gradient, PVector centerPoint, float zoom) {

		final float hypotSq = (renderWidth * renderWidth) + (renderHeight * renderHeight);

		zoom = 4 / zoom; // calc here, not in loop
		zoom /= hypotSq; // calc here, not in loop

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		makeThreadPool(renderPartitionsX, renderPartitionsY, RadialThread.class, renderMidpointX, renderMidpointY, zoom);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a conic gradient.
	 * 
	 * <p>
	 * A conic gradient is similar to a radial gradient. Both are circular and use
	 * the centerPoint as the source point for color stops. However, where the color
	 * stops of a radial gradient emerge from the center of the circle, a conic
	 * gradient places them around the circle. Aka "angled".
	 * 
	 * <p>
	 * They're called 'conic' because they tend to look like the shape of a cone
	 * that is being viewed from above. Well, at least when there is a distinct
	 * angle provided and the contrast between the color values is great enough to
	 * tell a difference.
	 * 
	 * <p>
	 * This method creates a hard stop where the first and last colors bump right up
	 * to one another. See
	 * {@link #conicGradientSmooth(Gradient, PVector, float, float)
	 * conicGradientSmooth()} to render a conic gradient with a smooth transition
	 * between the first and last colors.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    conic gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @see #conicGradientSmooth(Gradient, PVector, float, float)
	 */
	public void conicGradient(Gradient gradient, PVector centerPoint, float angle) {

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		makeThreadPool(renderPartitionsX, renderPartitionsY, ConicThread.class, renderMidpointX, renderMidpointY, angle);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a spiral gradient
	 * 
	 * <p>
	 * A spiral gradient builds upon the conic gradient: instead of colors extending
	 * at the same angle from the midpoint (as in a conic gradient), the angle is
	 * offset by an amount proportional to the distance, creating a spiral pattern.
	 * 
	 * <p>
	 * Consider calling {@code .primeAnimation()} on the input {@code gradient}
	 * before rendering it as a spiral gradient to avoid creating a seam in the
	 * gradient where the first and last colors bump right up to one another.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    spiral gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @param curveCount  akin to zoom
	 */
	public void spiralGradient(Gradient gradient, PVector centerPoint, float angle, float curveCount) {
		spiralGradient(gradient, centerPoint, angle, curveCount, 1); // 1 becomes 0.5 (a.k.a sqrt (a special case))
	}

	/**
	 * Renders a spiral gradient with a specific "curviness".
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    spiral gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @param curveCount
	 * @param curviness   curviness exponent. affect how distance from center point
	 *                    affects curve. default = 1
	 */
	public void spiralGradient(Gradient gradient, PVector centerPoint, float angle, float curveCount, float curviness) {

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		angle %= TWO_PIf;

		final double curveDenominator = 1d / (renderWidth * renderWidth + renderHeight * renderHeight);
		curveCount *= TWO_PI;

		curviness = 1f / curviness;
		curviness *= 0.5; // curviness of 1 == exponenent of 0.5

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		makeThreadPool(renderPartitionsX, renderPartitionsY, SpiralThread.class, renderMidpointX, renderMidpointY, curveDenominator,
				curviness, angle, curveCount);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a polygonal gradient
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    polygon gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle       angle offset to render polygon at
	 * @param zoom
	 * @param sides       Number of polgyon sides. Should be at least 3 (a triangle)
	 */
	public void polygonGradient(Gradient gradient, PVector centerPoint, float angle, float zoom, int sides) {

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		/**
		 * Calculate minlength/maxlength of distances between edges of unit-length
		 * polygon and its midpoint (generally around 0.85). Or, distance of the centre
		 * of the polygon to the midpoint of each side (which are closer than vertices)
		 */
		final double MIN_LENGTH_RATIO = FastMath.tan(HALF_PI - (Math.PI / sides)); // used for hexagon gradient (== tan(60)) tan(SIDES)
		final double SEGMENT_ANGLE = (2 * Math.PI) / sides; // max angle of polygon segment in radians

		angle %= SEGMENT_ANGLE; // mod angle to minimise difference between theta and SEGMENT_ANGLE in loop

		final double denominator = MIN_LENGTH_RATIO / ((Math.max(renderHeight, renderWidth)) * (0.01 * zoom * FastPow.fastPow(sides, 2.4)));

		final float midpointXSquared = renderMidpointX * renderMidpointX;

		final int LUT_SIZE = (int) Functions.min(10000, renderWidth * 10, renderHeight * 10); // suitable value?
		final int HALF_LUT_SIZE = (int) (LUT_SIZE / TWO_PI);
		final float[] ratioLookup = new float[(int) (LUT_SIZE) + 1]; // LUT

		/*
		 * Pre-compute the ratio used to scale euclidean distance between each pixel and
		 * the gradient midpoint. I've explained this calculation here:
		 * https://stackoverflow.com/q/11812300/63264634#63264634
		 */
		for (int i = 0; i < ratioLookup.length; i++) {
			float theta = (float) (i * 2) / (LUT_SIZE); // *2 for
			theta *= Math.PI;
			theta -= angle;
			theta = (float) (Math.abs(theta) % SEGMENT_ANGLE);
			ratioLookup[i] = (float) ((MIN_LENGTH_RATIO * FastMath.cosQuick(theta) + FastMath.sinQuick(theta)) * denominator);
		}

		makeThreadPool(renderPartitionsX, renderPartitionsY, PolygonThread.class, renderMidpointX, renderMidpointY, midpointXSquared,
				ratioLookup, HALF_LUT_SIZE);

		gradientPG.updatePixels();

	}

	/**
	 * Leading to a 'X' shape.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    cross gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle       in radians, where 0 orients the 'X' ; going clockwise
	 * @param zoom
	 */
	public void crossGradient(Gradient gradient, PVector centerPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // for 'X' orientation when angle = 0
		angle = TWO_PIf - angle; // orient rotation clockwise

		final float sin = (float) FastMath.sin(angle);
		final float cos = (float) FastMath.cos(angle);

		makeThreadPool(renderPartitionsX, renderPartitionsY, CrossThread.class, renderMidpointX, renderMidpointY, denominator, sin, cos);

		gradientPG.updatePixels();
	}

	/**
	 * Renders a gradient where colors are plotted according to the manhattan
	 * distance between the position and midpoint, forming a diamond-shaped
	 * spectrum.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    diamond gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle
	 * @param zoom
	 */
	public void diamondGradient(Gradient gradient, PVector centerPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // angled at 0
		angle = TWO_PIf - angle; // orient rotation clockwise

		final float sin = (float) FastMath.sin(angle);
		final float cos = (float) FastMath.cos(angle);

		makeThreadPool(renderPartitionsX, renderPartitionsY, DiamondThread.class, renderMidpointX, renderMidpointY, denominator, sin, cos);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a noise gradient, using the FastNoise library to generate noise
	 * values.
	 * 
	 * <p>
	 * This method uses Simplex noise with no fractalisation. Other noise gradient
	 * methods allow parameters to customise the noise renderer (noise type and
	 * fractalisation). etc.).
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    noise gradient
	 * @param centerPoint For noise gradients, the centre point is used only when
	 *                    rotating (the point to rotate around).
	 * @param angle
	 * @param scale       noise frequency (effectively scale) default is 1
	 * @see #noiseGradient(Gradient, PVector, float, float, NoiseType)
	 * @see #fractalNoiseGradient(Gradient, PVector, float, float, NoiseType,
	 *      FractalType, int, float, float)
	 */
	public void noiseGradient(Gradient gradient, PVector centerPoint, float angle, float scale) {

		fastNoiseLite.SetNoiseType(NoiseType.OpenSimplex2);
		fastNoiseLite.SetFrequency(1 / scale * 0.001f); // normalise scale to a more appropriate value

		gradient.prime();

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float sin = (float) FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final float cos = (float) FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		final float[][] noiseVals = new float[gradientPG.width][gradientPG.height];

		makeThreadPool(renderPartitionsX, renderPartitionsY, NoiseThread.class, centerPoint, sin, cos, noiseVals);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a noise gradient in the given noise type.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    noise gradient
	 * @param centerPoint
	 * @param angle
	 * @param scale
	 * @param noiseType   The type of noise: Cellular (Voronoi), Simplex, Perlin,
	 *                    Value, etc.
	 */
	public void noiseGradient(Gradient gradient, PVector centerPoint, float angle, float scale, NoiseType noiseType) {
		fractalNoiseGradient(gradient, centerPoint, angle, scale, noiseType, FractalType.None, 0, 0, 0);
	}

	/**
	 * 
	 * @param gradient          1D {@link Gradient gradient} to use as the basis for
	 *                          the fractal noise gradient
	 * @param centerPoint
	 * @param angle
	 * @param scale
	 * @param noiseType         The type of noise: Cellular, Simplex, Perlin, Value,
	 *                          etc.
	 * @param fractalType       The type of fractal: FBm, Ridged, PingPong,
	 *                          DomainWarpProgressive, DomainWarpIndependent.
	 * @param fractalOctaves    More octaves lead to finer and finer detail. Should
	 *                          be at least 1. 4 is a suitable value.
	 * @param fractalGain       refers to a constant value being added to another
	 *                          one for each loop of an octave.
	 * @param fractalLacunarity refers to the 'texture' of a fractal; in simple
	 *                          terms think of it like two noises with different
	 *                          sizes being overlayed making for finer and finer
	 *                          detail. 0...5 is a suitable range of possible values
	 */
	public void fractalNoiseGradient(Gradient gradient, PVector centerPoint, float angle, float scale, NoiseType noiseType,
			FractalType fractalType, int fractalOctaves, float fractalGain, float fractalLacunarity) {

		fastNoiseLite.SetFrequency(1 / scale * 0.001f); // normalise scale to a more appropriate value
		fastNoiseLite.SetNoiseType(noiseType);

		fastNoiseLite.SetFractalType(fractalType);
		fastNoiseLite.SetFractalOctaves(fractalOctaves);
		fastNoiseLite.SetFractalGain(fractalGain);
		fastNoiseLite.SetFractalLacunarity(fractalLacunarity);

		gradient.prime();

		/**
		 * The range of noise values from fastnoise are generally [-1...1] (some have
		 * slightly less range). We use this min and max to map the noise [-1...1] to a
		 * step in the gradient [0...1].
		 */
		float min = -1, max = 1;

		// special case: the range of non-fractal celluar noise is [-1...0]
		if (fractalType == FractalType.None && noiseType == NoiseType.Cellular) {
			max = 0;
		}
		float maxMinDenom = 1 / (max - min); // determines how to scale the noise value to get in necessary range [0...1]

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float sin = (float) FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final float cos = (float) FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		final float[][] noiseVals = new float[gradientPG.width][gradientPG.height];

		makeThreadPool(renderPartitionsX, renderPartitionsY, FractalNoiseThread.class, centerPoint, sin, cos, min, maxMinDenom, noiseVals);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a spotlight-like gradient using a given origin point, angle and light
	 * angle.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    spotlight gradient
	 * @param originPoint the PVector point where the spotlight originates from
	 * @param angle       the angle of the spotlightangle = 0 means spotlight
	 *                    (facing down)
	 * @param beamAngle   Determines the angle (in radians) of the light beam.
	 *                    Smaller values mean a narrower beam, larger values mean a
	 *                    wider beam. Range is 0...PI (a.k.a. 0...180 in degrees). A
	 *                    default value would be PI/2 (90degrees).
	 */
	public void spotlightGradient(Gradient gradient, final PVector originPoint, float angle, float beamAngle) {

		// TODO horizontal falloff

		gradient.prime(); // prime curr color stop

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		beamAngle = Math.min(beamAngle, PIf);

		/**
		 * fudge angle slightly to prevent solid line drawn above the gradient when
		 * angle == 0 and divide by 2 to increase input range to a more suitable 0...180
		 * degrees (PI)
		 */
		beamAngle = (float) FastMath.tan((beamAngle + 0.0001) / 2);

		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		final float xDiffMax = (renderWidth / 2f) * beamAngle; // * beamAngle for limit

		makeThreadPool(renderPartitionsX, renderPartitionsY, SpotlightThread.class, originPoint, sin, cos, beamAngle, xDiffMax);

		gradientPG.updatePixels();

	}

	/**
	 * where beam angle scales depending on distance between points
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    spotlight gradient
	 * @param originPoint
	 * @param endPoint
	 */
	public void spotlightGradient(Gradient gradient, PVector originPoint, PVector endPoint) {
		final float angle = Functions.angleBetween(endPoint, originPoint) + PIf * 1.5f;
		spotlightGradient(gradient, originPoint, angle,
				PApplet.map(PVector.dist(originPoint, endPoint), 0, Math.max(renderWidth, renderHeight) * 1.25f, PIf / 8, PIf));
	}

	/**
	 * Renders what I've described as a an hourglass gradient, owing to it's
	 * similarity with an hourglass at certain angles.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    hourglass gradient
	 * @param centerPoint
	 * @param angle
	 * @param zoom        default = 1
	 * @see #hourglassGradient(Gradient, PVector, float, float, float, float)
	 */
	public void hourglassGradient(Gradient gradient, PVector centerPoint, float angle, float zoom) {
		hourglassGradient(gradient, centerPoint, angle, zoom, 0, 1);
	}

	/**
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle
	 * @param zoom
	 * @param pinch       determines how "pinched" the join/neck between the two
	 *                    bulbs is. default is 0
	 * @param roundness   how rounded (or circular) the bulbs are. default = 1
	 * @see #hourglassGradient(Gradient, PVector, float, float)
	 */
	public void hourglassGradient(Gradient gradient, PVector centerPoint, float angle, float zoom, float pinch, float roundness) {

		gradient.prime();

		pinch *= pinch; // square here (since value is used in sqrt() function)

		angle += HALF_PI; // hourglass shape at angle=0

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = 1 / ((Math.max(renderHeight, renderWidth)) * zoom);

		final float sin = (float) FastMath.sin(PApplet.TWO_PI - angle);
		final float cos = (float) FastMath.cos(angle);

		makeThreadPool(renderPartitionsX, renderPartitionsY, HourglassThread.class, renderMidpointX, renderMidpointY, sin, cos, zoom, angle,
				pinch, roundness, denominator);

		gradientPG.updatePixels();

	}

	/**
	 * Creates a pool of threads to split the rendering work for the given gradient
	 * type (each thread works on a portion of the pixels array). This method will
	 * start the threads, returning when threads have completed.
	 * 
	 * @param partitionsX
	 * @param partitionsY
	 * @param gradientType class for the given gradient type thread
	 * @param args         args to pass to gradient thread constructor
	 */
	private void makeThreadPool(final int partitionsX, final int partitionsY, final Class<? extends RenderThread> gradientType,
			final Object... args) {

		final int partitionWidth = (int) Math.floor((float) renderWidth / partitionsX); // pixel width of each parition
		final int partitionHeight = (int) Math.floor((float) renderHeight / partitionsY); // pixel height of each parition

		Object[] fullArgs = new Object[5 + args.length]; // empty obj array (to use as input args for new thread instance)
		fullArgs[0] = this; // sub-classes require parent instance as (hidden) first param
		System.arraycopy(args, 0, fullArgs, 5, args.length); // copy the input-args into fullargs

		List<Callable<Boolean>> taskList = new ArrayList<>();

		try {

			@SuppressWarnings("unchecked")
			Constructor<? extends RenderThread> constructor = (Constructor<? extends RenderThread>) gradientType
					.getDeclaredConstructors()[0]; // only 1 constructor per thread class

			/**
			 * Render all paritions (except for bottom-most row and right-most column)
			 */
			fullArgs[3] = partitionWidth; // set thread partition width
			fullArgs[4] = partitionHeight; // set thread partition height
			for (int a = 0; a < partitionsX - 1; a++) {
				for (int b = 0; b < partitionsY - 1; b++) {

					fullArgs[1] = a * partitionWidth; // set thread offsetX
					fullArgs[2] = b * partitionHeight; // set thread offsetY

					RenderThread thread = constructor.newInstance(fullArgs);
					taskList.add(thread);
				}
			}

			/*
			 * Render bottom row partitions
			 */
			fullArgs[2] = (partitionsY - 1) * partitionHeight; // bottom y row offset
			fullArgs[4] = renderHeight - (partitionHeight * (partitionsY - 1)); // set bottom row partition height (to account for when
			// height/partitionsY isn't whole number
			for (int a = 0; a < partitionsX - 1; a++) {

				fullArgs[1] = a * partitionWidth; // set thread offsetX

				RenderThread thread = constructor.newInstance(fullArgs);
				taskList.add(thread);
			}

			/**
			 * Render right-most column partitions
			 */
			fullArgs[1] = (partitionsX - 1) * partitionWidth; // right-most x column offset
			fullArgs[3] = renderWidth - (partitionWidth * (partitionsX - 1));
			fullArgs[4] = partitionHeight; // reset to original value (changed for bottom row render above)
			for (int b = 0; b < partitionsY - 1; b++) {

				fullArgs[2] = b * partitionHeight; // set thread offsetY

				RenderThread thread = constructor.newInstance(fullArgs);
				taskList.add(thread);
			}

			/**
			 * Render individual bottom-right parition
			 */
			fullArgs[1] = (partitionsX - 1) * partitionWidth; // set thread offsetX
			fullArgs[2] = (partitionsY - 1) * partitionHeight; // set thread offsetY
			fullArgs[4] = renderHeight - (partitionHeight * (partitionsY - 1));
			RenderThread thread = constructor.newInstance(fullArgs);
			taskList.add(thread);

			THREAD_POOL.invokeAll(taskList); // run threads now

		} catch (Exception e) { // if exception, probably because the given args don't match the thread class'
								// args
			e.printStackTrace();
		}

	}

	/**
	 * Threads operate on a portion of the pixels grid.
	 * 
	 * RenderThread child classes will implement call(); here the parallel gradient
	 * rendering work is done.
	 * 
	 * @author micycle1
	 *
	 */
	private abstract class RenderThread implements Callable<Boolean> {

		final int offsetX, offsetY; // so that each thread calculates and renders gradient spectrum into a unique
									// parition of the pixel grid
		final int pixelsX, pixelsY; // specifies how many pixels, whose indexes starts at the relevant offsets, for
									// this thread to calculate

		/**
		 * All gradient rendering threads, regardless of their specific gradient type,
		 * need offsets and pixel length to determine which partition of the pixels grid
		 * it should operate on.
		 */
		public RenderThread(int offsetX, int offsetY, int pixelsX, int pixelsY) {
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.pixelsX = pixelsX;
			this.pixelsY = pixelsY;
		}
	}

	private final class LinearThread extends RenderThread {

		private final float odX, odY;
		private final float odSqInverse;
		private float opXod;

		LinearThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float odX, float odY, float odSqInverse, float opXod) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.odX = odX;
			this.odY = odY;
			this.odSqInverse = odSqInverse;
			this.opXod = opXod;
		}

		@Override
		public Boolean call() {

			/**
			 * Usually, we'd call Functions.linearProject() to calculate step, but the
			 * function is inlined here to optimise speed.
			 */

			opXod += offsetY * odY * scaleY; // offset for thread
			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {

				opXod += odY * scaleY;
				float xOff = odX * scaleX * pixelsX * (offsetX / (float) pixelsX); // set partition x offset to correct amount

				for (x = offsetX; x < offsetX + pixelsX; x++) {
					float step = (opXod + xOff - offsetX) * odSqInverse; // get position of point on 1D gradient and normalise
					step = (step < 0) ? 0 : (step > 1 ? 1 : step); // clamp
					int stepInt = (int) (step * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
					xOff += odX * scaleX;
				}
			}

			return true;
		}

	}

	private final class RadialThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float zoom;

		RadialThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY, float zoom) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.zoom = zoom;
		}

		@Override
		public Boolean call() {

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				float rise = renderMidpointY - y;
				rise *= rise;

				for (x = offsetX; x < offsetX + pixelsX; x++) {
					float run = renderMidpointX - x;
					run *= run;

					float distSq = run + rise;
					float dist = zoom * distSq;

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					int stepInt = (int) (dist * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class ConicThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float angle;

		ConicThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY, float angle) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.angle = angle;
		}

		@Override
		public Boolean call() {

			double t;

			float rise, run;

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				rise = renderMidpointY - y;
				run = renderMidpointX - offsetX; // subtract offsetX so it renders correctly when paritioned over threads
				for (x = offsetX; x < offsetX + pixelsX; x++) {

					t = Functions.fastAtan2b(rise, run) + PConstants.PI - angle; // + PI to align bump with angle
					t *= INV_TWO_PI; // normalise
					t -= Math.floor(t); // modulo

					int stepInt = (int) (t * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];

					run--;
				}
			}

			return true;
		}

	}

	/**
	 * Thread for variable-curviness spiral gradients.
	 * 
	 * @author micycle1
	 *
	 */
	private final class SpiralThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float angle;
		private final float curveCount;
		private final float curviness;
		private final double curveDenominator;

		SpiralThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY,
				double curveDenominator, float curviness, float angle, float curveCount) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.curveCount = curveCount;
			this.curveDenominator = curveDenominator;
			this.angle = angle;
			this.curviness = curviness;
		}

		@Override
		public Boolean call() throws Exception {

			double t;
			double spiralOffset = 0;

			/**
			 * Choose sqrt version (default, faster) or version with custom curviness
			 * exponent. Choose here once, rather than each iteration in loop
			 */
			if (curviness == 0.5f) {
				for (int y = offsetY, x; y < offsetY + pixelsY; y++) {

					float rise = renderMidpointY - y;
					final float riseSquared = rise * rise;

					for (x = offsetX; x < offsetX + pixelsX; x++) {
						float run = renderMidpointX - x;
						t = Functions.fastAtan2b(rise, run) - angle; // -PI...PI
						spiralOffset = curveCount * Math.sqrt((riseSquared + run * run) * curveDenominator);
						t += spiralOffset;

						t *= INV_TWO_PI; // normalise
						t -= Math.floor(t); // modulo

						int stepInt = (int) (t * gradientCacheSize);
						gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];

						run--;
					}
				}
			} else {
				for (int y = offsetY, x; y < offsetY + pixelsY; y++) {

					float rise = renderMidpointY - y;
					final float riseSquared = rise * rise;

					for (x = offsetX; x < offsetX + pixelsX; x++) {
						float run = renderMidpointX - x;
						t = Functions.fastAtan2b(rise, run) - angle; // -PI...PI
						spiralOffset = curveCount * FastPow.fastPow((riseSquared + run * run) * curveDenominator, curviness);
						t += spiralOffset;

						t *= INV_TWO_PI; // normalise
						t -= Math.floor(t); // modulo

						int stepInt = (int) (t * gradientCacheSize);
						gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];

						run--;
					}
				}
			}

			return true;

		}
	}

	private final class PolygonThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float midpointXSquared;
		private final float[] ratioLookup;
		private final int HALF_LUT_SIZE;

		PolygonThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY,
				float midpointXSquared, float[] ratioLookup, int HALF_LUT_SIZE) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.midpointXSquared = midpointXSquared;
			this.ratioLookup = ratioLookup;
			this.HALF_LUT_SIZE = HALF_LUT_SIZE;
		}

		@Override
		public Boolean call() {

			double yDist; // y distance between midpoint and a given pixel
			double xDist; // x distance between midpoint and a given pixel

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {

				int inc = 1 + (2 * offsetX); // (N+x*i)^2, difference between successive Ns; account for thread offset
				yDist = (renderMidpointY - y) * (renderMidpointY - y);
				xDist = midpointXSquared + 1;
				xDist -= offsetX * 2 * renderMidpointX; // account for thread offset
				xDist += offsetX * (inc / 2); // account for thread offset

				for (x = offsetX; x < offsetX + pixelsX; x++) {
					xDist -= 2 * renderMidpointX;
					xDist += inc;
					float pointDistance = (float) Math.sqrt(yDist + xDist); // euclidean dist between (x,y) and midpoint
					inc += 2;

					final double theta = Functions.fastAtan2b((renderMidpointY - y), (renderMidpointX - x)); // range = -PI...PI

					// Use LUT: +PI to make theta in range 0...2PI and array index positive
					float polygonRatio = ratioLookup[(int) ((theta + Math.PI) * HALF_LUT_SIZE)]; // use LUT

					float dist = polygonRatio * pointDistance;

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class CrossThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float denominator;
		private final float sin, cos;

		CrossThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY, float denominator,
				float sin, float cos) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.denominator = denominator;
			this.sin = sin;
			this.cos = cos;
		}

		@Override
		public Boolean call() {

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				final float yTranslate = (y - renderMidpointY);
				for (x = offsetX; x < offsetX + pixelsX; x++) {
					final float newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					final float newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					float dist = Math.min(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // min

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class DiamondThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float denominator;
		private final float sin, cos;

		DiamondThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY, float denominator,
				float sin, float cos) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.denominator = denominator;
			this.sin = sin;
			this.cos = cos;
		}

		@Override
		public Boolean call() {

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				final float yTranslate = (y - renderMidpointY);
				for (x = offsetX; x < offsetX + pixelsX; x++) {
					final float newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					final float newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					float dist = Math.max(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // max

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class NoiseThread extends RenderThread {

		private final PVector centerPoint;
		private final float sin, cos;
		float[][] noiseVals; // reference to main method var

		NoiseThread(int offsetX, int offsetY, int pixelsX, int pixelsY, PVector centerPoint, float sin, float cos, float[][] noisevals) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.centerPoint = centerPoint;
			this.sin = sin;
			this.cos = cos;
			this.noiseVals = noisevals;
		}

		@Override
		public Boolean call() {

			/**
			 * Even the simpler noise types are quite expensive to compute for every pixel,
			 * so we calculate a noise value for every 4th pixel (every 2nd pixel on both
			 * axes), and then interpolate these values for other pixels later. Visually
			 * this isn't apparent since noise is a gradual function anyway.
			 */
			for (int y = offsetY, x; y < offsetY + pixelsY; y += 2) {
				final float yTranslate = (y - centerPoint.y);
				for (x = offsetX; x < offsetX + pixelsX; x += 2) {

					float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method
					noiseVals[x][y] = step;

					final int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			/**
			 * Interpolate horizontally (x-axis)
			 */
			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				for (x = offsetX + 1; x < offsetX + pixelsX - 1; x += 2) {
					noiseVals[x][y] = (noiseVals[x - 1][y] + noiseVals[x + 1][y]) / 2;
					final int stepInt = (int) (noiseVals[x][y] * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			/**
			 * Interpolate vertically (y-axis)
			 */
			for (int y = offsetY + 1, x; y < offsetY + pixelsY - 1; y += 2) {
				for (x = offsetX; x < offsetX + pixelsX; x += 1) {
					noiseVals[x][y] = (noiseVals[x][y - 1] + noiseVals[x][y + 1]) / 2;
					final int stepInt = (int) (noiseVals[x][y] * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			/**
			 * Pass over very bottom row (of each parition)
			 */
			float yTranslate = (offsetY + pixelsY - 1 - centerPoint.y);
			for (int x = offsetX; x < offsetX + pixelsX; x++) {
				float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
				float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

				float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method

				final int stepInt = (int) (step * gradientCacheSize);

				gradientPG.pixels[gradientPG.width * (offsetY + pixelsY - 1 + renderOffsetY)
						+ (x + renderOffsetX)] = gradientCache[stepInt];
			}

			/**
			 * Pass over right-most column (of each parition)
			 */

			for (int y = 0; y < renderHeight; y++) {
				yTranslate = (y - centerPoint.y);
				float newXpos = ((offsetX + pixelsX - 1) - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about
																													// midpoint
				float newYpos = yTranslate * cos + ((offsetX + pixelsX - 1) - centerPoint.x) * sin + centerPoint.y; // rotate y about
																													// midpoint

				float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method

				final int stepInt = (int) (step * gradientCacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY)
						+ (offsetX + pixelsX - 1 + renderOffsetX)] = gradientCache[stepInt];
			}

			return true;
		}

	}

	private final class FractalNoiseThread extends RenderThread {

		private final PVector centerPoint;
		private final float sin, cos;
		private final float min, maxMinDenom;
		float[][] noiseVals; // reference to main method var

		FractalNoiseThread(int offsetX, int offsetY, int pixelsX, int pixelsY, PVector centerPoint, float sin, float cos, float min,
				float maxMinDenom, float[][] noiseVals) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.centerPoint = centerPoint;
			this.sin = sin;
			this.cos = cos;
			this.min = min;
			this.maxMinDenom = maxMinDenom;
			this.noiseVals = noiseVals;
		}

		@Override
		public Boolean call() {

			/**
			 * Even the simpler noise types are quite expensive to compute for every pixel,
			 * so we calculate a noise value for every 4th pixel (every 2nd pixel on both
			 * axes), and then interpolate these values for other pixels later. Visually
			 * this isn't apparent since noise is a gradual function anyway.
			 */
			for (int y = offsetY, x; y < offsetY + pixelsY; y += 2) {
				final float yTranslate = (y - centerPoint.y);
				for (x = offsetX; x < offsetX + pixelsX; x += 2) {

					float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					float step = fastNoiseLite.GetNoise(newXpos, newYpos);
					step = ((step - min) * (maxMinDenom)); // scale to 0...1
					noiseVals[x][y] = step;

					final int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			/**
			 * Interpolate horizontally (x-axis)
			 */
			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				for (x = offsetX + 1; x < offsetX + pixelsX - 1; x += 2) {
					noiseVals[x][y] = (noiseVals[x - 1][y] + noiseVals[x + 1][y]) / 2;
					final int stepInt = (int) (noiseVals[x][y] * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			/**
			 * Interpolate vertically (y-axis)
			 */
			for (int y = offsetY + 1, x; y < offsetY + pixelsY - 1; y += 2) {
				for (x = offsetX; x < offsetX + pixelsX; x += 1) {
					noiseVals[x][y] = (noiseVals[x][y - 1] + noiseVals[x][y + 1]) / 2;
					final int stepInt = (int) (noiseVals[x][y] * gradientCacheSize);
					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			/**
			 * Pass over very bottom row (of each parition)
			 */
			float yTranslate = (offsetY + pixelsY - 1 - centerPoint.y);
			for (int x = offsetX; x < offsetX + pixelsX; x++) {
				float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
				float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

				float step = fastNoiseLite.GetNoise(newXpos, newYpos);

				step = ((step - min) * (maxMinDenom));
				final int stepInt = (int) (step * gradientCacheSize);

				gradientPG.pixels[gradientPG.width * (offsetY + pixelsY - 1 + renderOffsetY)
						+ (x + renderOffsetX)] = gradientCache[stepInt];
			}

			/**
			 * Pass over right-most column (of each parition)
			 */

			for (int y = 0; y < renderHeight; y++) {
				yTranslate = (y - centerPoint.y);
				float newXpos = ((offsetX + pixelsX - 1) - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about
																													// midpoint
				float newYpos = yTranslate * cos + ((offsetX + pixelsX - 1) - centerPoint.x) * sin + centerPoint.y; // rotate y about
																													// midpoint

				float step = fastNoiseLite.GetNoise(newXpos, newYpos);

				step = ((step - min) * (maxMinDenom));

				final int stepInt = (int) (step * gradientCacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY)
						+ (offsetX + pixelsX - 1 + renderOffsetX)] = gradientCache[stepInt];
			}

			return true;
		}

	}

	private final class SpotlightThread extends RenderThread {

		private final PVector originPoint;
		private final float beamAngle;
		private final float xDiffMax;
		private final float sin, cos;

		SpotlightThread(int offsetX, int offsetY, int pixelsX, int pixelsY, PVector originPoint, float sin, float cos, float beamAngle,
				float xDiffMax) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.originPoint = originPoint;
			this.sin = sin;
			this.cos = cos;
			this.beamAngle = beamAngle;
			this.xDiffMax = xDiffMax;
		}

		@Override
		public Boolean call() {

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				final float yTranslate = (y - originPoint.y);
				for (x = offsetX; x < offsetX + pixelsX; x++) {

					float newXpos = (x - originPoint.x) * cos - yTranslate * sin + originPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - originPoint.x) * sin + originPoint.y; // rotate y about midpoint

					/**
					 * Calculate the max X difference between this pixel and centrepoint.x when
					 * light fall off reaches the maximum (step = 1) for a given row (at an angle)
					 */
					float fallOffWidth = xDiffMax * ((newYpos - originPoint.y) / renderHeight * beamAngle);
					if (fallOffWidth < 0) { // may be negative if centrePoint.y out of screen
						fallOffWidth = 0;
					}

					float xDiff = Math.abs(newXpos - originPoint.x); // actual difference in x between this pixel and centerpoint.x

					float step = xDiff / fallOffWidth; // calculate step
					if (step > 1) { // clamp to a high of 1
						step = 1;
					}

					int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class HourglassThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float sin, cos;
		private final float pinch, roundness;
		private final float denominator;

		HourglassThread(int offsetX, int offsetY, int pixelsX, int pixelsY, float renderMidpointX, float renderMidpointY, float sin,
				float cos, float zoom, float angle, float pinch, float roundness, float denominator) {
			super(offsetX, offsetY, pixelsX, pixelsY);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.sin = sin;
			this.cos = cos;
			this.pinch = pinch;
			this.roundness = roundness;
			this.denominator = denominator;
		}

		@Override
		public Boolean call() {

			float newXpos;
			float newYpos;

			float yDist;
			float xDist;

			for (int y = offsetY, x; y < offsetY + pixelsY; y++) {
				yDist = (renderMidpointY - y) * (renderMidpointY - y);
				final float yTranslate = (y - renderMidpointY);

				for (x = offsetX; x < offsetX + pixelsX; x++) {

					xDist = (renderMidpointX - x) * (renderMidpointX - x);

					newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					/**
					 * In the 2 lines below, we are effectively calculating dist = eDist/(cos(angle)
					 * + sin(angle)), where eDist is euclidean distance between (x,y) & midpoint,
					 * and angle is the (atan2) angle between (x,y) & midpoint. These trig functions
					 * and multiple sqrts have been cancelled out to derive the faster equivalent
					 * equations below.
					 */

					float z = (renderMidpointY - newYpos) / (renderMidpointX - newXpos); // atan2(y,x) === atan(y/x), so calc y/x here

					double dist = Math.sqrt((yDist + xDist + pinch) * (z * z + roundness)) * denominator; // cos(atan(x)) === sqrt(z * z +
																											// 1)

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

}
