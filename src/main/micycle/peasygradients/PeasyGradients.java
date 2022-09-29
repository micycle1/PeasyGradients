package micycle.peasygradients;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import micycle.peasygradients.gradient.Gradient;
import micycle.peasygradients.utilities.FastNoiseLite;
import micycle.peasygradients.utilities.FastNoiseLite.CellularDistanceFunction;
import micycle.peasygradients.utilities.FastNoiseLite.CellularReturnType;
import micycle.peasygradients.utilities.FastNoiseLite.FractalType;
import micycle.peasygradients.utilities.FastNoiseLite.NoiseType;
import micycle.peasygradients.utilities.FastPow;
import micycle.peasygradients.utilities.Functions;
import micycle.uniformnoise.UniformNoise;
import net.jafama.FastMath;
import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

/**
 * Renders 1D {@link Gradient gradients} as 2D spectrums in your Processing
 * sketch.
 * <p>
 * The class offers both quick constructors for more simple gradients (such as 2
 * color horizontal gradients) and more powerful constructors for more involved
 * gradients (centre-offset, angled, n-color gradient with color stops).
 * <p>
 * By default, a PeasyGradients instance draws directly into the Processing
 * sketch; you can give it a specific <code>PGraphics</code> pane to draw into
 * with the <code>.setRenderTarget()</code> method).
 * <p>
 * Algorithms for linear, radial & conic gradients are based on <a href=
 * "https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2">this</a>
 * work by Jeremy Behreandt; all others are mostly my own derivation.
 * 
 * @author Michael Carleton
 */
public final class PeasyGradients {

	// TODO set color interpolation method shape, TODO interpolation mode in this
	// class?!
	// https://helpx.adobe.com/illustrator/using/gradients.html#create_apply_freeform_gradient
	// for examples

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
	private final UniformNoise uniformNoise = new UniformNoise(0);

//	int colorMode = PConstants.RGB; // TODO colour mode in this class?
	private PImage gradientPG; // reference to the PGraphics object to render gradients into

	private int[] gradientCache; // a cache to store gradient colors (ARGB ints)
	private int gradientCacheSize; // size of cache

	private int renderHeight, renderWidth; // gradient region dimensions (usually the dimensions of gradientPG)
	private int renderOffsetX, renderOffsetY; // gradient region offsets (usually 0, 0)
	private float scaleY, scaleX;

	/**
	 * Number of horizontal strips the plane is paritioned into for threaded
	 * rendering
	 */
	private int renderStrips = (int) Math.max(cpuThreads * 0.75, 1);

	/**
	 * Constructs a new PeasyGradients renderer from a running Processing sketch;
	 * gradients will be rendered directly into the sketch.
	 * 
	 * @param p the Processing sketch. You'll usually refer to it as {@code this}
	 */
	public PeasyGradients(PApplet p) {
		this(p.getGraphics());
	}

	/**
	 * Constructs a new PeasyGradients renderer targeting a specfic
	 * <code>PImage</code>.
	 * 
	 * @param g
	 */
	public PeasyGradients(PImage g) {
		this();
		gradientPG = g;
		setRenderTarget(g);
	}

	/**
	 * Constructs a new PeasyGradients renderer with no rendering target. Note
	 * {@link #setRenderTarget(PImage)} must be called at least once later (before a
	 * gradient is drawn).
	 */
	public PeasyGradients() {
		fastNoiseLite.SetCellularReturnType(CellularReturnType.Distance2Div);
		fastNoiseLite.SetCellularDistanceFunction(CellularDistanceFunction.EuclideanSq);
		fastNoiseLite.SetFractalPingPongStrength(1);
	}

	/**
	 * Tells this PeasyGradients renderer to render 2D gradients into the Processing
	 * sketch (spanning the full size of the sketch).
	 * 
	 * @param p
	 */
	public void setRenderTarget(PApplet p) {
		setRenderTarget(p.getGraphics(), 0, 0, p.width, p.height);
	}

