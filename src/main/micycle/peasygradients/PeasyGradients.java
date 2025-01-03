package micycle.peasygradients;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
 * Samples 1D {@link Gradient} specifications to generate 2D raster gradient
 * visualizations in Processing sketches.
 *
 * <p>
 * The renderer provides multiple gradient sampling patterns. Here's a few
 * examples:
 * <ul>
 * <li>Linear: Colors sampled along parallel lines
 * <li>Radial: Colors sampled along concentric circles from a center point
 * <li>Conic: Colors sampled along angular sweeps around a center point
 * </ul>
 *
 * <p>
 * Supports two modes of construction:
 * <ul>
 * <li>Simple constructors for basic cases (e.g., two-color horizontal
 * gradients)
 * <li>Advanced constructors for complex configurations (e.g., angled gradients
 * with multiple color stops and custom center offsets)
 * </ul>
 *
 * <p>
 * By default, renders directly to the Processing sketch. Use
 * {@code .setRenderTarget()} to specify a custom {@code PGraphics} output
 * buffer.
 * </p>
 *
 * <p>
 * Linear, radial &amp; conic sampling algorithms adapted from Jeremy Behreandt.
 * Additional sampling patterns are original implementations.
 * </p>
 * 
 * @author Michael Carleton
 */
public final class PeasyGradients {

	// TODO set color interpolation method shape, TODO interpolation mode in this
	// class?!
	// https://helpx.adobe.com/illustrator/using/gradients.html#create_apply_freeform_gradient
	// for examples

	private static final double TWO_PI = (2 * Math.PI);
	private static final double PI = Math.PI;
	private static final double HALF_PI = (0.5 * Math.PI);
	private static final double INV_TWO_PI = 1 / TWO_PI;
	private static final double THREE_QRTR_PI = (0.75 * Math.PI);
	private static final double DEFAULT_DITHER = 0.01;

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
	private double scaleY, scaleX;

	private double ditherStrength = DEFAULT_DITHER;

	/**
	 * Number of horizontal strips the plane is paritioned into for threaded
	 * rendering.
	 */
	private int renderStrips = (int) Math.min(Math.max(cpuThreads * 0.75, 1), 10); // 1..10 strips
	
	void setRenderStrips(int renderStrips) {
		this.renderStrips = renderStrips;
	}

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
	 * @see #setRenderTarget(PApplet)
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
		renderOffsetX = Math.max(0, offSetX); // Offset X cannot be less than 0
		renderOffsetX = Math.min(renderOffsetX, g.width - 1); // Offset X cannot be beyond (image width - 1)
		renderOffsetY = Math.max(0, offSetY); // Offset Y cannot be less than 0
		renderOffsetY = Math.min(renderOffsetY, g.height - 1); // Offset Y cannot be beyond (image height - 1)

		// 2. Constrain Width and Height based on Constrained Offsets:
		renderWidth = Math.max(0, width); // Width cannot be negative
		renderWidth = Math.min(renderWidth, g.width - renderOffsetX); // Width cannot extend beyond the image's right edge
		renderHeight = Math.max(0, height); // Height cannot be negative
		renderHeight = Math.min(renderHeight, g.height - renderOffsetY); // Height cannot extend beyond the image's bottom edge

		scaleX = g.width / (double) width; // used for correct rendering increment for some gradients
		scaleY = g.height / (double) height; // used for correct rendering increment for some gradients

		if (!g.isLoaded()) { // load pixel array if not already done
			if (g instanceof PGraphics) {
				((PGraphics) g).beginDraw();
			}
			g.loadPixels(); // only needs to be called once
//			g.endDraw(); // commented out -- if called on PApplet during draw loop, will prevent being drawn to until next draw
		}

		gradientPG = g;

		gradientCacheSize = (3 * Math.max(renderWidth, renderHeight));
		gradientCache = new int[gradientCacheSize];
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
	 * Sets the dithering strength.
	 * <p>
	 * Dithering helps to reduce the visibility of color-banding within gradients.
	 * It breaks up the noticeable banding-edges and replace them with less
	 * noticeable noise. The strength parameter defines the maximum amount by which
	 * a color at read at location t may deviate from its position along the 1D
	 * gradient spectrum.
	 * 
	 * @param strength A value >=0. Default = 0.015 (akin to a max deviation of 1.5%
	 *                 from the colorstop)
	 */

