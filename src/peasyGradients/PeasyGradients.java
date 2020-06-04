package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

import static processing.core.PConstants.PI;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * TODO dithering/banding/rgb depth
 * TODO parallelStream for iteration/calculation?
 * 
 * gradient.mask(shape).mask(opacity)
 * 
 * @author micycle1
 *
 */
public final class PeasyGradients {

	private final static int threads = Runtime.getRuntime().availableProcessors();

	static final int HORIZONTAL = 0;
	static final int VERTICAL = 1;
	boolean debug;
	private final PApplet p;
	PImage imageMask;
	PShape shapeMask;
	int colorMode = PConstants.RGB;
	PGraphics gradientPG;
	ExecutorService exec;

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

	/**
	 * This defines how the gradient's colors are represented when they are
	 * interpolated. This can dramatically affect how a gradient (the transition
	 * colors) looks.
	 */
	public void setColorSpace() {

	}

	/**
	 * 
	 * @param dimensions
	 * @param centre
	 * @param angle
	 * @param smoothing  coefficient to lerp from centrepoint to edges (that
	 *                   intersect with angle) TODO
	 * @param palette
	 * @return
	 * 
	 * Preset: Several predefined configurations are provided in this menu for your use.
Start Point: Use these controls to define the location of the start point of the gradient.
Position: Sets the location for the start point, using X (horizontal) and Y (vertical) values.
End Point: Use these controls to define the location of the end point of the gradient.
Position: Sets the location for the end point, using X (horizontal) and Y (vertical) values.
Ramp Scatter: Adds subtle noise into the gradient areas between colors, which can help to improve naturalness.
Blend: Select the blend mode used to combine the gradient with the contents of the layer to which it is applied.
	 */
	public PImage linearGradient(PVector dimensions, PVector centre, Gradient gradient) {

		if (!(dimensions.x >= 0 && dimensions.y >= 0)) {
			System.err.println("Gradient dimensions must be greater than 0.");
			return null;
		}

		float theta = Functions.angleBetween(new PVector(p.mouseX, p.mouseY), centre);
		float dist = PApplet.dist(p.mouseX, p.mouseY, centre.x, centre.y);

		float xo = (float) (Math.cos(theta + PI) * dist);
		float yo = (float) (Math.sin(theta + PI) * dist);

		float ox = p.mouseX;
		float oy = p.mouseY;
		float dx = centre.x + xo;
		float dy = centre.y + yo;

		final int threadCount = 4;
		gradientPG = p.createGraphics((int) dimensions.x, (int) dimensions.y);

		gradientPG.beginDraw();
		gradientPG.loadPixels();
		final int pixels = gradientPG.pixels.length / threadCount;
		HashSet<LinearThread> threads = new HashSet<>(threadCount);
		for (int i = 0; i < threadCount; i++) {
			threads.add(new LinearThread(i * pixels, pixels, ox, oy, dx, dy, gradient));
		}

		try {
			exec.invokeAll(threads); // run threads, block until all finished
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		gradientPG.updatePixels();
		gradientPG.endDraw();

//		float m = (float) Math.tan(angle);
//
//		float c = centre.y - (m * centre.x); // y = mx+c >> y-mx = c
//
//		// find points of intersection with PImage boundary
//		// solves 2 of 4 possible points
//		// | x = c
//		//
//
//		float y1 = c; // x = 0
////		float y1x = 
//
//		float y2 = (dimensions.x - c) / m;
//
//		float x1 = -c / m; // y=0
//		float x2 = (dimensions.y - c) / m;

//		float dist = dist(mouseX, mouseY, midPoint.x, midPoint.y);
//
//		float xo = (float) (Math.cos(angle + PI) * dist);
//		float yo = (float) (Math.sin(angle + PI) * dist);
//
//		float ox = centre.x;
//		float oy = centre.y;
//		float dx = centre.x + xo;
//		float dy = centre.y + yo;

//		for (int i = 0, y = 0, x; y < g.height; ++y) {
//			for (x = 0; x < g.width; ++x, ++i) {
//				float step = project(ox, oy, dx, dy, x, y);
//				g.pixels[i] = lerpColors(palette, step, colorMode);
//			}
//		}
//
//		if (debug) {
//			g.strokeWeight(1);
//			g.stroke(255);
//			g.line(ox, oy, dx, dy);
//		}

		return gradientPG.get();
	}

	public PImage conicGradient(PVector dimensions, Gradient gradient, float angle) {
		return null;
	}

	public PImage conicGradient(PVector headPos, PVector tailPos, Gradient gradient) {
		return null;
	}

	public void lineGradient() {
	}

	/**
	 * Draws a bezier curve with a gradient.
	 */
	public void drawBezierCurveWithGradient(PVector headPos, PVector tailPos, Gradient gradient, float curviness,
			int curveDirection, int strokeWeight) {
		final float theta2 = Functions.angleBetween(tailPos, headPos);
		final PVector midPoint = new PVector((headPos.x + tailPos.x) / 2, (headPos.y + tailPos.y) / 2);
		final PVector bezierCPoint = new PVector(midPoint.x + (PApplet.sin(-theta2) * (curviness * 2 * curveDirection)),
				midPoint.y + (PApplet.cos(-theta2) * (curviness * 2 * curveDirection)));

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
	 * 5 = best (redner every pixel) 4 = every 2, etc. 1 = render every
	 * 
	 * @param quality
	 */
	public void setQuality(int quality) {
		quality = (quality > 5) ? 5 : (quality < 1 ? 1 : quality); // clamp to 1...5
//		quality = 2^(6-quality) // TODO
	}

	public static boolean withinRegion(PVector point, PVector UL, PVector BR) {
		return (point.x >= UL.x && point.y >= UL.y) && (point.x <= BR.x && point.y <= BR.y) // SE
				|| (point.x >= BR.x && point.y >= BR.y) && (point.x <= UL.x && point.y <= UL.y) // NW
				|| (point.x <= UL.x && point.x >= BR.x) && (point.y >= UL.y && point.y <= BR.y) // SW
				|| (point.x <= BR.x && point.x >= UL.x) && (point.y >= BR.y && point.y <= UL.y); // NE
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
				float step = Functions.project(ox, oy, dx, dy, i % p.width, (i - i % p.width) / p.height);
				gradientPG.pixels[i] = gradient.evalRGB(step);
			}
			return true;
		}

	}

}