	/**
	 * Tells this PeasyGradients renderer to render 2D gradients into the Processing
	 * sketch, within a certain region specified by input arguments.
	 * 
	 * @param p
	 * @param offSetX x-axis offset of the region to render gradients into (where 0
	 *                is top-left corner)
	 * @param offSetY y-axis offset of the region to render gradients into (where 0
	 *                is top-left corner)
	 * @param width   width of region to render gradients into
	 * @param height  height of region to render gradients into
	 */
	public void setRenderTarget(PApplet p, int offSetX, int offSetY, int width, int height) {
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

	/**
	 * Sets the graphics object and the rectangular region into which this
	 * PeasyGradients object will renderer gradients.
	 * 
	 * @param g
	 * @param offSetX x-axis offset of the region to render gradients into (where 0
	 *                is top-left corner)
	 * @param offSetY y-axis offset of the region to render gradients into (where 0
	 *                is top-left corner)
	 * @param width   width of region to render gradients into
	 * @param height  height of region to render gradients into
	 */
	public void setRenderTarget(PImage g, int offSetX, int offSetY, int width, int height) {
		if (offSetX < 0 || offSetY < 0 || (width + offSetX) > g.width || (offSetY + height) > g.height) {
			System.err.println("Invalid parameters.");
			return;
		}

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

		gradientCacheSize = (3 * Math.max(actualWidth, actualHeight));
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
	 * Renders a linear gradient (having its midpoint at the centre of the
	 * sketch/render target).
	 * <p>
	 * It's called 'linear' because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient 1D {@link Gradient gradient} to use as the basis for the
	 *                 linear gradient
	 * @param angle    gradient angle in radians (color is drawn parallel to this
	 *                 angle), where a value of <code>0</code> corresponds to a
	 *                 horizontal gradient spanning left-to-right. larger (positive)
	 *                 values correspond to a clockwise direction
	 */
	public void linearGradient(Gradient gradient, float angle) {

		PVector centerPoint = new PVector(gradientPG.width / 2f, gradientPG.height / 2f);
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

		makeThreadPool(renderStrips, LinearThread.class, odX, odY, odSqInverse, opXod);

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

		final int hypotSq = (renderWidth * renderWidth) + (renderHeight * renderHeight);

		zoom = 4 / zoom; // calc here, not in loop
		zoom /= hypotSq; // calc here, not in loop

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		makeThreadPool(renderStrips, RadialThread.class, renderMidpointX, renderMidpointY, zoom);

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
		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		makeThreadPool(renderStrips, ConicThread.class, renderMidpointX, renderMidpointY, angle);

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

		makeThreadPool(renderStrips, SpiralThread.class, renderMidpointX, renderMidpointY, curveDenominator, curviness, angle, curveCount);

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
		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		/*
		 * Calculate minlength/maxlength of distances between edges of unit-length
		 * polygon and its midpoint (generally around 0.85). Or, distance of the centre
		 * of the polygon to the midpoint of each side (which are closer than vertices)
		 */
		final double MIN_LENGTH_RATIO = FastMath.tan(HALF_PI - (Math.PI / sides)); // used for hexagon gradient (== tan(60)) tan(SIDES)
		final double SEGMENT_ANGLE = (2 * Math.PI) / sides; // max angle of polygon segment in radians

		angle %= SEGMENT_ANGLE; // mod angle to minimise difference between theta and SEGMENT_ANGLE in loop

		final double denominator = MIN_LENGTH_RATIO / ((Math.max(renderHeight, renderWidth)) * (0.01 * zoom * FastPow.fastPow(sides, 2.4)));

		final int LUT_SIZE = (int) Functions.min(2000, renderWidth * 10f, renderHeight * 10f); // suitable value?
		final int HALF_LUT_SIZE = (int) (LUT_SIZE / TWO_PI);
		final float[] ratioLookup = new float[(LUT_SIZE) + 1]; // LUT

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

		makeThreadPool(renderStrips, PolygonThread.class, renderMidpointX, renderMidpointY, ratioLookup, HALF_LUT_SIZE);

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

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = (Math.max(renderHeight, renderWidth) / 2f) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // for 'X' orientation when angle = 0
		angle = TWO_PIf - angle; // orient rotation clockwise

		final float sin = (float) FastMath.sin(angle);
		final float cos = (float) FastMath.cos(angle);

		makeThreadPool(renderStrips, CrossThread.class, renderMidpointX, renderMidpointY, denominator, sin, cos);

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

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = (Math.max(renderHeight, renderWidth) / 2f) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // angled at 0
		angle = TWO_PIf - angle; // orient rotation clockwise

		final float sin = (float) FastMath.sin(angle);
		final float cos = (float) FastMath.cos(angle);

		makeThreadPool(renderStrips, DiamondThread.class, renderMidpointX, renderMidpointY, denominator, sin, cos);

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

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float sin = (float) FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final float cos = (float) FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		makeThreadPool(renderStrips, NoiseThread.class, centerPoint, sin, cos);

		gradientPG.updatePixels();

	}

	/**
	 * Renders a noise gradient in the given noise type.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    noise gradient
	 * @param centerPoint For noise gradients, the centre point is used only when
	 *                    rotating (the point to rotate around).
	 * @param angle
	 * @param scale
	 * @param noiseType   The type of noise: Cellular (Voronoi), Simplex, Perlin,
	 *                    Value, etc.
	 */
	public void noiseGradient(Gradient gradient, PVector centerPoint, float angle, float scale, NoiseType noiseType) {
		fractalNoiseGradient(gradient, centerPoint, angle, scale, noiseType, FractalType.None, 0, 0, 0);
	}

	/**
	 * Renders a noise gradient having a uniform distribution, and using a given
	 * noise z value.
	 * 
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    noise gradient
	 * @param centerPoint For noise gradients, the centre point is used only when
	 *                    rotating (the point to rotate around).
	 * @param z           the z value of the noise (mutate this to animate the
	 *                    noise)
	 * @param angle
	 * @param scale       frequency/scale of noise. larger values are more "zoomed
	 *                    in"
	 */
	public void uniformNoiseGradient(Gradient gradient, PVector centerPoint, float z, float angle, float scale) {
		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float sin = (float) FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final float cos = (float) FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		makeThreadPool(renderStrips, UniformNoiseThread.class, centerPoint, sin, cos, scale, z);

		gradientPG.updatePixels();
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

		makeThreadPool(renderStrips, FractalNoiseThread.class, centerPoint, sin, cos, min, maxMinDenom);

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

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		beamAngle = Math.min(beamAngle, PIf);

		/*
		 * fudge angle slightly to prevent solid line drawn above the gradient when
		 * angle == 0 and divide by 2 to increase input range to a more suitable 0...180
		 * degrees (PI)
		 */
		beamAngle = (float) FastMath.tan((beamAngle + 0.0001) / 2);

		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		final float xDiffMax = (renderWidth / 2f) * beamAngle; // * beamAngle for limit

		makeThreadPool(renderStrips, SpotlightThread.class, originPoint, sin, cos, beamAngle, xDiffMax);

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
		pinch *= pinch; // square here (since value is used in sqrt() function)

		angle += HALF_PI; // hourglass shape at angle=0

		for (int i = 0; i < gradientCache.length; i++) { // calc LUT
			gradientCache[i] = gradient.getColor(i / (float) gradientCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = 1 / ((Math.max(renderHeight, renderWidth)) * zoom);

		final float sin = (float) FastMath.sin(PConstants.TWO_PI - angle);
		final float cos = (float) FastMath.cos(angle);

		makeThreadPool(renderStrips, HourglassThread.class, renderMidpointX, renderMidpointY, sin, cos, zoom, angle, pinch, roundness,
				denominator);

		gradientPG.updatePixels();

	}

	/**
	 * Creates a pool of threads to split the rendering work for the given gradient
	 * type (each thread works on a portion of the pixels array). This method starts
	 * the threads and returns when all threads have completed.
	 * 
	 * @param partitionsY
	 * @param gradientType class for the given gradient type thread
	 * @param args         args to pass to gradient thread constructor
	 */
	private void makeThreadPool(final int partitionsY, final Class<?> gradientType, final Object... args) {

		Object[] fullArgs = new Object[3 + args.length]; // empty obj array (to use as input args for new thread instance)
		fullArgs[0] = this; // sub-classes require parent instance as (hidden) first param
		System.arraycopy(args, 0, fullArgs, 3, args.length); // copy the input-args into fullargs

		List<Callable<Boolean>> taskList = new ArrayList<>();

		try {
			@SuppressWarnings("unchecked")
			Constructor<? extends RenderThread> constructor = (Constructor<? extends RenderThread>) gradientType
					.getDeclaredConstructors()[0]; // only 1 constructor per thread class

			int rows = renderHeight / partitionsY; // rows per strip (except for last strip, which may have less/more, due to floor
													// division)
			for (int strip = 0; strip < partitionsY - 1; strip++) {
				fullArgs[1] = rows * strip;
				fullArgs[2] = rows;
				RenderThread thread = constructor.newInstance(fullArgs);
				taskList.add(thread);
			}

			fullArgs[1] = rows * (partitionsY - 1);
			fullArgs[2] = renderHeight - rows * (partitionsY - 1);
			RenderThread thread = constructor.newInstance(fullArgs);
			taskList.add(thread);

			THREAD_POOL.invokeAll(taskList); // run threads; wait for completion

		} catch (Exception e) { // the given args probably don't match the thread class args
			e.printStackTrace();
		}

	}

	/**
	 * Threads operate on a portion of the pixels grid.
	 * 
	 * RenderThread child classes will implement call(); here the parallel gradient
	 * rendering work is done.
	 * 
	 * @author Michael Carleton
	 *
	 */
	private abstract class RenderThread implements Callable<Boolean> {

		final int rowOffset, rows; // so that each thread calculates and renders gradient spectrum into a unique
									// parition of the pixel grid
		int pixel;

		/*
		 * All gradient rendering threads, regardless of their specific gradient type,
		 * need offsets and pixel length to determine which partition of the pixels grid
		 * it should operate on.
		 */
		RenderThread(int rowOffset, int rows) {
			this.rowOffset = rowOffset;
			this.rows = rows;
			pixel = rowOffset * renderWidth;
		}
	}

	private final class LinearThread extends RenderThread {

		private final float odX, odY;
		private final float odSqInverse;
		private float opXod;

		LinearThread(int rowOffset, int rows, float odX, float odY, float odSqInverse, float opXod) {
			super(rowOffset, rows);
			this.odX = odX;
			this.odY = odY;
			this.odSqInverse = odSqInverse;
			this.opXod = opXod;
		}

		@Override
		public Boolean call() {
			/*
			 * Usually we'd call Functions.linearProject() to calculate step at each pixel,
			 * but the function is inlined here to optimise speed.
			 */
			opXod += rowOffset * odY * scaleY; // offset for thread
			for (int y = rowOffset; y < rowOffset + rows; y++) {
				opXod += odY * scaleY;
				float xOff = 0; // set partition x offset to correct amount
				for (int x = 0; x < renderWidth; x++) {
					float step = (opXod + xOff - rowOffset) * odSqInverse; // get position of point on 1D gradient and normalise
					step = (step < 0) ? 0 : (step > 1 ? 1 : step); // clamp between 0...1
					int stepInt = (int) (step * gradientCacheSize);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
					xOff += odX * scaleX;
				}
			}

			return true;
		}

	}

	private final class RadialThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float zoom;

		RadialThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, float zoom) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.zoom = zoom;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				float rise = renderMidpointY - y;
				rise *= rise;
				for (int x = 0; x < renderWidth; x++) {

					float run = renderMidpointX - x;
					run *= run;

					float distSq = run + rise;
					float dist = zoom * distSq;

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					int stepInt = (int) (dist * gradientCacheSize);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class ConicThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float angle;

		ConicThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, float angle) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.angle = angle;
		}

		@Override
		public Boolean call() {

			double t;
			float rise, run;

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				rise = renderMidpointY - y;
				for (int x = 0; x < gradientPG.width; x++) { // FULL WIDTH
					run = renderMidpointX - x;
					t = Functions.fastAtan2b(rise, run) + PConstants.PI - angle; // + PI to align bump with angle
					t *= INV_TWO_PI; // normalise
					t -= Math.floor(t); // modulo

					int stepInt = (int) (t * gradientCacheSize);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	/**
	 * Thread for variable-curviness spiral gradients.
	 * 
	 * @author Michael Carleton
	 *
	 */
	private final class SpiralThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float angle;
		private final float curveCount;
		private final float curviness;
		private final double curveDenominator;

		SpiralThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, double curveDenominator, float curviness,
				float angle, float curveCount) {
			super(rowOffset, rows);
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

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				float rise = renderMidpointY - y;
				final float riseSquared = rise * rise;
				for (int x = 0; x < gradientPG.width; x++) { // FULL WIDTH

					float run = renderMidpointX - x;
					t = Functions.fastAtan2b(rise, run) - angle; // -PI...PI
					spiralOffset = curveCount * curviness == 0.5f ? Math.sqrt((riseSquared + run * run) * curveDenominator)
							: FastPow.fastPow((riseSquared + run * run) * curveDenominator, curviness);
					t += spiralOffset;

					t *= INV_TWO_PI; // normalise
					t -= Math.floor(t); // modulo

					int stepInt = (int) (t * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}
	}

	private final class PolygonThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float[] ratioLookup;
		private final int HALF_LUT_SIZE;

		PolygonThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, float[] ratioLookup, int HALF_LUT_SIZE) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.ratioLookup = ratioLookup;
			this.HALF_LUT_SIZE = HALF_LUT_SIZE;
		}

		@Override
		public Boolean call() {

			double yDist; // y distance between midpoint and a given pixel
			double xDist; // x distance between midpoint and a given pixel

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				yDist = (renderMidpointY - y);
				xDist = renderMidpointX;
				for (int x = 0; x < renderWidth; x++) {
					final float pointDistance = (float) Math.sqrt(yDist * yDist + xDist * xDist); // euclidean dist between (x,y) and
																									// midpoint
					xDist--;

					final double theta = Functions.fastAtan2b((renderMidpointY - y), (renderMidpointX - x)); // range = -PI...PI

					// Use LUT: +PI to make theta in range 0...2PI and array index positive
					float polygonRatio = ratioLookup[(int) ((theta + Math.PI) * HALF_LUT_SIZE)]; // use LUT

					float dist = polygonRatio * pointDistance;

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class CrossThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float denominator;
		private final float sin, cos;

		CrossThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, float denominator, float sin, float cos) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.denominator = denominator;
			this.sin = sin;
			this.cos = cos;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				final float yTranslate = (y - renderMidpointY);
				for (int x = 0; x < renderWidth; x++) {
					final float newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					final float newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					float dist = Math.min(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // min

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class DiamondThread extends RenderThread {

		private final float renderMidpointX, renderMidpointY;
		private final float denominator;
		private final float sin, cos;

		DiamondThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, float denominator, float sin, float cos) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.denominator = denominator;
			this.sin = sin;
			this.cos = cos;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				final float yTranslate = (y - renderMidpointY);
				for (int x = 0; x < renderWidth; x++) {
					final float newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					final float newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					float dist = Math.max(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // max

					if (dist > 1) { // clamp to a high of 1
						dist = 1;
					}

					final int stepInt = (int) (dist * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private class NoiseThread extends RenderThread {

		final PVector centerPoint;
		final float sin, cos;

		NoiseThread(int rowOffset, int rows, PVector centerPoint, float sin, float cos) {
			super(rowOffset, rows);
			this.centerPoint = centerPoint;
			this.sin = sin;
			this.cos = cos;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				final float yTranslate = (y - centerPoint.y);
				for (int x = 0; x < gradientPG.width; x++) {
					float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method

					final int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class UniformNoiseThread extends NoiseThread {

		private final float z;
		private final float scale;

		UniformNoiseThread(int rowOffset, int rows, PVector centerPoint, float sin, float cos, float scale, float z) {
			super(rowOffset, rows, centerPoint, sin, cos);
			this.scale = 1 / (200 * scale);
			this.z = z;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				final float yTranslate = (y - centerPoint.y);
				for (int x = 0; x < gradientPG.width; x++) {
					float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					float step = uniformNoise.uniformNoise(scale * newXpos, newYpos * scale, z);

					final int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class FractalNoiseThread extends RenderThread {

		private final PVector centerPoint;
		private final float sin, cos;
		private final float min, maxMinDenom;

		FractalNoiseThread(int rowOffset, int rows, PVector centerPoint, float sin, float cos, float min, float maxMinDenom) {
			super(rowOffset, rows);
			this.centerPoint = centerPoint;
			this.sin = sin;
			this.cos = cos;
			this.min = min;
			this.maxMinDenom = maxMinDenom;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				final float yTranslate = (y - centerPoint.y);
				for (int x = 0; x < gradientPG.width; x++) {
					float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					float step = fastNoiseLite.GetNoise(newXpos, newYpos);
					step = ((step - min) * (maxMinDenom)); // scale to 0...1

					final int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

	private final class SpotlightThread extends RenderThread {

		private final PVector originPoint;
		private final float beamAngle;
		private final float xDiffMax;
		private final float sin, cos;

		SpotlightThread(int rowOffset, int rows, PVector originPoint, float sin, float cos, float beamAngle, float xDiffMax) {
			super(rowOffset, rows);
			this.originPoint = originPoint;
			this.sin = sin;
			this.cos = cos;
			this.beamAngle = beamAngle;
			this.xDiffMax = xDiffMax;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				final float yTranslate = (y - originPoint.y);
				for (int x = 0; x < renderWidth; x++) {
					float newXpos = (x - originPoint.x) * cos - yTranslate * sin + originPoint.x; // rotate x about midpoint
					float newYpos = yTranslate * cos + (x - originPoint.x) * sin + originPoint.y; // rotate y about midpoint

					/*
					 * Calculate the max X difference between this pixel and centrepoint.x when
					 * light fall off reaches the maximum (step = 1) for a given row (at an angle)
					 */
					float fallOffWidth = xDiffMax * ((newYpos - originPoint.y) / renderHeight * beamAngle);
					if (fallOffWidth < 0) { // may be negative if centrePoint.y out of screen
						fallOffWidth = Float.MIN_VALUE; // avoid divide by zero error
					}

					float xDiff = Math.abs(newXpos - originPoint.x); // actual difference in x between this pixel and centerpoint.x

					float step = xDiff / fallOffWidth; // calculate step
					if (step > 1) { // clamp to a high of 1
						step = 1;
					}

					int stepInt = (int) (step * gradientCacheSize);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
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

		HourglassThread(int rowOffset, int rows, float renderMidpointX, float renderMidpointY, float sin, float cos, float zoom,
				float angle, float pinch, float roundness, float denominator) {
			super(rowOffset, rows);
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

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				yDist = (renderMidpointY - y) * (renderMidpointY - y);
				final float yTranslate = (y - renderMidpointY);
				for (int x = 0; x < renderWidth; x++) {
					xDist = (renderMidpointX - x) * (renderMidpointX - x);

					newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					/*
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

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
			}

			return true;
		}

	}

}
