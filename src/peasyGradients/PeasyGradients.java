package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.Callable;

import peasyGradients.gradient.Gradient;
import peasyGradients.utilities.Functions;

/**
 * Offers both quick constructors for more simple gradients (such as 2 color
 * horizontal) and more powerful constructors for more __ gradients
 * (centre-offset, angled, n-color gradient with color stops)
 * 
 * TODO memoisation TODO pshape masks TODO set color interpolation method shape,
 * TODO interpolation mode in this class! gradient
 * Shape.applycolorgradient(gradient).applyopacitygradient(shape.applyopacity))
 * 
 * TODO dithering/banding/rgb depth TODO parallelStream for
 * iteration/calculation?
 * 
 * TODO text masking
 * 
 * TODO JBLAS for multiplication
 * 
 * API:
 * rectPane.applyLinearGradient(gradient).setOpacity().applyCircularGradient(gradient1).setColorspace(HSV).get().getRaw().applyRadialGradientMask().get()
 * 
 * gradient.mask(shape).mask(opacity) lineargradient() mask get()
 * 
 * <p>
 * Algorithms for linear/radial/conic gradient calculation are based on <a href=
 * "https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2">this</a>
 * work by Jeremy Behreandt
 * 
 * @author micycle1
 *
 */
public final class PeasyGradients {

	private final static int threads = Runtime.getRuntime().availableProcessors();
	private static final float INV_TWO_PI = 1f / PConstants.TWO_PI;
	private static final float THREE_QRTR_PI = (float) (Math.PI * 3 / 4);

	private boolean debug;
	private final PApplet p;
	private PImage imageMask;
	private PShape shapeMask;
	int colorMode = PConstants.RGB;
	private PGraphics gradientPG;
	int[] pixels;
	HashMap<Float, Integer> cache = new HashMap<>();
	int[] pixelCache;
	int cacheSize;
	int[] pixelCacheConic;
	int cacheSizeConic;

	private boolean renderInternal = true; // render internal pgraphics, or parent papplet?
	private int pAppletRenderHeight, pAppletRenderWidth;
	private int pAppletRenderOffsetX, pAppletRenderOffsetY;

	private PGraphics emptyPGraphics;

	public PeasyGradients(PApplet p) {
		this.p = p;
		emptyPGraphics = p.createGraphics(0, 0);
	}

	/**
	 * Mask will apply to the next gradient only.
	 * 
	 * @param mask
	 */
	public void setMask(PImage mask) {

	}

	public void setCanvas(int width, int height) {
		if (!(width >= 0 && height >= 0)) {
			System.err.println("Dimensions must each be greater than 0.");
			return;
		}
		pixels = new int[width * height];
		gradientPG = p.createGraphics(width, height);
		gradientPG.beginDraw();
		gradientPG.loadPixels();
		gradientPG.endDraw();
//		cacheSize = (int) (Math.max(gradientPG.width, gradientPG.height) * 1.4143); // minimum required for linear gradient
		cacheSize = 2 * (gradientPG.width + gradientPG.height); // minimum required for perfectly smooth conic gradient
		pixelCache = new int[cacheSize + 1];
		renderInternal = true;
	}

	/**
	 * @see #renderIntoPApplet(int, int, int, int)
	 */
	public void renderIntoPApplet() {
		pAppletRenderWidth = p.width;
		pAppletRenderHeight = p.height;
		pAppletRenderOffsetX = 0;
		pAppletRenderOffsetY = 0;
		setRenderTarget(p.getGraphics());
	}

	/**
	 * 
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 * @see #renderIntoPApplet()
	 */
	public void renderIntoPApplet(int x, int y, int width, int height) {
		if (x < 0 || y < 0 || width > p.width || height > p.height) {
			System.err.println("Invalid parameters.");
			return;
		}
		
		pAppletRenderWidth = width;
		pAppletRenderHeight = height;
		pAppletRenderOffsetX = x;
		pAppletRenderOffsetY = y;
		setRenderTarget(p.getGraphics());
	}

