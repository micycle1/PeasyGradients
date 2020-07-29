package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

import net.jafama.FastMath;

import peasyGradients.gradient.Gradient;
import peasyGradients.utilities.Functions;

/**
 * Offers both quick constructors for more simple gradients (such as 2 color
 * horizontal) and more powerful constructors for more __ gradients
 * (centre-offset, angled, n-color gradient with color stops)
 * 
 * TODO pshape masks TODO set color interpolation method shape, TODO
 * interpolation mode in this class! gradient
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

	private static final float INV_TWO_PI = 1f / PConstants.TWO_PI;
	private static final float THREE_QRTR_PI = (float) (Math.PI * 3 / 4);

	private boolean debug;
	private final PApplet p;
	private PImage imageMask;
	private PShape shapeMask;
	int colorMode = PConstants.RGB;
	private PGraphics gradientPG;

	private int[] pixelCache;
	private int cacheSize;
	private int[] pixelCacheConic;
	private int cacheSizeConic;

	private boolean renderInternal = true; // render to internal pgraphics, or parent papplet?
	private int renderHeight, renderWidth;
	private int renderOffsetX, renderOffsetY;
	private float scaleY, scaleX;

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
	public void renderIntoPApplet(int x, int y, int width, int height) {
		if (x < 0 || y < 0 || (width + x) > p.width || (y + height) > p.height) {
			System.err.println("Invalid parameters.");
			return;
		}

		renderWidth = width;
		renderHeight = height;
		renderOffsetX = x;
		renderOffsetY = y;
		setRenderTarget(p.getGraphics(), x, y, width, height);
	}

	public void setRenderTarget(PGraphics g) {
		setRenderTarget(g, 0, 0, g.width, g.height);
	}

	private void setRenderTarget(PGraphics g, int offSetX, int offSetY, int width, int height) {

		final int actualWidth = width - offSetX;
		final int actualHeight = height - offSetY;

		scaleX = g.width / (float) width; // used for correct rendering increment
		scaleY = g.height / (float) height; // used for correct rendering increment

		g.beginDraw();
		g.loadPixels(); // only needs to be called once
		g.endDraw();

		renderInternal = false; // don't render to internal PG, let user control PG
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
	 * Restricts any all rendered to use at most n colours (posterisation).
	 * 
	 * @param n max number of colours
	 * @see #clearPosterise()
	 */
	public void posterise(int n) {
		if (n != cacheSize) {
			cacheSize = n; // minimum required for perfectly smooth conic gradient
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
	 * 
	 * @param gradient 1D Gradient to use as the basis for the linear gradient
	 * @param angle    radians. East is 0, moves clockwise
	 * @return
	 */
	public PImage linearGradient(Gradient gradient, float angle) {

		PVector centerPoint = new PVector(gradientPG.width / 2, gradientPG.height / 2);
		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
//		PVector[] o = new PVector[] {new PVector(10, 10), new PVector(20,20)};
		if (angle > PConstants.HALF_PI && angle <= PConstants.HALF_PI * 3) {
			return linearGradient(gradient, centerPoint, o[1], o[0]);
		} else {
			return linearGradient(gradient, centerPoint, o[0], o[1]);
		}
	}

	/**
	 * 
	 * @param gradient
	 * @param centerPoint
	 * @param angle       in radians
	 * @return
	 */
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
	 * @param angle       in radians
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
		for (int y = 0, x; y < renderHeight; ++y) { // loop for quality = 0 (every pixel)
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

		if (debug) {
			gradientPG.ellipseMode(PConstants.CENTER);
			gradientPG.fill(250);
			gradientPG.ellipse(controlPoint1.x, controlPoint1.y, 25, 25);
			gradientPG.fill(0);
			gradientPG.ellipse(controlPoint2.x, controlPoint2.y, 25, 25);
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

		float hypotSq = (renderWidth * renderWidth) + (renderHeight * renderHeight);
		float rise, run, distSq, dist;
		zoom = 1 / zoom;

		float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

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
				dist = zoom * 4.0f * distSq / hypotSq;
				if (dist > 1) {
					dist = 1; // constrain
				}

				int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCache[stepInt];

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
	 * @return
	 * @see #conicGradientSmooth(Gradient, PVector, float, float)
	 */
	public PImage conicGradient(Gradient gradient, PVector midPoint, float angle) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		float rise, run;
		double t = 0;

		float renderMidpointX = (midPoint.x / gradientPG.width) * renderWidth;
		float renderMidpointY = (midPoint.y / gradientPG.height) * renderHeight;

		for (int y = 0, x; y < renderHeight; ++y) {
			rise = renderMidpointY - y;
			run = renderMidpointX;
			for (x = 0; x < renderWidth; ++x) {

				t = Functions.fastAtan2(rise, run) + angle;

				// Ensure a positive value if angle is negative.
				t = Functions.floorMod(t, PConstants.TWO_PI);

				// Divide by TWO_PI to get value in range 0...1
				t *= INV_TWO_PI;

				int stepInt = (int) (t * cacheSizeConic);
				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];

				run -= 1;
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
	public PImage conicGradientSmooth(Gradient gradient, PVector midPoint, float angle) {
		gradient.push(gradient.colourAt(0)); // add copy of first colour to end
		PImage out = conicGradient(gradient, midPoint, angle);
		gradient.removeLast(); // remove colour copy
		return out;
	}
	
	public PImage diamondGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		final double denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom; // calc here, not in loop
		final double sin = FastMath.sin(angle);
		final double cos = FastMath.cos(angle);

		double dist = 0;

		double newXpos;
		double newYpos;

		for (int y = 0, x; y < renderHeight; ++y) {
			final double yTranslate = (y - midPoint.y);
			for (x = 0; x < renderWidth; ++x) {

				newXpos = (x - midPoint.x) * cos - yTranslate * sin + midPoint.x; // rotate y about midpoint
				newYpos = yTranslate * cos + (x - midPoint.x) * sin + midPoint.y; // rotate y about midpoint

				dist = Math.max(Math.abs(newYpos - midPoint.y), Math.abs(newXpos - midPoint.x)) / denominator; // max

				if (dist > 1) {
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
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

	public PImage diamondGradientInverse(Gradient gradient, PVector midPoint, float angle, float zoom) {

		gradient.prime();

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		final double denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom; // calc here, not in loop
		final double sin = FastMath.sin(angle);
		final double cos = FastMath.cos(angle);

		double dist = 0;

		double newXpos;
		double newYpos;

		for (int y = 0, x; y < renderHeight; ++y) {
			final double yTranslate = (y - midPoint.y);
			for (x = 0; x < renderWidth; ++x) {

				newXpos = (x - midPoint.x) * cos - yTranslate * sin + midPoint.x; // rotate y about midpoint
				newYpos = yTranslate * cos + (x - midPoint.x) * sin + midPoint.y; // rotate y about midpoint

				dist = Math.min(Math.abs(newYpos - midPoint.y), Math.abs(newXpos - midPoint.x)) / denominator; // min

				if (dist > 1) {
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
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

	public void quadGradient() {
		// TODO multiple linear gradient passes, then lerp?
	}

	public void multiGradient() {

		// TODO two/n pass
	}

	/**
	 * Petal gradient
	 * 
	 * @param gradient
	 * @param midPoint
	 * @param angle
	 * @param zoom
	 * @param petals nearest multiple of 2 (floor)
	 * @return
	 */
	public PImage petalGradient(Gradient gradient, PVector midPoint, float angle, float zoom, int petals) {

		gradient.prime();
		petals /= 2;
		
		double denominator = (Math.max(renderHeight, renderWidth) / 2) * zoom;

		for (int i = 0; i < pixelCacheConic.length; i++) { // calc LUT
			pixelCacheConic[i] = gradient.evalRGB(i / (float) pixelCacheConic.length);
		}

		double yDist;
		double xDist;
		for (int y = 0, x; y < renderHeight; ++y) {
			yDist = (midPoint.y - y) * (midPoint.y - y);
			for (x = 0; x < renderWidth; ++x) {
				xDist = (midPoint.x - x) * (midPoint.x - x);

				// TODO calc based on prev value
				float pointAngle = Functions.angleBetween(midPoint, x, y) - angle + THREE_QRTR_PI;
				pointAngle*=petals;
				
				final double absdist = Math.sqrt(yDist + xDist); // euclidean distance between midpoint and (x,y)

				double dist = Math
						.abs(absdist / (FastMath.cosQuick(pointAngle) + FastMath.sinQuick(pointAngle)) / denominator);

				if (dist > 1) {
					dist = 1;
				}

				final int stepInt = (int) (dist * cacheSize);

				gradientPG.pixels[gradientPG.width * (y + renderOffsetY) + (x + renderOffsetX)] = pixelCacheConic[stepInt];
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
}