	/**
	 * Adjusts the dithering strength to reduce color banding in gradients.
	 * <p>
	 * Dithering disperses the visible color-banding by replacing sharp banding
	 * edges with subtle noise. The 'strength' parameter specifies the maximum
	 * permissible deviation for a color at a given position from its expected
	 * position in the underlying 1D gradient spectrum, expressed as a fraction.
	 *
	 * @param strength A non-negative value specifying the deviation fraction.
	 *                 Default is 0.015 (1.5% deviation).
	 */
	public void setDitherStrength(double strength) {
		ditherStrength = strength;
	}

	/**
	 * Restricts any and all rendered gradients to render in at most n colors
	 * (a.k.a. posterisation).
	 * <p>
	 * Note that applying posterisation will disable dithering.
	 * 
	 * @param n max number of colors
	 * @see #clearPosterisation()
	 */
	public void posterise(int n) {
		n = Math.max(n, 0);
		if (n != gradientCacheSize) {
			gradientCacheSize = n;
			gradientCache = new int[gradientCacheSize];
			ditherStrength = 0;
		}
	}

	/**
	 * Clears any user-defined color posterisation setting.
	 * <p>
	 * Note clearing posterisation restores dithering back to its default strength.
	 * 
	 * @see #posterise(int)
	 */
	public void clearPosterisation() {
		if (ditherStrength == 0) {
			ditherStrength = DEFAULT_DITHER;
		}
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
	public void linearGradient(Gradient gradient, double angle) {

		PVector centerPoint = new PVector(gradientPG.width / 2f, gradientPG.height / 2f);
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);

		double modAngle = angle % TWO_PI;

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
	public void linearGradient(Gradient gradient, PVector centerPoint, double angle) {

		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);

		double modAngle = angle % TWO_PI;

		if (modAngle > HALF_PI && modAngle <= HALF_PI * 3) {
			linearGradient(gradient, o[0], o[1]);
		} else {
			linearGradient(gradient, o[1], o[0]);
		}
	}

