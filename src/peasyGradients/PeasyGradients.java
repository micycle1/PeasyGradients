package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;

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
 * Turns 1D gradients into 2D images.
 * 
 * <p>
 * Offers both quick constructors for more simple gradients (such as 2 color
 * horizontal) and more powerful constructors for more __ gradients
 * (centre-offset, angled, n-color gradient with color stops)
 * 
 * <p>
 * TODO pshape masks TODO set color interpolation method shape, TODO
 * interpolation mode in this class! gradient
 * Shape.applycolorgradient(gradient).applyopacitygradient(shape.applyopacity))
 * 
 * API:
 * rectPane.applyLinearGradient(gradient).setOpacity().applyCircularGradient(gradient1).setcolorspace(HSV).get().getRaw().applyRadialGradientMask().get()
 * 
 * gradient.mask(shape).mask(opacity) lineargradient() mask get()
 * 
 * https://www.filterforge.com/wiki/index.php/Index_of_Gradient_Types
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
	private static final double HALF_PI = (0.5f * Math.PI);
	private static final double QRTR_PI = (0.25 * Math.PI);
	private static final float INV_TWO_PI = 1f / PConstants.TWO_PI;

	public final FastNoiseLite fastNoiseLite = new FastNoiseLite(0); // using default seed

	private final PApplet p;