	public void setRenderTarget(PGraphics g) {
		g.loadPixels(); // only needs to be called once
		renderInternal = false; // don't render to internal PG, let user control PG
		gradientPG = g;
		
		cacheSize = (int) Math.ceil(Math.max(gradientPG.width, gradientPG.height) * 1.4143);
		pixelCache = new int[cacheSize + 1];
		
		cacheSizeConic = (int) (2 * Math.ceil(Math.max(gradientPG.width, gradientPG.height)));
		pixelCacheConic = new int[cacheSizeConic + 1];
		
//		pixels = new int[g.width * g.height];
	}

	/**
	 * This defines how the gradient's colors are represented when they are
	 * interpolated. This can dramatically affect how a gradient (the transition
	 * colors) looks.
	 */
	public void setColorSpace() {

	}

	/**
	 * Return RGB (OR LAB?) int grid (not PImage)
	 * 
	 * @return
	 */
	public int[] getRaw() {
		return null; // TODO
	}

	/**
	 * Restrict the gradient to use at most n colours (posterisation).
	 * 
	 * @param n
	 * @see #resetPosterise()
	 */
	public void posterise(int n) {
		cacheSize = n; // minimum required for perfectly smooth conic gradient
		pixelCache = new int[n + 1];
	}

	/**
	 * Clears any user-defined colour posterisation.
	 * @see #posterise(int)
	 */
	public void resetPosterise() {
		cacheSize = 2 * (gradientPG.width + gradientPG.height); // minimum required for perfectly smooth conic gradient
		pixelCache = new int[cacheSize + 1];
	}
	

	public PImage linearGradient(Gradient gradient, float angle) {

		PVector centerPoint = new PVector(gradientPG.width / 2, gradientPG.height / 2);
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			return linearGradient(gradient, centerPoint, o[1], o[0]);
		} else {
			return linearGradient(gradient, centerPoint, o[0], o[1]);
		}
	}

	public PImage linearGradient(Gradient gradient, PVector centerPoint, float angle) {

		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			return linearGradient(gradient, centerPoint, o[1], o[0]);
		} else {
			return linearGradient(gradient, centerPoint, o[0], o[1]);
		}
	}

	/**
	 * 
	 * @param gradient    colour source
	 * @param centerPoint gradient midpoint
	 * @param angle       radians
	 * @param length      coefficient to lerp from centrepoint to edges (that
	 *                    intersect with angle). default = 1: (first and last
	 *                    colours will be exactly on edge); <1: colours will be
	 *                    sqashed; >1 colours spread out (outermost colours will
	 *                    leave the view).
	 */
	public PImage linearGradient(Gradient gradient, PVector centerPoint, float angle, float length) {
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		o[0].lerp(centerPoint, 1 - length); // mutate length
		o[1].lerp(centerPoint, 1 - length); // mutate length
		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			return linearGradient(gradient, centerPoint, o[1], o[0]);
		} else {
			return linearGradient(gradient, centerPoint, o[0], o[1]);
		}
	}

