package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import java.lang.Math;

import net.jafama.FastMath;
import peasyGradients.gradient.Gradient;
import peasyGradients.gradient.Palette;

import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;

/**
 * https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2
 * http://devmag.org.za/2012/07/29/how-to-choose-colours-procedurally-algorithms/
 * https://github.com/LFBFerreira/BetterGradients/tree/master/src/luis/ferreira/libraries/color
 * ?? TODO TODO: render with low res LAB, then upscale pixels (interpolate rgb).
 * TODO loop unrolling
 * 
 * https://www.filterforge.com/wiki/index.php/Gradients_101#Gradient_Types
 * 
 * @author micycle1
 *
 */
public class Test extends PApplet {

	/**
	 * Mask, static gradient Mask, dynamic gradient gradient stops render into
	 * thread
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		Test.main(Test.class);
	}

	int WIDTH = 500;
	int HEIGHT = 500;

	int[] palette = { 0xfffc0398, 0xffff7f00 }; // 0xff007fff
	int[] bw = { color(255), 0, 0 };
	PVector mousePos = new PVector(0, 0);
	public PVector midPoint; // change to change midpoitn of linear grad
	final static float INV_TWO_PI = 1 / PConstants.TWO_PI;
	public static PGraphics gradient;
	PImage radialOpacity;
	int nthPixel = 1;
	long t = 0;
	int hue = 255;
	boolean temp = true;

	PeasyGradients pGradients;

	public static Gradient gradient1, gradient2, gradient3;

	public static PGraphics gradientPG;

	@Override
	public void settings() {
		size(WIDTH, HEIGHT);
//		fullScreen();
//		WIDTH = 1920;
//		HEIGHT = 1080;
//		smooth(4);
	}

	@Override
	public void setup() {
		
		gradient = createGraphics(600, 600);
		
		frameRate(60);
		midPoint = new PVector(WIDTH / 2, HEIGHT / 2);
//		gradient1 = Gradient.randomGradient(5);
//		gradient2 = new Gradient();
//		gradient2 = new Gradient(color(255, 0, 0), color(0, 255, 0), color(0, 0, 255));

//		gradient3 = new Gradient(color(hue, 0, 0), color(0, hue, 0), color(0, 0, hue));
//		gradient3 = new Gradient(Palette.randomColors(3));

//		ColorStop c1 = new ColorStop(color(hue, 0, 0), 0f);
//		ColorStop c2 = new ColorStop(color(0, hue, 0), 0.33f);
//		ColorStop c4 = new ColorStop(color(0, 0, hue), 0.66f);
//		ColorStop c5 = new ColorStop(color(0, 0,255), 1);

//		gradient3 = new Gradient(color(187, 255, 51), color(122, 56, 190, 254));
		gradient3 = new Gradient(color(9, 101, 133), color(071, 244, 218), color(11, 167, 228),color(238, 85, 23),color(112, 12, 25));
//		gradient3 = new Gradient(color(255, 255, 51), color(0, 255, 255));
//		gradient3 = new Gradient(c1,c5);
//		gradient3 = new Gradient(color(0), color(255));
		
//	    0.0       [009, 101, 133, 254]     -32938619   [9.0, 11.06, 23.85] 
//	    	    0.25      [071, 244, 218, 255]     -12061478   [47.6, 71.1, 77.55] 
//	    	    0.5       [011, 167, 228, 255]     -16013340   [27.96, 33.31, 78.36]
//	    	    0.75      [238, 085, 023, 254]     -17935081   [38.66, 24.74, 3.55]
//	    	    1.0       [112, 012, 025, 255]     -9434087    [6.99, 3.78, 1.28]  
		
		print(gradient3.toString() + "\n");
//		gradient3.primeAnimation();
		print(gradient3.toString() + "\n");

		pGradients = new PeasyGradients(this);

		textSize(14);

		FastPow.init(11); // TODO call statically in class, lower numbers are better due to cache hits
							// JAB+ITP_fast requireds high value

		pGradients.renderIntoPApplet();
		
		
//		pGradients.setRenderTarget(gradient);
//		pGradients.renderIntoPApplet(0,0, 500, 500);
	}

	float pAngle = 0;

	@Override
	public void draw() {
//		background(0);
//		background(0);
//		background(255,0,0);
		fill(0);
//		rect(0, 0, 60, 50);
		mousePos.set(mouseX, mouseY);
//		PImage gradientImage = pGradients.linearGradient(new PVector(WIDTH, HEIGHT), midPoint, gradient1);
//		image(gradientImage, 0, 0);

//		int w = width;
//		int h = height;
//
//		PGraphics mask = createGraphics(w, h);
//		mask.smooth();// this really does nothing, i wish it did
//		mask.beginDraw();
//
//		mask.background(0);// background color to target
//		mask.fill(255); // keep
//		mask.ellipse(w / 2, h / 2, w, h);
//		mask.endDraw();
//
//		mask.loadPixels();
//		pGradients.posterise(frameCount*1000);
//		gradient3.mutateColour(2);
		float angle = (float) Functions.angleBetween(midPoint, mouseX, mouseY);
		

		float lerpAngle = Functions.lerpAngle(pAngle, angle, 0.02f);
//		angle = PI/4;
		PImage z;
//		System.out.println(angle);

//		pGradients.posterise(1+round(frameCount*0.1f));
//		gradient3 = new Gradient(color(187, 255, 51), color(122, 56, 190, 255- frameCount*0.1f));
//		gradient3.animate(0.003f);
//		

//		z = pGradients.linearGradient(gradient3, midPoint, frameCount * 0.01f, noise(frameCount * 0.01f) * 2.5f);
//		z = pGradients.linearGradient(gradient3, midPoint, midPoint.mult(noise(frameCount * 0.1f)), new PVector(mouseX, mouseY));
//		z = pGradients.linearGradient(gradient3,midPoint, angle, map(mouseX, 0, width, 0, 2));

//		z = pGradients.conicGradient(gradient3, midPoint, 0, 1);
//		z = pGradients.radialGradient(gradient3, midPoint, 1f);


//		gradient.beginDraw();
//		pGradients.conicGradient(gradient3, midPoint, angle);
//		pGradients.linearGradient(gradient3, 0);
//		pGradients.coneGradient(gradient3, mousePos, angle);
//		pGradients.diamondGradient(gradient3, midPoint, 0, 0.5f);
		pGradients.crossGradient(gradient3, midPoint, 0, 0.5f);
//		pGradients.spiralGradient(gradient3, midPoint, angle, 2);
//		pGradients.polygonGradient(gradient3, midPoint, angle, 1.9f, 5);
//		pGradients.noiseGradient(gradient3, midPoint, angle, 1);
//		pGradients.hourglassGradient(gradient3, midPoint, angle, 1);
//		pGradients.radialGradient(gradient3, midPoint, mouseX*0.01f + 0.0000001f);
//		gradient.endDraw();
//		image(gradient.get(),0,0);

//		z = pGradients.conicGradientSmooth(gradient3, midPoint, frameCount * 0.01f);
//		z = pGradients.conicGradientSmooth(gradient3, midPoint, frameCount * 0.01f, 1);
//		image(z, 0, 0);
//		} else {
//		quadCornerGradient();
//		image(gradient, 0, 0);
//		}

//		strokeWeight(55);
//		pGradients.drawBezierCurveWithGradient(new PVector(50, 50), new PVector(450, 600), gradient1, 100, -1, 3);
//		surface.setTitle(String.valueOf(angle));
//		if (angle % (PI) < PI / 4 || angle % (PI) > PI*3 /4) {
//			rect(50, 50, 50, 50); // horiontal gradient 
//		}
//		else {
//			// vertical gradient
//		}

//		fill(255);

//		text(String.valueOf(Math.round(1000.0 / (System.currentTimeMillis() - t))), 10, 15);
//		t = System.currentTimeMillis();
//		text(frameRate, 10, 30);
//		text(angle, 10, 55);
		pAngle = lerpAngle;
	}

	int[] interpolateBilinear(int w, int h, int[] corners) {
		int[] arr = new int[w * h];
		for (int x = 0; x < w; x++) {
			float xinc = (float) x / w;
			int t = lerpColor(corners[0], corners[2], xinc);
			int b = lerpColor(corners[1], corners[3], xinc);
			for (int y = 0; y < h; y++) {
				float yinc = (float) y / h;
				int m = lerpColor(t, b, yinc);
				arr[x + y * w] = m;
			}
		}
		return arr;
	}

	// https://discourse.processing.org/t/per-vertex-color-gradient-fill/9679/7

	/**
	 * For each pixel, calculate weights using distance betweewn gradient control
	 * points.
	 * 
	 * Color Blend: Adjusts the area affected by each color point. at 1.0, all four
	 * colors are blended across the entire frame, resulting in the frame being
	 * colored with an average of all four color values. At 25.0, each color extends
	 * from its center point, half of the distance to the nearest point in any
	 * direction.
	 */
	void quadCornerGradient() {
		gradient.beginDraw();
		gradient.loadPixels();
//		linearGradient(gradient2);
		gradient.updatePixels();
		final int[] g1 = gradient.get().pixels.clone();
		gradient1 = gradient3;
		gradient.loadPixels();
//		linearGradientTemp(gradient3);
		gradient.updatePixels();
		final int[] g2 = gradient.get().pixels.clone();
		loadPixels();
		for (int x = 0; x < width - 1; x++) { // -1 edge buffer
			for (int y = 0; y < height - 1; y++) {
				float[] outX = new float[4], outY = new float[4], out = new float[4];
				float[] col = Functions.decomposeclr(g1[x + width * y]);
				float[] col2 = Functions.decomposeclr(g2[x + width * y]);
				Functions.interpolateLinear(col, col2, y / (float) height, outY);
				Functions.interpolateLinear(col, col2, x / (float) width, outX);
//				peasyGradients.colourSpaces.RGB.interpolate(outX, outY, y / (float) height, out);
				pixels[x + width * y] = Functions.composeclr(outX);
			}
		}
		updatePixels();
		gradient.endDraw();

		// TODO or n-corner gradient?
		// total distance from corners = w√2 + h√2
	}