//	int colorMode = PConstants.RGB;
	private PImage gradientPG; // reference to object to render into

	private int[] pixelCache;
	private int cacheSize;
	private int[] pixelCacheConic;
	private int cacheSizeConic;

	private int renderHeight, renderWidth;
	private int renderOffsetX, renderOffsetY;
	private float scaleY, scaleX;

	/**
	 * Constructs a new PeasyGradients renderer from a running sketch.
	 * 
	 * @param p the Processing sketch. You'll usually refer to it as {@code this}
	 */
	public PeasyGradients(PApplet p) {
		this.p = p;

		renderIntoSketch();

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
	 * @param offSetX x-axis offset of the region to render gradients into (0 is
	 * @param offSetY
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

		scaleX = g.width / (float) width; // used for correct rendering increment
		scaleY = g.height / (float) height; // used for correct rendering increment

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

		cacheSize = (int) Math.ceil(Math.max(actualWidth, actualHeight) * 1.4143);
		pixelCache = new int[cacheSize + 1];

		cacheSizeConic = (int) (2 * Math.ceil(Math.max(actualWidth, actualHeight)));
		pixelCacheConic = new int[cacheSizeConic + 1];
	}

	/**
	 * Change the seed used by the noise generator used by noise gradients.
	 * 
	 * @param seed
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
		if (n != cacheSize) {
			cacheSize = n;
			cacheSizeConic = n;
			pixelCache = new int[n + 1];
			pixelCacheConic = new int[n + 1];
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
	 * It’s called “linear” because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient 1D Gradient to use as the basis for the linear gradient
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
	 * It’s called “linear” because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient
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
	 * It’s called “linear” because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient    color source
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
	 * @param gradient
	 * @param controlPoint1 the location for the start point, using X (horizontal)
	 *                      and Y (vertical) values (can extend past the coordinates
	 *                      of the sketch)
	 * @param controlPoint2 location of the end point of the gradient (can extend
	 *                      past the coordinates of the sketch)
	 */
	public void linearGradient(Gradient gradient, PVector controlPoint1, PVector controlPoint2) {

		gradient.prime(); // prime curr color stop

		/**
		 * Pre-compute vals for linearprojection
		 */
		float odX = controlPoint2.x - controlPoint1.x; // Rise and run of line.
		float odY = controlPoint2.y - controlPoint1.y; // Rise and run of line.
		final float odSqInverse = 1 / (odX * odX + odY * odY); // Distance-squared of line.
		float opXod = -controlPoint1.x * odX + -controlPoint1.y * odY;

		int xOff = 0;
		float step = 0;

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.getColor(i / (float) pixelCache.length);
		}

		/**
		 * Usually, we'd call Functions.linearProject() to calculate step, but the
		 * function is inlined here to optimise speed.
		 */
		for (int y = 0, x; y < renderHeight; ++y) {
			opXod += odY * scaleY;
			xOff = 0;
			for (x = 0; x < renderWidth; ++x) {
				step = (opXod + xOff) * odSqInverse; // get position of point on 1D gradient and normalise
				step = (step < 0) ? 0 : (step > 1 ? 1 : step); // clamp
				int stepInt = (int) (step * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
				xOff += odX * scaleX;
			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * Renders a radial gradient.
	 * 
	 * <p>
	 * A radial gradient starts at a single point (the source point for color stops)
	 * and radiates outwards.
	 * 
	 * @param gradient
	 * @param centerPoint The midpoint of the gradient -- the position where it
	 *                    radiates from.
	 * @param zoom
	 */
	public void radialGradient(Gradient gradient, PVector centerPoint, float zoom) {

		final float hypotSq = (renderWidth * renderWidth) + (renderHeight * renderHeight);
		float rise, run, distSq, dist;
		zoom = 4 / zoom; // calc here, not in loop
		zoom /= hypotSq; // calc here, not in loop

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		gradient.prime();

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.getColor(i / (float) pixelCache.length);
		}

		for (int y = 0, x; y < renderHeight; ++y) {
			rise = renderMidpointY - y;
			rise *= rise;

			for (x = 0; x < renderWidth; ++x) {
				run = renderMidpointX - x;
				run *= run;

				distSq = run + rise;
				dist = zoom * distSq;

				if (dist > 1) { // clamp to a high of 1
					dist = 1;
				}

				int stepInt = (int) (dist * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];

			}
		}

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
	 * They’re called “conic” because they tend to look like the shape of a cone
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
	 * @param gradient
	 * @param centerPoint
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @see #conicGradientSmooth(Gradient, PVector, float, float)
	 */
	public void conicGradient(Gradient gradient, PVector centerPoint, float angle) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.getColor(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		for (int y = 0, x; y < renderHeight; ++y) {
			rise = renderMidpointY - y;
			run = renderMidpointX;
			for (x = 0; x < renderWidth; ++x) {

				t = Functions.fastAtan2b(rise, run) + Math.PI - angle; // + PI to align bump with angle
				t *= INV_TWO_PI; // normalise
				t -= Math.floor(t); // modulo

				int stepInt = (int) (t * cacheSizeConic);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];

				run--;
			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * Renders a {@link #conicGradient(Gradient, PVector, float) conic gradient}
	 * with a smooth transition between the first and last colors (unlike
	 * {@link #conicGradient(Gradient, PVector, float, float) this} method where
	 * there is a hard transition). For this reason, this method generally gives
	 * nice looking (more gradient-like) results.
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @see #conicGradient(Gradient, PVector, float, float)
	 */
	public void conicGradientSmooth(Gradient gradient, PVector centerPoint, float angle) {
		gradient.push(gradient.colorAt(0)); // add copy of first color to end
		conicGradient(gradient, centerPoint, angle);
		gradient.removeLast(); // remove color copy
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
	 * @param gradient    1D Gradient to use as the basis for the linear gradient
	 * @param centerPoint PVector specifying midpoint coordinate
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @param curveCount  akin to zoom
	 */
	public void spiralGradient(Gradient gradient, PVector centerPoint, float angle, float curveCount) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.getColor(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;
		double spiralOffset = 0;
		angle %= TWO_PIf;

		final double curveDenominator = 1d / (renderWidth * renderWidth + renderHeight * renderHeight);
		curveCount *= TWO_PI;

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		for (int y = 0, x; y < renderHeight; ++y) {

			rise = renderMidpointY - y;
			final double riseSquared = rise * rise;
			run = renderMidpointX;

			for (x = 0; x < renderWidth; ++x) {

				t = Functions.fastAtan2b(rise, run) - angle; // -PI...PI
				spiralOffset = curveCount * Math.sqrt((riseSquared + run * run) * curveDenominator);
				t += spiralOffset;

				t *= INV_TWO_PI; // normalise
				t -= Math.floor(t); // modulo

				int stepInt = (int) (t * cacheSizeConic);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];

				run--;
			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * Specifiy curviness
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle       in radians, where 0 is east; going clockwise
	 * @param curveCount
	 * @param curviness   curviness exponent. affect how distance from center point
	 *                    affects curve
	 */
	public void spiralGradient(Gradient gradient, PVector centerPoint, final float angle, float curveCount, float curviness) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.getColor(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;
		double spiralOffset = 0;

		final double curveDenominator = 1d / (renderWidth * renderWidth + renderHeight * renderHeight);
		curveCount *= TWO_PI;

		curviness = 1f / curviness;
		curviness *= 0.5; // curviness of 1 == exponenent of 0.5

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		for (int y = 0, x; y < renderHeight; ++y) {

			rise = renderMidpointY - y;
			final double riseSquared = rise * rise;
			run = renderMidpointX;

			for (x = 0; x < renderWidth; ++x) {

				t = Functions.fastAtan2b(rise, run) - angle; // -PI...PI
				spiralOffset = curveCount * FastPow.fastPow((riseSquared + run * run) * curveDenominator, curviness);
				t += spiralOffset;

				t *= INV_TWO_PI; // normalise
				t -= Math.floor(t); // modulo

				int stepInt = (int) (t * cacheSizeConic);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];

				run--;
			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * Leading to a 'X' shape.
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle       in radians, where 0 orients the 'X' ; going clockwise
	 * @param zoom
	 */
	public void crossGradient(Gradient gradient, PVector centerPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.getColor(i / (float) pixelCacheConic.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final float denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // for 'X' orientation when angle = 0
		final float sin = (float) FastMath.sin(angle);
		final float cos = (float) FastMath.cos(angle);

		float dist = 0;

		float newXpos;
		float newYpos;

		for (int y = 0, x; y < renderHeight; ++y) {
			final float yTranslate = (y - renderMidpointY);
			for (x = 0; x < renderWidth; ++x) {

				newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
				newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

				dist = Math.min(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointY)) / denominator; // min

				if (dist > 1) { // clamp to a high of 1
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
			}
		}

		gradientPG.updatePixels();
	}

	/**
	 * Renders a polygonal gradient
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle       angle offset to render polygon at
	 * @param zoom
	 * @param sides       Number of polgyon sides. Should be at least 3 (a triangle)
	 */
	public void polygonGradient(Gradient gradient, PVector centerPoint, float angle, float zoom, int sides) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.getColor(i / (float) pixelCacheConic.length);
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

//		angle = (float) (TWO_PI - angle);
		angle %= SEGMENT_ANGLE; // mod angle to minimise difference between theta and SEGMENT_ANGLE in loop

		double dist = 0;

		final double denominator = MIN_LENGTH_RATIO / ((Math.max(renderHeight, renderWidth)) * (0.01 * zoom * FastPow.fastPow(sides, 2.4)));

		double yDist; // y distance between midpoint and a given pixel
		double xDist; // x distance between midpoint and a given pixel

		final double midpointXSquared = renderMidpointX * renderMidpointX;
		final double twoMidpointX = 2 * renderMidpointX;

		final int LUT_SIZE = 1000;
		final int HALF_LUT_SIZE = (int) (LUT_SIZE / TWO_PI);
		final double[] ratioLookup = new double[(int) (LUT_SIZE) + 1]; // LUT

		/*
		 * Pre-compute the ratio used to scale euclidean distance between each pixel and
		 * the gradient midpoint. I've explained this calculation here:
		 * https://stackoverflow.com/q/11812300/63264634#63264634
		 */
		for (int i = 0; i < ratioLookup.length; i++) {
			double theta = (float) (i * 2) / (LUT_SIZE); // *2 for
			theta *= Math.PI;
			theta -= angle;
			theta = Math.abs(theta) % SEGMENT_ANGLE;
			ratioLookup[i] = (MIN_LENGTH_RATIO * FastMath.cosQuick(theta) + FastMath.sinQuick(theta)) * denominator;
		}

		for (int y = 0, x; y < renderHeight; ++y) {

			yDist = (renderMidpointY - y) * (renderMidpointY - y);
			xDist = midpointXSquared + 1;
			int inc = 1; // (N+x*i)^2, difference between successive Ns.
			for (x = 0; x < renderWidth; ++x) {

				xDist -= twoMidpointX;
				xDist += inc;
				double pointDistance = Math.sqrt(yDist + xDist); // euclidean dist between (x,y) and midpoint
				inc += 2;

				double theta = Functions.fastAtan2b((renderMidpointY - y), (renderMidpointX - x)); // range = -PI...PI

				// Use LUT: +PI to make theta in range 0...2PI and array index positive
				double polygonRatio = ratioLookup[(int) ((theta + Math.PI) * HALF_LUT_SIZE)]; // use LUT

				dist = polygonRatio * pointDistance;

				if (dist > 1) { // clamp to a high of 1
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSizeConic);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];

			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * Renders a gradient where colors are plotted according to the manhattan
	 * distance between the position and midpoint, forming a diamond-shaped
	 * spectrum.
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle
	 * @param zoom
	 */
	public void diamondGradient(Gradient gradient, PVector centerPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.getColor(i / (float) pixelCache.length);
		}

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final double denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom; // calc here, not in loop
		angle += PConstants.QUARTER_PI; // angled at 0
		final double sin = FastMath.sin(angle);
		final double cos = FastMath.cos(angle);

		double dist = 0;

		double newXpos;
		double newYpos;

		for (int y = 0, x; y < renderHeight; ++y) {
			final double yTranslate = (y - renderMidpointY);
			for (x = 0; x < renderWidth; ++x) {

				newXpos = (x - renderMidpointX) * cos - yTranslate * sin + renderMidpointX; // rotate x about midpoint
				newYpos = yTranslate * cos + (x - renderMidpointX) * sin + renderMidpointY; // rotate y about midpoint

				dist = Math.max(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointX)) / denominator; // max

				if (dist > 1) { // clamp to a high of 1
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

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
	 * @param gradient
	 * @param centerPoint used only when rotating
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

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.getColor(i / (float) pixelCache.length);
		}

		gradientPG.updatePixels();

		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		final float[][] noiseVals = new float[gradientPG.width][gradientPG.height];

		/**
		 * Even the simpler noise types are quite expensive to compute for every pixel,
		 * so we calculate a noise value for every 4th pixel (every 2nd pixel on both
		 * axes), and then interpolate these values for other pixels later. Visually
		 * this isn't apparent since the noise value for adjacent pixels is mostly
		 * gradual anyway.
		 */
		for (int y = 0, x; y < renderHeight; y += 2) {
			final float yTranslate = (y - centerPoint.y);

			for (x = 0; x < renderWidth; x += 2) {

				float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
				float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

				float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method
				noiseVals[x][y] = step;

				final int stepInt = (int) (step * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		/**
		 * Interpolate horizontally (x-axis)
		 */
		for (int y = 0, x; y < renderHeight; ++y) {
			for (x = 1; x < renderWidth - 1; x += 2) {
				noiseVals[x][y] = (noiseVals[x - 1][y] + noiseVals[x + 1][y]) / 2;
				final int stepInt = (int) (noiseVals[x][y] * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		/**
		 * Interpolate vertically (y-axis)
		 */
		for (int y = 1, x; y < renderHeight - 1; y += 2) {
			for (x = 0; x < renderWidth; x += 1) {
				noiseVals[x][y] = (noiseVals[x][y - 1] + noiseVals[x][y + 1]) / 2;
				final int stepInt = (int) (noiseVals[x][y] * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		/**
		 * Pass over very bottom row
		 */
		float yTranslate = (renderHeight - 1 - centerPoint.y);
		for (int x = 0; x < renderWidth; x++) {
			float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
			float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

			float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method

			final int stepInt = (int) (step * cacheSize);

			gradientPG.pixels[gradientPG.width * (renderHeight - 1 + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
		}

		/**
		 * Pass over right-most column
		 */

		for (int y = 0; y < renderHeight; y++) {
			yTranslate = (y - centerPoint.y);
			float newXpos = ((renderWidth - 1) - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
			float newYpos = yTranslate * cos + ((renderWidth - 1) - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

			float step = fastNoiseLite.getSimplexNoiseFast(newXpos, newYpos); // call custom method

			final int stepInt = (int) (step * cacheSize);

			gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (renderWidth - 1 + renderOffsetX)] = pixelCache[stepInt];
		}
	}

	/**
	 * 
	 * @param gradient
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
	 * @param gradient
	 * @param centerPoint
	 * @param angle
	 * @param scale
	 * @param noiseType         The type of noise: Cellular, Simplex, Perlin, Value,
	 *                          etc.
	 * @param fractalType       The type of fractal: FBm, Ridged, PingPong,
	 *                          DomainWarpProgressive, DomainWarpIndependent.
	 * @param fractalOctaves    More octaves lead to finer and finer detail. At
	 *                          least 1...4 is suitable
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

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.getColor(i / (float) pixelCache.length);
		}

		gradientPG.updatePixels();

		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		final float[][] noiseVals = new float[gradientPG.width][gradientPG.height];

		float min = 0, max = 0;

		/**
		 * Even the simpler noise types are quite expensive to compute for every pixel,
		 * so we calculate a noise value for every 4th pixel (every 2nd pixel on both
		 * axes), and then interpolate these values for other pixels later. Visually
		 * this isn't apparent since the noise value for adjacent pixels is mostly
		 * gradual anyway.
		 */
		for (int y = 0, x; y < renderHeight; y += 2) {
			final float yTranslate = (y - centerPoint.y);

			for (x = 0; x < renderWidth; x += 2) {

				float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
				float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

				float step = fastNoiseLite.GetNoise(newXpos, newYpos);
				/**
				 * dynamically map the step to 0...1 based on the noise range (which varies
				 * depending on noise type, etc.)
				 */
				min = Math.min(min, step);
				max = Math.max(max, step);
				float maxMinDenom = 1 / (max - min);
				step = ((step - min) * (maxMinDenom));
				noiseVals[x][y] = step;

				final int stepInt = (int) (step * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		/**
		 * Interpolate horizontally (x-axis)
		 */
		for (int y = 0, x; y < renderHeight; ++y) {
			for (x = 1; x < renderWidth - 1; x += 2) {
				noiseVals[x][y] = (noiseVals[x - 1][y] + noiseVals[x + 1][y]) / 2;
				final int stepInt = (int) (noiseVals[x][y] * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		/**
		 * Interpolate vertically (y-axis)
		 */
		for (int y = 1, x; y < renderHeight - 1; y += 2) {
			for (x = 0; x < renderWidth; x += 1) {
				noiseVals[x][y] = (noiseVals[x][y - 1] + noiseVals[x][y + 1]) / 2;
				final int stepInt = (int) (noiseVals[x][y] * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		/**
		 * Pass over very bottom row
		 */
		float yTranslate = (renderHeight - 1 - centerPoint.y);
		for (int x = 0; x < renderWidth; x++) {
			float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
			float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

			float step = fastNoiseLite.GetNoise(newXpos, newYpos);

			min = Math.min(min, step);
			max = Math.max(max, step);
			float maxMinDenom = 1 / (max - min);
			step = ((step - min) * (maxMinDenom));
			final int stepInt = (int) (step * cacheSize);

			gradientPG.pixels[gradientPG.width * (renderHeight - 1 + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
		}

		/**
		 * Pass over right-most column
		 */

		for (int y = 0; y < renderHeight; y++) {
			yTranslate = (y - centerPoint.y);
			float newXpos = ((renderWidth - 1) - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
			float newYpos = yTranslate * cos + ((renderWidth - 1) - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

			float step = fastNoiseLite.GetNoise(newXpos, newYpos);

			min = Math.min(min, step);
			max = Math.max(max, step);
			float maxMinDenom = 1 / (max - min);
			step = ((step - min) * (maxMinDenom));

			final int stepInt = (int) (step * cacheSize);

			gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (renderWidth - 1 + renderOffsetX)] = pixelCache[stepInt];
		}

	}

	/**
	 * Generates a cone-shaped (stagelight) gradient. TODO finish! examples:
	 * https://www.filterforge.com/filters/2312.html
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle
	 */
	public void coneGradient(Gradient gradient, PVector centerPoint, float angle) {

		gradient.prime(); // prime curr color stop

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		double step = 0;
		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.getColor(i / (float) pixelCache.length);
		}

		/**
		 * Usually, we'd call Functions.linearProject() to calculate step, but the
		 * function is inlined here to optimise speed.
		 */
		for (int y = 0, x; y < renderHeight; ++y) { // loop for quality = 0 (every pixel)
			final float yTranslate = (y - centerPoint.y);
			for (x = 0; x < renderWidth; ++x) {
//				step = 1-(Math.abs(x-centerPoint.x)/renderWidth);
//				step = Math.abs(x-centerPoint.x) < y? 1: 0;
//				step*=(y/(float)renderHeight);

				step = Math.abs(x - centerPoint.x) > ((y - centerPoint.y) / 2) ? 0
						: 1 - Math.abs(x - centerPoint.x) / ((y - centerPoint.y) / 2);
				float W = renderWidth - centerPoint.x;
				step = (1 - (y / (float) renderHeight));

				double test = 0;
				if (x < (centerPoint.x + W * (y / renderHeight)) || x > (centerPoint.x - (y / renderHeight))) {
					test = 1 - (Math.abs(x - centerPoint.x) / renderWidth);
				}
//				* Math.max((y / (float) renderHeight), 0.00001)
//				System.out.println(test);
				step = step * test;
//				System.out.println(step);
//				System.out.println(step);
//				System.out.println(step);
//				  = x
//				step *= (y / (float) renderHeight);

//				float newXpos = (x - centerPoint.x) * cos - yTranslate * sin + centerPoint.x; // rotate x about midpoint
//				float newYpos = yTranslate * cos + (x - centerPoint.x) * sin + centerPoint.y; // rotate y about midpoint

//				System.out.println(step);
				int stepInt = (int) (step * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * aka quad gradient, aka grid gradient
	 */
	public void multiGradient() {
		// TODO two/n pass
	}

	/**
	 * Renders what I've described as a an hourglass gradient, owing to it's
	 * similarity with an hourglass at certain angles.
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle
	 * @param zoom        default = 1
	 */
	public void hourglassGradient(Gradient gradient, PVector centerPoint, float angle, float zoom) {

		gradient.prime();

		final float renderMidpointX = (centerPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (centerPoint.y / gradientPG.height) * renderHeight;

		final double denominator = 1 / ((Math.max(renderHeight, renderWidth)) * zoom);

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.getColor(i / (float) pixelCacheConic.length);
		}

		final float sin = (float) FastMath.sin(PApplet.TWO_PI - angle);
		final float cos = (float) FastMath.cos(angle);

		float newXpos;
		float newYpos;

		float yDist;
		float xDist;

		for (int y = 0, x; y < renderHeight; ++y) {
			yDist = (renderMidpointY - y) * (renderMidpointY - y);
			final float yTranslate = (y - renderMidpointY);

			for (x = 0; x < renderWidth; ++x) {
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

				double dist = Math.sqrt((yDist + xDist) * (z * z + 1)) * denominator; // cos(atan(x)) === sqrt(z * z + 1)

				if (dist > 1) { // clamp to a high of 1
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSizeConic);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
			}
		}

		gradientPG.updatePixels();

	}
}