//	void linearGradiet(BlendMode b)

	/**
	 * User defined control points. try to make on a line.
	 * 
	 * * It’s called “linear” because the colors flow from left-to-right,
	 * top-to-bottom, or at any angle you chose in a single direction.
	 * 
	 * Preset: Several predefined configurations are provided in this menu for your
	 * use. Start Point: Use these controls to define the location of the start
	 * point of the gradient.
	 * 
	 * 
	 * Ramp Scatter: Adds subtle noise into the gradient areas between colors, which
	 * can help to improve naturalness.
	 * 
	 * Blend: Select the blend mode used to combine the gradient with the contents
	 * of the layer to which it is applied.
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param controlPoint1 the location for the start point, using X (horizontal)
	 *                      and Y (vertical) values.
	 * @param controlPoint2 location of the end point of the gradient.
	 * @param angle
	 * @return
	 */
	public PImage linearGradient(Gradient gradient, PVector centerPoint, PVector controlPoint1, PVector controlPoint2) {
//		if (!PGPrimed) {
//			System.err.println("First use setCanvas() to create a plane to render into");
//			return null;
//		}

		gradient.prime(); // prime curr color stop

		/**
		 * Pre-compute vals for linearprojection
		 */
		final float odX = controlPoint2.x - controlPoint1.x; // Rise and run of line.
		final float odY = controlPoint2.y - controlPoint1.y; // Rise and run of line.
		final float odSqInverse = 1 / (odX * odX + odY * odY); // Distance-squared of line.
		float opXod = -controlPoint1.x * odX + -controlPoint1.y * odY;

		int xOff = 0;
		float step = 0;

		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
		}

		if (renderInternal) {
			/**
			 * Usually, we'd call Functions.linearProject() to calculate step, but the
			 * function is inlined here to optimise speed.
			 */
			for (int i = 0, y = 0, x; y < gradientPG.height; ++y) { // loop for quality = 0 (every pixel)
				opXod += odY;
				xOff = 0;
				for (x = 0; x < gradientPG.width; ++x, ++i) {
					step = (opXod + xOff) * odSqInverse; // get position of point on 1D gradient and normalise
					step = (step < 0) ? 0 : (step > 1 ? 1 : step); // clamp
					int stepInt = (int) (step * cacheSize);
					gradientPG.pixels[i] = pixelCache[stepInt];
					xOff += odX;
				}
			}
		} else {
			for (int i = 0, y = pAppletRenderOffsetY, x; y < pAppletRenderHeight; ++y) { // loop for quality = 0 (every pixel)
				opXod += odY;
				xOff = 0;
				for (x = pAppletRenderOffsetX; x < pAppletRenderWidth; ++x, ++i) {
					step = (opXod + xOff) * odSqInverse; // get position of point on 1D gradient and normalise
					step = (step < 0) ? 0 : (step > 1 ? 1 : step); // clamp
					int stepInt = (int) (step * cacheSize);
					gradientPG.pixels[i] = pixelCache[stepInt];
					xOff += odX;
				}
			}
		}

		gradientPG.updatePixels();

		if (debug) {
			gradientPG.ellipseMode(PConstants.CENTER);
			gradientPG.fill(250);
			gradientPG.ellipse(controlPoint1.x, controlPoint1.y, 50, 50);
			gradientPG.fill(0);
			gradientPG.ellipse(controlPoint2.x, controlPoint2.y, 50, 50);
			gradientPG.ellipse(centerPoint.x, centerPoint.y, 10, 10);
		}
		
		if (renderInternal) {
			gradientPG.endDraw();
			return gradientPG;
		} else {
			return emptyPGraphics; // avoid caller null pointer
		}

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
	public PImage radialGradient(Gradient gradient, PVector midPoint, float zoom) {

		float hypotSq = (gradientPG.width * gradientPG.width) + (gradientPG.height * gradientPG.height);
		float rise, run, distSq, dist;
		zoom = 1 / zoom;

		gradient.prime();
		
		for (int i = 0; i < pixelCache.length; i++) { // calc LUT
			pixelCache[i] = gradient.evalRGB(i / (float) pixelCache.length);
		}

		for (int i = 0, y = 0, x; y < gradientPG.height; ++y) {
			rise = midPoint.y - y;
			rise *= rise;

			for (x = 0; x < gradientPG.width; ++x, ++i) {
				run = midPoint.x - x;
				run *= run;

				distSq = run + rise;
				dist = zoom * 4.0f * distSq / hypotSq;
				if (dist > 1) {
					dist = 1; // constrain
				}

				int stepInt = (int) (dist * cacheSize);
				
				gradientPG.pixels[i] = pixelCache[stepInt];
			}
		}
		
		gradientPG.updatePixels();
		
		if (renderInternal) {
			gradientPG.endDraw();
			return gradientPG.get();
		} else {
			return emptyPGraphics;
		}
	}

	/**
	 * A conic gradient is similar to a radial gradient. Both are circular and use
	 * the center of the element as the source point for color stops. However, where
	 * the color stops of a radial gradient emerge from the center of the circle, a
	 * conic gradient places them around the circle.
	 * 
	 * <p>
	 * They’re called “conic” because they tend to look like the shape of a cone
	 * that is being viewed from above. Well, at least when there is a distinct
	 * angle provided and the contrast between the color values is great enough to
	 * tell a difference.
	 * 
	 * <p>
	 * This method creates a hard stop where the last and first colors bump right up
	 * to one another.
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 * @return
	 * @see #conicGradientSmooth(Gradient, PVector, float, float)
	 */
	public PImage conicGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {

		gradient.prime();
		
		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;

		for (int i = 0, y = 0, x; y < gradientPG.height; ++y) {
			rise = midPoint.y - y;
			run = midPoint.x;
			for (x = 0; x < gradientPG.width; ++x, ++i) {
				run = midPoint.x - x;

				t = Functions.fastAtan2(rise, run) + angle;

				// Ensure a positive value if angle is negative.
				t = Functions.floorMod(t, PConstants.TWO_PI);

				// Divide by TWO_PI to get value in range 0...1
				t *= INV_TWO_PI;
				int stepInt = (int) (t * pixelCacheConic.length);
				gradientPG.pixels[i] = pixelCacheConic[stepInt];
			}
		}

		gradientPG.updatePixels();

		if (renderInternal) {
			gradientPG.endDraw();
			return gradientPG;
		} else {
			return emptyPGraphics;
		}
	}

	/**
	 * Renders a conic gradient with a smooth transition between the first and last
	 * colours (unlike {@link #conicGradient(Gradient, PVector, float, float) this}
	 * where there is a hard transition). For this reason, this method generally
	 * gives nice looking (more gradient-like) results.
	 * 
	 * TODO offset angle: https://css-tricks.com/snippets/css/css-conic-gradient/
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 * @param offset
	 * @return
	 * @see #conicGradient(Gradient, PVector, float, float)
	 */
	public PImage conicGradientSmooth(Gradient gradient, PVector midPoint, float angle, float zoom) {
		gradient.push(gradient.colourAt(0)); // add copy of first colour to end
		PImage out = conicGradient(gradient, midPoint, angle, zoom);
		gradient.removeLast(); // remove colour copy
		return out;
	}
	
	public void quadGradient() {
		// TODO multiple linear gradient passes, then lerp?
	}

	/**
	 * Rasterize gradient and export as PImage
	 * 
	 * @return
	 */
	public PImage get() {
		return gradientPG.get();
	}

	/**
	 * Draws a bezier curve with a gradient.
	 */
	public void drawBezierCurveWithGradient(PVector headPos, PVector tailPos, Gradient gradient, float curviness, int curveDirection,
			int strokeWeight) {
		final float theta2 = Functions.angleBetween(tailPos, headPos);
		final PVector centerPoint = new PVector((headPos.x + tailPos.x) / 2, (headPos.y + tailPos.y) / 2);
		final PVector bezierCPoint = new PVector(centerPoint.x + (PApplet.sin(-theta2) * (curviness * 2 * curveDirection)),
				centerPoint.y + (PApplet.cos(-theta2) * (curviness * 2 * curveDirection)));

		PVector point = headPos.copy(); // initial point

		for (float t = 0; t <= 1; t += 0.01) {
			float x1 = p.bezierPoint(headPos.x, bezierCPoint.x, bezierCPoint.x, tailPos.x, t);
			float y1 = p.bezierPoint(headPos.y, bezierCPoint.y, bezierCPoint.y, tailPos.y, t);
			PVector pointB = new PVector(x1, y1);
			p.stroke(gradient.evalRGB(PApplet.abs(PApplet.sin(t + p.frameCount * 0.02f))));
			p.line(point.x, point.y, pointB.x, pointB.y);
			point = pointB.copy(); // previous point
		}
	}

	/**
	 * Use a pshape as an alpha mask
	 * 
	 * @param gradient
	 * @param shape
	 * @return
	 */
	public PImage maskWithPShape(PImage gradient, PShape shape) {
		return null;
	}

	public class LinearThread implements Callable<Boolean> {

		final int offset, pixels;
		final float ox, oy, dx, dy;
		final Gradient gradient;

		public LinearThread(int offset, int pixels, float ox, float oy, float dx, float dy, Gradient gradient) {
			this.offset = offset;
			this.gradient = gradient;
			this.dx = dx;
			this.dy = dy;
			this.ox = ox;
			this.oy = oy;
			this.pixels = pixels;
		}

		@Override
		public Boolean call() {
			for (int i = offset; i < offset + pixels; i++) {
				float step = Functions.linearProject(ox, oy, dx, dy, i % p.width, (i - i % p.width) / p.height);
				gradientPG.pixels[i] = gradient.evalRGB(step);
			}
			return true;
		}

	}

}