	/**
	 * Renders a linear gradient using a given gradient centerpoint, angle and
	 * length.
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
	 *                    will be exactly on edge); When &lt;1, colors will be
	 *                    squashed; when &gt;1, colors spread out (outermost colors
	 *                    will leave the view).
	 */
	public void linearGradient(Gradient gradient, PVector centerPoint, double angle, double length) {
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		o[0].lerp(centerPoint, (float) (1 - length)); // mutate length
		o[1].lerp(centerPoint, (float) (1 - length)); // mutate length

		double modAngle = angle % TWO_PI;

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
		/**
		 * Pre-compute vals for linearprojection
		 */
		double odX = controlPoint2.x - controlPoint1.x; // Rise and run of line.
		double odY = controlPoint2.y - controlPoint1.y; // Rise and run of line.
		final double odSqInverse = 1 / (odX * odX + odY * odY); // Distance-squared of line.
		double opXod = -controlPoint1.x * odX + -controlPoint1.y * odY;
		makeThreadPool(gradient, renderStrips, LinearThread.class, odX, odY, odSqInverse, opXod);

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
	public void radialGradient(Gradient gradient, PVector centerPoint, double zoom) {

		final int hypotSq = (renderWidth * renderWidth) + (renderHeight * renderHeight);

		zoom = 4 / zoom; // calc here, not in loop
		zoom /= hypotSq; // calc here, not in loop

		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		makeThreadPool(gradient, renderStrips, RadialThread.class, renderMidpointX, renderMidpointY, zoom);

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
	 * @param gradient    1D {@link Gradient gradient} to use as the basis for the
	 *                    conic gradient
	 * @param centerPoint The PVector center point of the gradient — the position
	 *                    where it radiates from.
	 * @param angle       in radians, where 0 is east; going clockwise
	 */
	public void conicGradient(Gradient gradient, PVector centerPoint, double angle) {
		// TODO add zoom arg
		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		makeThreadPool(gradient, renderStrips, ConicThread.class, renderMidpointX, renderMidpointY, angle);

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
	public void spiralGradient(Gradient gradient, PVector centerPoint, double angle, double curveCount) {
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
	public void spiralGradient(Gradient gradient, PVector centerPoint, double angle, double curveCount, double curviness) {
		angle %= TWO_PI;

		final double curveDenominator = 1d / (renderWidth * renderWidth + renderHeight * renderHeight);
		curveCount *= TWO_PI;

		curviness = 1f / curviness;
		curviness *= 0.5; // curviness of 1 == exponent of 0.5

		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		makeThreadPool(gradient, renderStrips, SpiralThread.class, renderMidpointX, renderMidpointY, curveDenominator, curviness, angle, curveCount);

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
	public void polygonGradient(Gradient gradient, PVector centerPoint, double angle, double zoom, int sides) {
		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		/*
		 * Calculate minlength/maxlength of distances between edges of unit-length
		 * polygon and its midpoint (generally around 0.85). Or, distance of the centre
		 * of the polygon to the midpoint of each side (which are closer than vertices)
		 */
		final double MIN_LENGTH_RATIO = FastMath.tan(HALF_PI - (Math.PI / sides)); // used for hexagon gradient (== tan(60)) tan(SIDES)
		final double SEGMENT_ANGLE = TWO_PI / sides; // max angle of polygon segment in radians

		angle %= SEGMENT_ANGLE; // mod angle to minimise difference between theta and SEGMENT_ANGLE in loop

		final double denominator = MIN_LENGTH_RATIO / ((Math.max(renderHeight, renderWidth)) * (0.0125 * zoom * FastMath.pow(sides, 2.4)));

		int LUT_SIZE = (int) Functions.max(2000, renderWidth * 20f, renderHeight * 20f); // suitable value?
		final int HALF_LUT_SIZE = (int) (LUT_SIZE / TWO_PI);
		final double[] ratioLookup = new double[(LUT_SIZE) + 1]; // LUT

		/*
		 * Pre-compute the ratio used to scale euclidean distance between each pixel and
		 * the gradient midpoint. I've explained this calculation here:
		 * https://stackoverflow.com/q/11812300/63264634#63264634
		 */
		for (int i = 0; i < ratioLookup.length; i++) {
			double theta = (double) (i * 2) / (LUT_SIZE); // *2 for
			theta *= Math.PI;
			theta -= angle;
			theta = (Math.abs(theta) % SEGMENT_ANGLE);
			ratioLookup[i] = ((MIN_LENGTH_RATIO * FastMath.cos(theta) + FastMath.sin(theta)) * denominator);
		}

		makeThreadPool(gradient, renderStrips, PolygonThread.class, renderMidpointX, renderMidpointY, ratioLookup, HALF_LUT_SIZE);

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
	public void crossGradient(Gradient gradient, PVector centerPoint, double angle, double zoom) {
		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final double denominator = (Math.max(renderHeight, renderWidth) / 2f) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // for 'X' orientation when angle = 0
		angle = TWO_PI - angle; // orient rotation clockwise

		final double sin = FastMath.sin(angle);
		final double cos = FastMath.cos(angle);

		makeThreadPool(gradient, renderStrips, CrossThread.class, renderMidpointX, renderMidpointY, denominator, sin, cos);

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
	public void diamondGradient(Gradient gradient, PVector centerPoint, double angle, double zoom) {
		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final double denominator = (Math.max(renderHeight, renderWidth) / 2f) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // angled at 0
		angle = TWO_PI - angle; // orient rotation clockwise

		final double sin = FastMath.sin(angle);
		final double cos = FastMath.cos(angle);

		makeThreadPool(gradient, renderStrips, DiamondThread.class, renderMidpointX, renderMidpointY, denominator, sin, cos);

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
	 * @see #noiseGradient(Gradient, PVector, double, double, NoiseType)
	 * @see #fractalNoiseGradient(Gradient, PVector, double, double, NoiseType,
	 *      FractalType, int, double, double)
	 */
	public void noiseGradient(Gradient gradient, PVector centerPoint, double angle, double scale) {

		fastNoiseLite.SetNoiseType(NoiseType.OpenSimplex2);
		fastNoiseLite.SetFrequency((float) (1 / scale * 0.001)); // normalise scale to a more appropriate value

		final double sin = FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final double cos = FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		makeThreadPool(gradient, renderStrips, NoiseThread.class, centerPoint, sin, cos);

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
	public void noiseGradient(Gradient gradient, PVector centerPoint, double angle, double scale, NoiseType noiseType) {
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
	public void uniformNoiseGradient(Gradient gradient, PVector centerPoint, double z, double angle, double scale) {
		final double sin = FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final double cos = FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		makeThreadPool(gradient, renderStrips, UniformNoiseThread.class, centerPoint, sin, cos, scale, z);

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
	public void fractalNoiseGradient(Gradient gradient, PVector centerPoint, double angle, double scale, NoiseType noiseType, FractalType fractalType,
			int fractalOctaves, double fractalGain, double fractalLacunarity) {

		fastNoiseLite.SetFrequency((float) (1 / scale * 0.001f)); // normalise scale to a more appropriate value
		fastNoiseLite.SetNoiseType(noiseType);

		fastNoiseLite.SetFractalType(fractalType);
		fastNoiseLite.SetFractalOctaves(fractalOctaves);
		fastNoiseLite.SetFractalGain((float) fractalGain);
		fastNoiseLite.SetFractalLacunarity((float) fractalLacunarity);

		/**
		 * The range of noise values from fastnoise are generally [-1...1] (some have
		 * slightly less range). We use this min and max to map the noise [-1...1] to a
		 * step in the gradient [0...1].
		 */
		double min = -1, max = 1;

		// special case: the range of non-fractal celluar noise is [-1...0]
		if (fractalType == FractalType.None && noiseType == NoiseType.Cellular) {
			max = 0;
		}
		double maxMinDenom = 1 / (max - min); // determines how to scale the noise value to get in necessary range [0...1]

		final double sin = FastMath.sin(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position
		final double cos = FastMath.cos(angle + THREE_QRTR_PI); // +THREE_QRTR_PI to align centrepoint with noise position

		makeThreadPool(gradient, renderStrips, FractalNoiseThread.class, centerPoint, sin, cos, min, maxMinDenom);

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
	public void spotlightGradient(Gradient gradient, final PVector originPoint, double angle, double beamAngle) {
		// TODO horizontal falloff
		beamAngle = Math.min(beamAngle, PI);

		/*
		 * fudge angle slightly to prevent solid line drawn above the gradient when
		 * angle == 0 and divide by 2 to increase input range to a more suitable 0...180
		 * degrees (PI)
		 */
		beamAngle = FastMath.tan((beamAngle + 0.0001) / 2);

		final double sin = FastMath.sin(-angle);
		final double cos = FastMath.cos(-angle);

		final double xDiffMax = (renderWidth / 2f) * beamAngle; // * beamAngle for limit

		makeThreadPool(gradient, renderStrips, SpotlightThread.class, originPoint, sin, cos, beamAngle, xDiffMax);

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
		final double angle = Functions.angleBetween(endPoint, originPoint) + PI * 1.5;
		double beamangle = PApplet.map(PVector.dist(originPoint, endPoint), 0, Math.max(renderWidth, renderHeight) * 1.25f, (float) PI / 8, (float) PI);
		spotlightGradient(gradient, originPoint, angle, beamangle);
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
	 * @see #hourglassGradient(Gradient, PVector, double, double, double, double)
	 */
	public void hourglassGradient(Gradient gradient, PVector centerPoint, double angle, double zoom) {
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
	 * @see #hourglassGradient(Gradient, PVector, double, double)
	 */
	public void hourglassGradient(Gradient gradient, PVector centerPoint, double angle, double zoom, double pinch, double roundness) {
		pinch *= pinch; // square here (since value is used in sqrt() function)

		angle += HALF_PI; // hourglass shape at angle=0

		final double renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final double renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final double denominator = 1 / ((Math.max(renderHeight, renderWidth)) * zoom);

		final double sin = FastMath.sin(PConstants.TWO_PI - angle);
		final double cos = FastMath.cos(angle);

		makeThreadPool(gradient, renderStrips, HourglassThread.class, renderMidpointX, renderMidpointY, sin, cos, zoom, angle, pinch, roundness, denominator);

		gradientPG.updatePixels();

	}

	/**
	 * Creates a pool of threads to split the rendering work for the given gradient
	 * type (each thread works on a horizontal strip portion of the pixels array).
	 * This method starts the threads and returns when all threads have completed.
	 * 
	 * @param gradient     TODO
	 * @param partitionsY
	 * @param gradientType class for the given gradient type thread
	 * @param args         args to pass to gradient thread constructor
	 */
	private void makeThreadPool(Gradient gradient, final int partitionsY, final Class<?> gradientType, final Object... args) {

		// compute LUT
		for (int i = 0; i < gradientCache.length; i++) {
			gradientCache[i] = gradient.getColor((double) i / (gradientCache.length - 1));
		}

		Object[] fullArgs = new Object[3 + args.length]; // empty obj array (to use as input args for new thread instance)
		fullArgs[0] = this; // sub-classes require parent instance as (hidden) first param
		System.arraycopy(args, 0, fullArgs, 3, args.length); // copy the input-args into fullargs

		List<Callable<Boolean>> taskList = new ArrayList<>();

		try {
			@SuppressWarnings("unchecked")
			// only 1 constructor per thread class, so use [0]
			Constructor<? extends RenderThread> constructor = (Constructor<? extends RenderThread>) gradientType.getDeclaredConstructors()[0];

			// rows per strip (except for last strip, which may have less/more, due to floor
			// division)
			int rows = renderHeight / partitionsY;
			for (int strip = 0; strip < partitionsY - 1; strip++) {
				fullArgs[1] = rows * strip; // row vertical offset (y coord to start rendering at)
				fullArgs[2] = rows; // row count (height of horizontal strip)
				RenderThread thread = constructor.newInstance(fullArgs);
				taskList.add(thread);
			}

			fullArgs[1] = rows * (partitionsY - 1);
			fullArgs[2] = renderHeight - rows * (partitionsY - 1);

			RenderThread thread = constructor.newInstance(fullArgs);
			taskList.add(thread);

			// run threads and wait for completion
			List<Future<Boolean>> futures = THREAD_POOL.invokeAll(taskList);
			// errors are swallowed by default, so throw if present
			for (Future<Boolean> future : futures) {
				try {
					future.get();
				} catch (ExecutionException e) {
					throw new RuntimeException(e.getCause());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		// else, the given args probably don't match the thread class args
	}

	/**
	 * @return an appropriate index into the gradient color LUT
	 */
	private int clampAndDither(double t, int x, int y) {
		/*
		 * Clamp first, then dither (i.e. no point dithering values >1 if they are later
		 * clamped back to 1.0.
		 */
		t = (t < 0) ? 0 : (t > 1 ? 1 : t); // clamp between 0...1

		if (ditherStrength > 0) {
			double d = interleavedGradientNoise(x, y);
			t += d;
			// reclamp
			if (t < 0) {
				t = 0;
			}
		}

		// if t==1, index would be out of bounds, so take min()
		int stepInt = Math.min((int) (t * gradientCacheSize), gradientCacheSize - 1);

		return stepInt;
	}

	private double interleavedGradientNoise(final int x, final int y) {
		// http://www.iryoku.com/next-generation-post-processing-in-call-of-duty-advanced-warfare
		// https://bartwronski.com/2016/10/30/dithering-part-three-real-world-2d-quantization-dithering/
		final double v = 52.9829189 * (0.06711056 * x + 0.00583715 * y);
		double n = (v - Math.floor(v)); // take fractional part (range of 1)
		n = n * 2 - 1; // adjust range from [0,1] to [-1,1]
		return n * ditherStrength; // scale output
	}

	/**
	 * Threads operate on a portion (horizontal strip) of the pixels grid.
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
			pixel = (rowOffset + renderOffsetY) * gradientPG.width; // Start at the correct global row, only considering renderOffsetY here.
		}
	}

	private final class LinearThread extends RenderThread {

		private final double odX, odY;
		private final double odSqInverse;
		private double opXod;

		LinearThread(int rowOffset, int rows, double odX, double odY, double odSqInverse, double opXod) {
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
			opXod += rowOffset * odY * scaleY;
			for (int y = rowOffset; y < rowOffset + rows; y++) {
				opXod += odY * scaleY;
				// Add renderOffsetX only at the start of each row.
				pixel += renderOffsetX;
				for (int x = 0; x < renderWidth; x++) {
					double step = (opXod + x * odX * scaleX) * odSqInverse;

					int stepInt = clampAndDither(step, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				// After rendering a row, jump to the beginning of the next row.
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}
			return true;
		}

	}

	private final class RadialThread extends RenderThread {

		private final double renderMidpointX, renderMidpointY;
		private final double zoom;

		RadialThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double zoom) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.zoom = zoom;
		}

		@Override
		public Boolean call() {
			for (int y = rowOffset; y < rowOffset + rows; y++) {
				double rise = renderMidpointY - y;
				rise *= rise;
				pixel += renderOffsetX;
				for (int x = 0; x < renderWidth; x++) {

					double run = renderMidpointX - x;
					run *= run;

					double distSq = run + rise;
					double dist = zoom * distSq;

					int stepInt = clampAndDither(dist, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}
			return true;
		}

	}

	private final class ConicThread extends RenderThread {

		private final double renderMidpointX, renderMidpointY;
		private final double angle;

		ConicThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double angle) {
			super(rowOffset, rows);
			this.renderMidpointX = renderMidpointX;
			this.renderMidpointY = renderMidpointY;
			this.angle = angle;
		}

		@Override
		public Boolean call() {

			double t;
			double rise, run;

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				rise = renderMidpointY - y;
				pixel += renderOffsetX;
				for (int x = 0; x < renderWidth; x++) { // FULL WIDTH
					run = renderMidpointX - x;
					t = Functions.fastAtan2b(rise, run) + Math.PI - angle; // + PI to align bump with angle
					t *= INV_TWO_PI; // normalise
					t -= Math.floor(t); // modulo

					int stepInt = clampAndDither(t, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];

				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
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

		private final double renderMidpointX, renderMidpointY;
		private final double angle;
		private final double curveCount;
		private final double curviness;
		private final double curveDenominator;

		SpiralThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double curveDenominator, double curviness, double angle,
				double curveCount) {
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
				pixel += renderOffsetX;
				double rise = renderMidpointY - y;
				final double riseSquared = rise * rise;
				for (int x = 0; x < renderWidth; x++) { // FULL WIDTH

					double run = renderMidpointX - x;
					t = Functions.fastAtan2b(rise, run) - angle; // -PI...PI
					spiralOffset = curviness == 0.5f ? Math.sqrt((riseSquared + run * run) * curveDenominator)
							: FastPow.fastPow((riseSquared + run * run) * curveDenominator, curviness);
					spiralOffset *= curveCount;
					t += spiralOffset;

					t *= INV_TWO_PI; // normalise
					t -= Math.floor(t); // modulo

					int stepInt = clampAndDither(t, x, y);

					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}
	}

	private final class PolygonThread extends RenderThread {

		private final double renderMidpointX, renderMidpointY;
		private final double[] ratioLookup;
		private final int HALF_LUT_SIZE;

		PolygonThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double[] ratioLookup, int HALF_LUT_SIZE) {
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
				pixel += renderOffsetX;
				yDist = (renderMidpointY - y);
				xDist = renderMidpointX;
				for (int x = 0; x < renderWidth; x++) {
					final double pointDistance = Math.sqrt(yDist * yDist + xDist * xDist); // euclidean dist between (x,y) and midpoint
					xDist--;

					double theta = FastMath.atan2((renderMidpointY - y), (renderMidpointX - x)); // range = -PI...PI
					// Use LUT: +PI to make theta in range 0...2PI and array index positive
					double polygonRatio = ratioLookup[(int) ((theta + Math.PI) * HALF_LUT_SIZE)]; // use LUT

					double dist = polygonRatio * pointDistance;

					final int stepInt = clampAndDither(dist, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private final class CrossThread extends RenderThread {

		private final double renderMidpointX, renderMidpointY;
		private final double denominator;
		private final double sin, cos;

		CrossThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double denominator, double sin, double cos) {
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
				pixel += renderOffsetX;
				final double yTranslate = (y - renderMidpointY);
				for (int x = 0; x < renderWidth; x++) {
					final double newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					final double newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					double dist = Math.min(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // min

					final int stepInt = clampAndDither(dist, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private final class DiamondThread extends RenderThread {

		private final double renderMidpointX, renderMidpointY;
		private final double denominator;
		private final double sin, cos;

		DiamondThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double denominator, double sin, double cos) {
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
				pixel += renderOffsetX;
				final double yTranslate = (y - renderMidpointY);
				for (int x = 0; x < renderWidth; x++) {
					final double newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
					final double newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

					double dist = Math.max(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // max

					final int stepInt = clampAndDither(dist, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private class NoiseThread extends RenderThread {

		final PVector centerPoint;
		final double sin, cos;

		NoiseThread(int rowOffset, int rows, PVector centerPoint, double sin, double cos) {
			super(rowOffset, rows);
			this.centerPoint = centerPoint;
			this.sin = sin;
			this.cos = cos;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				pixel += renderOffsetX;
				final double yTranslate = (y - centerPoint.y);
				for (int x = 0; x < renderWidth; x++) {
					double newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					double newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					double step = fastNoiseLite.getSimplexNoiseFast((float) newXpos, (float) newYpos); // call custom method

					final int stepInt = clampAndDither(step, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private final class UniformNoiseThread extends NoiseThread {

		private final double z;
		private final double scale;

		UniformNoiseThread(int rowOffset, int rows, PVector centerPoint, double sin, double cos, double scale, double z) {
			super(rowOffset, rows, centerPoint, sin, cos);
			this.scale = 1 / (200 * scale);
			this.z = z;
		}

		@Override
		public Boolean call() {

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				pixel += renderOffsetX;
				final double yTranslate = (y - centerPoint.y);
				for (int x = 0; x < renderWidth; x++) {
					double newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					double newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					double step = uniformNoise.uniformNoise(scale * newXpos, newYpos * scale, z);

					final int stepInt = clampAndDither(step, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private final class FractalNoiseThread extends RenderThread {

		private final PVector centerPoint;
		private final double sin, cos;
		private final double min, maxMinDenom;

		FractalNoiseThread(int rowOffset, int rows, PVector centerPoint, double sin, double cos, double min, double maxMinDenom) {
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
				pixel += renderOffsetX;
				final double yTranslate = (y - centerPoint.y);
				for (int x = 0; x < renderWidth; x++) {
					double newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
					double newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

					double step = fastNoiseLite.GetNoise((float) newXpos, (float) newYpos);
					step = ((step - min) * (maxMinDenom)); // scale to 0...1

					final int stepInt = clampAndDither(step, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private final class SpotlightThread extends RenderThread {

		private final PVector originPoint;
		private final double beamAngle;
		private final double xDiffMax;
		private final double sin, cos;

		SpotlightThread(int rowOffset, int rows, PVector originPoint, double sin, double cos, double beamAngle, double xDiffMax) {
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
				pixel += renderOffsetX;
				final double yTranslate = (y - originPoint.y);
				for (int x = 0; x < renderWidth; x++) {
					double newXpos = (x - originPoint.x) * cos - yTranslate * sin + originPoint.x; // rotate x about midpoint
					double newYpos = yTranslate * cos + (x - originPoint.x) * sin + originPoint.y; // rotate y about midpoint

					/*
					 * Calculate the max X difference between this pixel and centrepoint.x when
					 * light fall off reaches the maximum (step = 1) for a given row (at an angle)
					 */
					double fallOffWidth = xDiffMax * ((newYpos - originPoint.y) / renderHeight * beamAngle);
					if (fallOffWidth < 0) { // may be negative if centrePoint.y out of screen
						fallOffWidth = Double.MIN_VALUE; // avoid divide by zero error
					}

					double xDiff = Math.abs(newXpos - originPoint.x); // actual difference in x between this pixel and centerpoint.x

					double step = xDiff / fallOffWidth; // calculate step
					if (step > 1) { // clamp to a high of 1
						step = 1;
					}

					int stepInt = clampAndDither(step, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

	private final class HourglassThread extends RenderThread {

		private final double renderMidpointX, renderMidpointY;
		private final double sin, cos;
		private final double pinch, roundness;
		private final double denominator;

		HourglassThread(int rowOffset, int rows, double renderMidpointX, double renderMidpointY, double sin, double cos, double zoom, double angle,
				double pinch, double roundness, double denominator) {
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

			double newXpos;
			double newYpos;

			double yDist;
			double xDist;

			for (int y = rowOffset; y < rowOffset + rows; y++) {
				pixel += renderOffsetX;
				yDist = (renderMidpointY - y) * (renderMidpointY - y);
				final double yTranslate = (y - renderMidpointY);
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

					double z = (renderMidpointY - newYpos) / (renderMidpointX - newXpos); // atan2(y,x) === atan(y/x), so calc y/x here

					// cos(atan(x)) === sqrt(z * z + 1)
					double dist = Math.sqrt((yDist + xDist + pinch) * (z * z + roundness)) * denominator;

					final int stepInt = clampAndDither(dist, x, y);
					gradientPG.pixels[pixel++] = gradientCache[stepInt];
				}
				pixel += gradientPG.width - (renderWidth + renderOffsetX);
			}

			return true;
		}

	}

}
