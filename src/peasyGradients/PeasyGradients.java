package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * TODO conic & sweep gradietn
 * (https://css-tricks.com/snippets/css/css-conic-gradient/)
 * 
 * TODO dithering/banding/rgb depth TODO parallelStream for
 * iteration/calculation?
 * 
 * TODO text masking
 * 
 * API:
 * rectPane.applyLinearGradient(gradient).applyCircularGradient(gradient1).setColorspace(HSV).get().getRaw().applyRadialGradientMask().get()
 * 
 * gradient.mask(shape).mask(opacity)
 * 
 * @author micycle1
 *
 */
public final class PeasyGradients {

	private final static int threads = Runtime.getRuntime().availableProcessors();
	private static final float INV_TWO_PI = 1f/PConstants.TWO_PI;

	private boolean debug;
	private final PApplet p;
	private PImage imageMask;
	private PShape shapeMask;
	int colorMode = PConstants.RGB;
	private PGraphics gradientPG;
	private ExecutorService exec;
	private int quality = 1; // calc every pixel

	private boolean PGPrimed = false;

	public PeasyGradients(PApplet p) {
		this.p = p;
		exec = Executors.newFixedThreadPool(threads);
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

		gradientPG = p.createGraphics(width, height);
		PGPrimed = true;
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
	 * 
	 * @param gradient    colour source
	 * @param centerPoint gradient midpoint
	 * @param angle       radians
	 * @param length      coefficient to lerp from centrepoint to edges (that
	 *                    intersect with angle). default = 1: (first and last
	 *                    colours will be exactly on edge); <1: colours will be
	 *                    sqashed; >1 colours spread out (outermost colours will
	 *                    leave the view).
	 * @return
	 * 
	 * 
	 *         Preset: Several predefined configurations are provided in this menu
	 *         for your use. Start Point: Use these controls to define the location
	 *         of the start point of the gradient. Position: Sets the location for
	 *         the start point, using X (horizontal) and Y (vertical) values. End
	 *         Point: Use these controls to define the location of the end point of
	 *         the gradient. Position: Sets the location for the end point, using X
	 *         (horizontal) and Y (vertical) values. Ramp Scatter: Adds subtle noise
	 *         into the gradient areas between colors, which can help to improve
	 *         naturalness. Blend: Select the blend mode used to combine the
	 *         gradient with the contents of the layer to which it is applied.
	 */
	public PImage linearGradient(Gradient gradient, PVector centerPoint, float angle, float length) {

		if (!PGPrimed) {
			System.err.println("First use setCanvas() to create a plane to render into");
			return null;
		}

		// get edge-line intersection points
		PVector[] o = Functions.lineRectIntersection(gradientPG.width, gradientPG.height, centerPoint, angle);
		o[0].lerp(centerPoint, 1 - length); // mutate length
		o[1].lerp(centerPoint, 1 - length); // mutate length

		gradient.prime(); // prime curr color stop

		gradientPG.beginDraw();
		gradientPG.loadPixels();

		int recentPixel = 0;
		for (int i = 0, y = 0, x; y < gradientPG.height; ++y) {
			for (x = 0; x < gradientPG.width; ++x, ++i) {
				if ((i & quality) == 0) { // faster modulo
					float step = Functions.linearProject(o[0].x, o[0].y, o[1].x, o[1].y, x, y);
					recentPixel = gradient.evalRGB(step);
				}
				gradientPG.pixels[i] = recentPixel;
			}
		}
		gradientPG.updatePixels();

		if (debug) {
			gradientPG.ellipseMode(PConstants.CENTER);
			gradientPG.fill(250);
			gradientPG.ellipse(o[0].x, o[0].y, 50, 50);
			gradientPG.fill(0);
			gradientPG.ellipse(o[1].x, o[1].y, 50, 50);
			gradientPG.ellipse(centerPoint.x, centerPoint.y, 10, 10);
		}

		gradientPG.endDraw();

//		if (mask) { // TODO
//		 /// apply mask
//		}
		return gradientPG.get();
	}

	public PImage radialGradient(Gradient gradient, PVector midPoint, float zoom) {

		float hypotSq = (gradientPG.width * gradientPG.width) + (gradientPG.height * gradientPG.height);
		float rise, run, distSq, dist;
		zoom = 1/zoom;
		
		gradient.prime();
		
		gradientPG.beginDraw();
		gradientPG.loadPixels();

		for (int i = 0, y = 0, x; y < gradientPG.height; ++y) {
			rise = midPoint.y - y;
			rise *= rise;

			for (x = 0; x < gradientPG.width; ++x, ++i) {
				run = midPoint.x - x;
				run *= run;

				distSq = run + rise;
				dist = zoom*4.0f * distSq / hypotSq;
				if (dist > 1) {
					dist = 1; // constrain
				}
				gradientPG.pixels[i] = gradient.evalRGB(dist);
			}
		}
		gradientPG.updatePixels();
		gradientPG.endDraw();
		
		return gradientPG.get();
	}

	public PImage conicGradient(Gradient gradient, PVector midPoint, float angle, float zoom) {
		
		float rise, run;
		
		gradientPG.beginDraw();
		gradientPG.loadPixels();
		
		gradient.prime();
		
		double t;
		for (int i = 0, y = 0, x; y < gradientPG.height; ++y) {
			rise = midPoint.y - y;
			for (x = 0; x < gradientPG.width; ++x, ++i) {
				run = midPoint.x - x;
				
				t = Functions.fastAtan2(rise, run) - angle;

				// Ensure a positive value if angle is negative.
				t = Functions.floorMod(t, PConstants.TWO_PI);

				// Divide by TWO_PI to get value in range 0...1
				t *= INV_TWO_PI;

				gradientPG.pixels[i] = gradient.evalRGB((float) t);
			}
		}
		gradientPG.updatePixels();
		gradientPG.endDraw();

		return gradientPG.get();
	}

	public void lineGradient() {
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

	/**
	 * 5 = best (redner every pixel) 4 = every 2, etc. 1 = calc every 16th
	 * 
	 * @param quality
	 */
	public void setQuality(int quality) {
		this.quality = (quality > 5) ? 5 : (quality < 1 ? 1 : quality); // clamp to 1...5
		quality = 1 << (5 - quality); // 2^(5-quality)
		quality--; // subtract 1 here for the faster modulo
		// TODO calc every nth, but linear interpolate between?)
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