	/**
	 * Similar to quad corner, but extends to n colours. A gradient where colours
	 * are distributed around the edges of canvas and interpolated to fill inwardly.
	 */
	public void edgeGradient(float offset) {
		// TODO offset 0->1
		// 2*(width+height) = max
//		if max*colorStopPos in Range(0, width) || (width, width+height) || (width+height, w*width+height):
//		x = 0 || y = height, etc
	}

	@Override
	public void keyPressed(KeyEvent event) {
		if (event.getKeyCode() == RIGHT) {
			return;
		}
		if (event.getKeyCode() == LEFT) {
			return;
		}
		if (event.getKeyCode() == UP) {
			gradient3.nextColSpace();
			surface.setTitle(String.valueOf(nthPixel) + " " + gradient3.colourSpace);
			return;
		}
		if (event.getKeyCode() == DOWN) {
			gradient3.prevColSpace();
			surface.setTitle(String.valueOf(nthPixel) + " " + gradient3.colourSpace);
			return;
		}

		if (event.getKeyCode() == BACKSPACE) {
			hue = 255;
			gradient3 = new Gradient(Palette.randomRandomColors(Functions.randomInt(3, 5)));
//			gradient3.primeAnimation();
			println(gradient3.toString());

		}

	}

	@Override
	public void mouseClicked(MouseEvent event) {
		if (mouseButton == LEFT) {
			gradient3.nextInterpolationMode();
		} else {
			gradient3.prevInterpolationMode();
		}

		surface.setTitle(gradient3.interpolationMode.toString() + " " + gradient3.colourSpace);
	}

	@Override
	public void mouseWheel() {
		hue--;
		hue = max(0, hue);
		hue = min(255, hue);
		gradient3.setColorStopCol(0, color(hue, 0, 0));
		gradient3.setColorStopCol(1, color(0, hue, 0));
		gradient3.setColorStopCol(2, color(0, 0, hue));
//		gradient3 = new Gradient(color(hue, 0, 0), color(0, hue, 0), color(0, 0, hue));
	}

}
