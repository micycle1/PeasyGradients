package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PVector;

import net.jafama.FastMath;

import peasyGradients.gradient.Gradient;
import peasyGradients.utilities.FastNoise;
import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;

/**
 * Turns 1D gradients into 2D images.
 * 
 * Offers both quick constructors for more simple gradients (such as 2 color
 * horizontal) and more powerful constructors for more __ gradients
 * (centre-offset, angled, n-color gradient with color stops)
 * 
 * TODO pshape masks TODO set color interpolation method shape, TODO
 * interpolation mode in this class! gradient
 * Shape.applycolorgradient(gradient).applyopacitygradient(shape.applyopacity))
 * 
 * API:
 * rectPane.applyLinearGradient(gradient).setOpacity().applyCircularGradient(gradient1).setColorspace(HSV).get().getRaw().applyRadialGradientMask().get()
 * 
 * gradient.mask(shape).mask(opacity) lineargradient() mask get()
 * 
 * https://www.filterforge.com/wiki/index.php/Index_of_Gradient_Types
 * 
 * <p>
 * Algorithms for linear/radial/conic gradient calculation are based on <a href=
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

	private final PApplet p;
//	int colorMode = PConstants.RGB;
	private PGraphics gradientPG; // reference to object to render into

	private FastNoise fastNoise = new FastNoise();

	private int[] pixelCache;
	private int cacheSize;
	private int[] pixelCacheConic;
	private int cacheSizeConic;

	private int renderHeight, renderWidth;
	private int renderOffsetX, renderOffsetY;
	private float scaleY, scaleX;

	public PeasyGradients(PApplet p) {
		this.p = p;
	}

	/**
	 * @see #renderIntoPApplet(int, int, int, int)
	 */
	public void renderIntoPApplet() {
		renderIntoPApplet(0, 0, p.width, p.height);
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @see #renderIntoPApplet()
	 */
	public void renderIntoPApplet(int offSetX, int offSetY, int width, int height) {
		if (offSetY < 0 || offSetY < 0 || (width + offSetX) > p.width || (offSetY + height) > p.height) {
			System.err.println("Invalid parameters.");
			return;
		}
		setRenderTarget(p.getGraphics(), offSetX, offSetY, width, height);
	}

	public void setRenderTarget(PGraphics g) {
		setRenderTarget(g, 0, 0, g.width, g.height);
	}

	private void setRenderTarget(PGraphics g, int offSetX, int offSetY, int width, int height) {
		System.out.println(width);

		final int actualWidth = width - offSetX;
		final int actualHeight = height - offSetY;

		scaleX = g.width / (float) width; // used for correct rendering increment
		scaleY = g.height / (float) height; // used for correct rendering increment

		renderWidth = width;
		renderHeight = height;
		renderOffsetX = offSetX;
		renderOffsetY = offSetY;

		g.beginDraw();
		g.loadPixels(); // only needs to be called once
		g.endDraw();

		gradientPG = g;

		cacheSize = (int) Math.ceil(Math.max(actualWidth, actualHeight) * 1.4143);
		pixelCache = new int[cacheSize + 1];

		cacheSizeConic = (int) (2 * Math.ceil(Math.max(actualWidth, actualHeight)));
		pixelCacheConic = new int[cacheSizeConic + 1];
	}

	/**
	 * This defines how the gradient's colors are represented when they are
	 * interpolated. This can dramatically affect how a gradient (the transition
	 * colors) looks.
	 */
	public void setColorSpace() {
		// TODO ?
	}

	/**
	 * Restricts any all rendered to use at most n colours (posterisation).
	 * 
	 * @param n max number of colours
	 * @see #clearPosterise()
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
	 * Clears any user-defined colour posterisation.
	 * 
	 * @see #posterise(int)
	 */
	public void clearPosterise() {
		setRenderTarget(gradientPG, renderOffsetX, renderOffsetY, renderWidth, renderHeight);
	}

	/**
	 * Renders into the parent sketch unless otherwise
	 * {@link #setRenderTarget(PGraphics) specified}.
	 * 
	 * @param gradient 1D Gradient to use as the basis for the linear gradient
	 * @param angle    radians. East is 0, moves clockwise
	 * @return
	 */
	public void linearGradient(Gradient gradient, float angle) {

		PVector centerPoint = new PVector(gradientPG.width / 2, gradientPG.height / 2);
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);

		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			linearGradient(gradient, o[1], o[0]);
		} else {
			linearGradient(gradient, o[0], o[1]);
		}
	}

	/**
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle       in radians
	 * @return
	 */
	public void linearGradient(Gradient gradient, PVector centerPoint, float angle) {

		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);

		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			linearGradient(gradient, o[1], o[0]);
		} else {
			linearGradient(gradient, o[0], o[1]);
		}
	}

	/**
	 * 
	 * @param gradient    colour source
	 * @param centerPoint gradient midpoint
	 * @param angle       in radians
	 * @param length      coefficient to lerp from centrepoint to edges (that
	 *                    intersect with angle). default = 1: (first and last
	 *                    colours will be exactly on edge); <1: colours will be
	 *                    sqashed; >1 colours spread out (outermost colours will
	 *                    leave the view).
	 */
	public void linearGradient(Gradient gradient, PVector centerPoint, float angle, float length) {
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		o[0].lerp(centerPoint, 1 - length); // mutate length
		o[1].lerp(centerPoint, 1 - length); // mutate length

		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			linearGradient(gradient, o[1], o[0]);
		} else {
			linearGradient(gradient, o[0], o[1]);
		}
	}

	/**
	 * User defined control points. try to make on a line.
	 * 
	 * * It’s called “linear” because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param controlPoint1 the location for the start point, using X (horizontal)
	 *                      and Y (vertical) values.
	 * @param controlPoint2 location of the end point of the gradient.
	 * @param angle
	 * @return
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
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
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
	 * A radial gradient differs from a linear gradient in that it starts at a
	 * single point and emanates outward.
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param zoom
	 * @return
	 */
	public void radialGradient(Gradient gradient, PVector midPoint, float zoom) {

		final float hypotSq = (renderWidth * renderWidth) + (renderHeight * renderHeight);
		float rise, run, distSq, dist;
		zoom = 4 / zoom; // calc here, not in loop
		zoom /= hypotSq; // calc here, not in loop

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

		gradient.prime();

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
		}

		for (int y = 0, x; y < renderHeight; ++y) {
			rise = renderMidpointY - y;
			rise *= rise;

			for (x = 0; x < renderWidth; ++x) {
				run = renderMidpointX - x;
				run *= run;

				distSq = run + rise;
				dist = zoom * distSq;
				if (dist > 1) {
					dist = 1; // constrain
				}

				int stepInt = (int) (dist * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];

			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * A conic gradient is similar to a radial gradient. Both are circular and use
	 * the center of the element as the source point for color stops. However, where
	 * the color stops of a radial gradient emerge from the center of the circle, a
	 * conic gradient places them around the circle. Aka "angled".
	 * 
	 * <p>
	 * They’re called “conic” because they tend to look like the shape of a cone
	 * that is being viewed from above. Well, at least when there is a distinct
	 * angle provided and the contrast between the color values is great enough to
	 * tell a difference.
	 * 
	 * <p>
	 * This method creates a hard stop where the last and first colors bump right up
	 * to one another. See
	 * {@link #conicGradientSmooth(Gradient, PVector, float, float)
	 * conicGradientSmooth()} to smoothly transition between the first and last
	 * colours.
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle    in radians, where east is 0 and moves clockwise
	 * @param zoom     default=1
	 * @see #conicGradientSmooth(Gradient, PVector, float, float)
	 */
	public void conicGradient(Gradient gradient, PVector midPoint, float angle) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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
	 * Renders a conic gradient with a smooth transition between the first and last
	 * colours (unlike {@link #conicGradient(Gradient, PVector, float, float) this}
	 * method where there is a hard transition). For this reason, this method
	 * generally gives nice looking (more gradient-like) results.
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 * @param offset
	 * @return
	 * @see #conicGradient(Gradient, PVector, float, float)
	 */
	public void conicGradientSmooth(Gradient gradient, PVector midPoint, float angle) {
		gradient.push(gradient.colourAt(0)); // add copy of first colour to end
		conicGradient(gradient, midPoint, angle);
		gradient.removeLast(); // remove colour copy
	}

	/**
	 * A spiral gradient is similar to a conic gradient, where instead of colours
	 * extending at the same angle from the midpoint,
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param curveCount
	 */
	public void spiralGradient(Gradient gradient, PVector midPoint, float angle, float curveCount) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;
		double spiralOffset = 0;
		angle %= TWO_PIf;

		final double curveDenominator = 1d / (renderWidth * renderWidth + renderHeight * renderHeight);
		curveCount *= TWO_PI;

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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
	 * @param midPoint
	 * @param angle
	 * @param curveCount
	 * @param curviness
	 * @return
	 */
	public void spiralGradient(Gradient gradient, PVector midPoint, final float angle, float curveCount, float curviness) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;
		double spiralOffset = 0;

		final double curveDenominator = 1d / (renderWidth * renderWidth + renderHeight * renderHeight);
		curveCount *= TWO_PI;

		curviness = 1f / curviness;
		curviness *= 0.5; // curviness of 1 == exponenent of 0.5

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 */
	public void crossGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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

				dist = Math.min(Math.abs(newYpos - renderMidpointY), Math.abs(newXpos - renderMidpointY)) / denominator; // min

				if (dist > 1) {
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
			}
		}

		gradientPG.updatePixels();
	}

	/**
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 * @param sides    at least 3
	 */
	public void polygonGradient(Gradient gradient, PVector midPoint, float angle, float zoom, int sides) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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
			int inc = 1; // (N+x*i)^2, difference between successive ones.
			for (x = 0; x < renderWidth; ++x) {

				xDist -= twoMidpointX;
				xDist += inc;
				double pointDistance = Math.sqrt(yDist + xDist); // euclidean dist between (x,y) and midpoint
				inc += 2;

				double theta = Functions.fastAtan2b((renderMidpointY - y), (renderMidpointX - x)); // -PI...PI

				// Use LUT: +PI to makes array index positive 0...2PI
				double polygonRatio = ratioLookup[(int) ((theta + Math.PI) * HALF_LUT_SIZE)]; // use LUT

				dist = polygonRatio * pointDistance;

				if (dist > 1) { // clamp gradient
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSizeConic);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];

			}
		}

		gradientPG.updatePixels();

	}

	/**
	 * A gradient where colours are according to the manhattan distance between the
	 * point and midpoint, leading to a diamond shape.
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 */
	public void diamondGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
		}

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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

				if (dist > 1) {
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		gradientPG.updatePixels();

	}

	public void noiseGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {
		gradient.prime();

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
		}

		gradientPG.updatePixels();

		fastNoise.SetFrequency(0.02f);
