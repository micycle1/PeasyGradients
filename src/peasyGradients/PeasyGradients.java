package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PShape;
import processing.core.PVector;

import static processing.core.PConstants.PI;

/**
 * Offers both quick constructors for more simple gradients (such as 2 color
 * horizontal) and more powerful constructors for more __ gradients
 * (centre-offset, angled, n-color gradient with color stops)
 * 
 * TODO memoisation TODO pshape masks TODO set color interpolation method shape,
 * gradient
 * Shape.applycolorgradient(gradient).applyopacitygradient(shape.applyopacity))
 * 
 * gradient.mask(shape).mask(opacity)
 * 
 * @author micycle1
 *
 */
public final class PeasyGradients {

	static final int HORIZONTAL = 0;
	static final int VERTICAL = 1;
	boolean debug;
	private final PApplet p;
	PImage imageMask;
	PShape shapeMask;
	int colorMode = PConstants.RGB;

	public PeasyGradients(PApplet p) {
		this.p = p;
	}

	/**
	 * Mask will apply to the next gradient only.
	 * 
	 * @param mask
	 */
	public void setMask(PImage mask) {

	}

	/**
	 * 
	 * @param dimensions
	 * @param centre
	 * @param angle
	 * @param smoothing  coefficient to lerp from centrepoint to edges (that
	 *                   intersect with angle)
	 * @param palette
	 * @return
	 */
	public PImage linearGradient(PVector dimensions, PVector centre, float angle, float smoothing, int[] palette) {

		if (!(dimensions.x >= 0 && dimensions.y >= 0)) {
			System.err.println("Gradient dimensions must be greater than 0.");
			return null;
		}

		PGraphics g = p.createGraphics((int) dimensions.x, (int) dimensions.y);

		float m = (float) Math.tan(angle);

		float c = centre.y - (m * centre.x); // y = mx+c >> y-mx = c

		// find points of intersection with PImage boundary
		// solves 2 of 4 possible points
		// | x = c
		//

		float y1 = c; // x = 0
//		float y1x = 

		float y2 = (dimensions.x - c) / m;

		float x1 = -c / m; // y=0
		float x2 = (dimensions.y - c) / m;

		g.beginDraw();
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
		g.endDraw();
		g.updatePixels();
		return g.get();
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
			p.stroke(gradient.eval(PApplet.abs(PApplet.sin(t + p.frameCount * 0.02f))));
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

	public static boolean withinRegion(PVector point, PVector UL, PVector BR) {
		return (point.x >= UL.x && point.y >= UL.y) && (point.x <= BR.x && point.y <= BR.y) // SE
				|| (point.x >= BR.x && point.y >= BR.y) && (point.x <= UL.x && point.y <= UL.y) // NW
				|| (point.x <= UL.x && point.x >= BR.x) && (point.y >= UL.y && point.y <= BR.y) // SW
				|| (point.x <= BR.x && point.x >= UL.x) && (point.y >= BR.y && point.y <= UL.y); // NE
	}

}