//		noise2.SetInterp(FastNoise.Interp.Quintic);
//		noise2.SetFractalOctaves(5);
		fastNoise.SetCellularDistanceFunction(FastNoise.CellularDistanceFunction.Euclidean);
		fastNoise.SetFractalLacunarity(2);
		fastNoise.SetFractalGain(0.5f);
//		noise2.SetGradientPerturbAmp(1);
		fastNoise.SetCellularReturnType(FastNoise.CellularReturnType.Distance2Div);
		fastNoise.SetNoiseType(FastNoise.NoiseType.Cellular);

		fastNoise.SetInterp(FastNoise.Interp.Linear);

		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		double min = 0, max = 0;
		for (int y = 0, x; y < renderHeight; ++y) {
			final float yTranslate = (y - midPoint.y);

			for (x = 0; x < renderWidth; ++x) {

				float newXpos = (x - midPoint.x) * cos - yTranslate * sin + midPoint.x; // rotate x about midpoint
				float newYpos = yTranslate * cos + (x - midPoint.x) * sin + midPoint.y; // rotate y about midpoint

//				double step = noise.eval(x * inc, y * inc); // -0.8313049677675405...0.8074247384505447
				double step = fastNoise.GetCellular(newXpos, newYpos);
//				step = Math.abs(step);

				min = Math.min(min, step);
				max = Math.max(max, step);
				double maxMinDenom = 1 / (max - min);
				step = ((step - min) * (maxMinDenom)); // map to 0...1

				final int stepInt = (int) (step * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

	}

	public void coneGradient(Gradient gradient, PVector midPoint, float angle) {

		gradient.prime(); // prime curr color stop

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

		double step = 0;
		final float sin = (float) FastMath.sin(-angle);
		final float cos = (float) FastMath.cos(-angle);

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
		}

		/**
		 * Usually, we'd call Functions.linearProject() to calculate step, but the
		 * function is inlined here to optimise speed.
		 */
		for (int y = 0, x; y < renderHeight; ++y) { // loop for quality = 0 (every pixel)
			final float yTranslate = (y - midPoint.y);
			for (x = 0; x < renderWidth; ++x) {
//				step = 1-(Math.abs(x-midPoint.x)/renderWidth);
//				step = Math.abs(x-midPoint.x) < y? 1: 0;
//				step*=(y/(float)renderHeight);

				step = Math.abs(x - midPoint.x) > ((y - midPoint.y) / 2) ? 0 : 1 - Math.abs(x - midPoint.x) / ((y - midPoint.y) / 2);
				step *= (y / (float) renderHeight);

//				float newXpos = (x - midPoint.x) * cos - yTranslate * sin + midPoint.x; // rotate x about midpoint
//				float newYpos = yTranslate * cos + (x - midPoint.x) * sin + midPoint.y; // rotate y about midpoint

//				System.out.println(step);
				int stepInt = (int) (step * cacheSize);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];
			}
		}

		gradientPG.updatePixels();

	}

	public void multiGradient() {
		// TODO two/n pass
	}

	/**
	 * Hourglass gradient
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom     default = 1
	 * @return
	 */
	public void hourglassGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {

		gradient.prime();

		final float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		final float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

		final double denominator = 1 / ((Math.max(renderHeight, renderWidth)) * zoom);

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
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

				double dist = Math.sqrt((yDist + xDist) * (z * z + 1)) * denominator; // sqrt(z * z + 1) === cos(atan(x))

				if (dist > 1) {
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSizeConic);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
			}
		}

		gradientPG.updatePixels();

	}
}
