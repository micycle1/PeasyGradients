package peasyGradients;

import processing.core.PApplet;
import processing.core.PConstants;
import processing.core.PGraphics;
import processing.core.PImage;
import processing.core.PVector;
import processing.event.KeyEvent;
import processing.event.MouseEvent;

import peasyGradients.gradient.Gradient;
import peasyGradients.gradient.Palette;

import peasyGradients.utilities.FastPow;
import peasyGradients.utilities.Functions;
import peasyGradients.utilities.FastNoiseLite.FractalType;
import peasyGradients.utilities.FastNoiseLite.NoiseType;

//import com.jogamp.newt.opengl.GLWindow;

/**
 * https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2
 * http://devmag.org.za/2012/07/29/how-to-choose-colors-procedurally-algorithms/
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

	int WIDTH = 1800;
	int HEIGHT = 1200;

	int[] palette = { 0xfffc0398, 0xffff7f00 }; // 0xff007fff
	int[] bw = { color(255), 0, 0 };
	PVector mousePos = new PVector(0, 0);
	public PVector midPoint; // change to change midpoitn of linear grad
	final static float INV_TWO_PI = 1 / PConstants.TWO_PI;
	public static PGraphics gradientLayer;
	PImage radialOpacity;
	int nthPixel = 1;
	long t = 0;
	int hue = 255;
	boolean temp = true;

	PeasyGradients pGradients;

	public static Gradient gradient1, gradient2, gradient3;

	public static PGraphics gradientPG;

	PGraphics textLayer;

	@Override
	public void settings() {
//		
//		fullScreen();
//		WIDTH = 800;
//		HEIGHT = 800;
		size(WIDTH, HEIGHT);
//		smooth(4);
	}

	@Override
	public void setup() {

		gradientLayer = createGraphics(WIDTH, HEIGHT);
		textLayer = createGraphics(WIDTH, HEIGHT);

		frameRate(600);
		midPoint = new PVector(WIDTH / 2, HEIGHT / 2);
//		gradient1 = Gradient.randomGradient(5);
//		gradient2 = new Gradient();
//		gradient2 = new Gradient(color(255, 0, 0), color(0, 255, 0), color(0, 0, 255));

//		gradient3 = new Gradient(color(hue, 0, 0), color(0, hue, 0), color(0, 0, hue));
//		gradient3 = new Gradient(Palette.randomcolors(3));

//		colorStop c1 = new colorStop(color(hue, 0, 0), 0f);
//		colorStop c2 = new colorStop(color(0, hue, 0), 0.33f);
//		colorStop c4 = new colorStop(color(0, 0, hue), 0.66f);
//		colorStop c5 = new colorStop(color(0, 0,255), 1);

		gradient3 = new Gradient(color(255), color(0));
		gradient3 = new Gradient(color(9, 101, 133), color(071, 244, 218), color(11, 167, 228), color(238, 85, 23), color(112, 12, 25)); // for screenshots
//		gradient3 = new Gradient(color(255, 0, 0), color(0, 255,0), color(0, 0,255));
//		gradient3 = new Gradient(c1,c5);
//		gradient3 = new Gradient(color(0), color(20, 150, 20) , color(0, 50, 100));
//		gradient3 = new Gradient(color(201, 136, 179), color(061, 243, 205), color(110, 011, 201), color(043, 18, 073), color(224, 8, 94),
//				color(228, 247, 90), color(201, 136, 179));
//		gradient3 = new Gradient(color(255, 255, 0), color(0, 255, 255));

		print(gradient3.toString() + "\n");
//		gradient3.primeAnimation();
//		gradient1.primeAnimation();
		print(gradient3.toString() + "\n");

		pGradients = new PeasyGradients(this);
//		pGradients.renderIntoSketch(200, 200, 600, 600);

		textSize(14);



//		pGradients.renderIntoSketch();
//		pGradients.posterise(25);

//		pGradients.setRenderTarget(gradientLayer);
//		pGradients.renderIntoPApplet(0,0, 500, 500);
		textLayer.beginDraw();
		textLayer.fill(255);
		textLayer.textSize(350);
		textLayer.textAlign(CENTER, CENTER);
		textLayer.text("P", width / 2, height / 2 - textLayer.textDescent() / 2);
		textLayer.endDraw();

//		pGradients.renderIntoSketch();
		
//		noLoop();
	}

//	GLWindow window;

	float pAngle = 0;
	float angle = 0;

	@Override
	public void draw() {
//		background(0);
//		background(255);
//		background(255,0,0);
		fill(0);
//		rect(0, 0, 60, 50);
//		mousePos.set(mouseX, mouseY);
//		PImage gradientImage = pGradients.linearGradient(new PVector(WIDTH, HEIGHT), midPoint, gradient1);
//		image(gradientImage, 0, 0);

//		surface.setTitle(String.valueOf(frameCount % 360));

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
//		gradient3.mutatecolor(2);
//		float angle = (float) Functions.angleBetween(midPoint, mouseX, mouseY);
//		angle = 0;

		float lerpAngle = Functions.lerpAngle(pAngle, angle, 0.02f);

//		angle = -PI/4;
//		angle += (2 * PI) / 360;
		PImage z;
//		System.out.println(angle);

//		pGradients.posterise(1+round(frameCount*0.1f));
//		gradient3 = new Gradient(color(187, 255, 51), color(122, 56, 190, 255- frameCount*0.1f));
//		gradient3.animate(0.003f);
//		

//		z = pGradients.linearGradient(gradient3, midPoint, frameCount * 0.01f, noise(frameCount * 0.01f) * 2.5f);
//		z = pGradients.linearGradient(gradient3, midPoint, midPoint.mult(noise(frameCount * 0.1f)), new PVector(mouseX, mouseY));
//		pGradients.linearGradient(gradient3,midPoint, angle, map(mouseX, 0, width, 0, 2));

//		z = pGradients.conicGradient(gradient3, midPoint, 0, 1);
//		z = pGradients.radialGradient(gradient3, midPoint, 1f);
//		angle += PI / 180;

//		gradient3.setColorStopPosition(1, map(mouseX, 0, width, 0, 1));

		pGradients.linearGradient(gradient3, 0);

//		gradient.beginDraw();
//		pGradients.conicGradient(gradient3, midPoint,angle);
//		pGradients.radialGradient(gradient3, midPoint,1);

//		pGradients.coneGradient(gradient3, mousePos, angle);
//		new PVector(width/2, 0)
//		pGradients.diamondGradient(gradient3, midPoint, 0, 0.75f);
//		pGradients.crossGradient(gradient3, midPoint, 0, 1f);

//		pGradients.spiralGradient(gradient3, midPoint, 0, map(frameCount * 3 % width, 0, width, 0, 10),
//				map(frameCount * 3 % width, 0, width, 10, 0.1f)); // NOTE cool effect

//		pGradients.spiralGradient(gradient3, midPoint, 0, 2,2);

//		pGradients.posterise(25);
//		pGradients.spiralGradient(gradient3, mousePos, map(mouseY, 0, height, 0.5f, PI * 6), 2, 1);
//		pGradients.setRenderTarget(gradientLayer);
//		pGradients.spiralGradient(gradient3, mousePos, map(mouseY, height, 0, 0.5f, PI * 6), 2);
//		pGradients.spiralGradient(gradient3, mousePos, 0, 20);
//		gradientLayer.mask(textLayer.get());
//		image(textLayer, 0, 0);
//		image(gradientLayer, 0, 0);
//		pGradients.polygonGradient(gradient3, midPoint, angle, 1f, 3);
		
//			pGradients.simplexNoiseGradient(gradient3, midPoint, 0, 1);
//			pGradients.noiseGradient(gradient3, midPoint, 0, 1);
//			pGradients.fractalNoiseGradient(gradient3, midPoint, 0, 1, NoiseType.OpenSimplex2, FractalType.Ridged, 3, 1, 2);
//		pGradients.fastNoiseLite.SetFractalPingPongStrength(1);
//		pGradients.setNoiseSeed(100);
//		pGradients.fractalNoiseGradient(gradient3, midPoint, 0, 1, NoiseType.OpenSimplex2, FractalType.PingPong, 3,
//				map(mouseY, 0, height, 0f, 5), map(mouseX, 0, width, 0f, 5));

//		pGradients.spotlightGradient(gradient3, new PVector(width/2, -5), frameCount*0.01f, PI/2);
//		pGradients.spotlightGradient(gradient3, new PVector(width/2, -5), mousePos);

//		pGradients.hourglassGradient(gradient3, midPoint, 0, 1);
//		pGradients.radialGradient(gradient3, midPoint, map(mouseX, 0, width, 0f, 1));
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

		text(String.valueOf(Math.round(1000.0 / (System.currentTimeMillis() - t))), 10, 15);
		t = System.currentTimeMillis();
		text(round(frameRate), 10, 30);
//		text(angle, 10, 55);
		pAngle = lerpAngle;
//		PImage a;
//		ellipse(200, 200, 5, 5);
	}

//	int[] interpolateBilinear(int w, int h, int[] corners) {
//		int[] arr = new int[w * h];
//		for (int x = 0; x < w; x++) {
//			float xinc = (float) x / w;
//			int t = lerpcolor(corners[0], corners[2], xinc);
//			int b = lerpcolor(corners[1], corners[3], xinc);
//			for (int y = 0; y < h; y++) {
//				float yinc = (float) y / h;
//				int m = lerpcolor(t, b, yinc);
//				arr[x + y * w] = m;
//			}
//		}
//		return arr;
//	}

	// https://discourse.processing.org/t/per-vertex-color-gradient-fill/9679/7
	boolean newNoise = true;

	/**
	 * For each pixel, calculate weights using distance betweewn gradient control
	 * points.
	 * 
	 * color Blend: Adjusts the area affected by each color point. at 1.0, all four
	 * colors are blended across the entire frame, resulting in the frame being
	 * colored with an average of all four color values. At 25.0, each color extends
	 * from its center point, half of the distance to the nearest point in any
	 * direction.
	 */
//	void quadCornerGradient() {
//		gradientLayer.beginDraw();
//		gradientLayer.loadPixels();
////		linearGradient(gradient2);
//		gradientLayer.updatePixels();
//		final int[] g1 = gradientLayer.get().pixels.clone();
//		gradient1 = gradient3;
//		gradientLayer.loadPixels();
////		linearGradientTemp(gradient3);
//		gradientLayer.updatePixels();
//		final int[] g2 = gradientLayer.get().pixels.clone();
//		loadPixels();
//		for (int x = 0; x < width - 1; x++) { // -1 edge buffer
//			for (int y = 0; y < height - 1; y++) {
//				float[] outX = new float[4], outY = new float[4], out = new float[4];
//				float[] col = Functions.decomposeclr(g1[x + width * y]);
//				float[] col2 = Functions.decomposeclr(g2[x + width * y]);
//				Functions.interpolateLinear(col, col2, y / (float) height, outY);
//				Functions.interpolateLinear(col, col2, x / (float) width, outX);
////				peasyGradients.colorSpaces.RGB.interpolate(outX, outY, y / (float) height, out);
//				pixels[x + width * y] = ColorUtils.composeclr(outX);
//			}
//		}
//		updatePixels();
//		gradientLayer.endDraw();
//
//		// TODO or n-corner gradient?
//		// total distance from corners = w√2 + h√2
//	}

	/**
	 * Similar to quad corner, but extends to n colors. A gradient where colors are
	 * distributed around the edges of canvas and interpolated to fill inwardly.
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
			surface.setTitle(String.valueOf(nthPixel) + " " + gradient3.colorSpace);
			return;
		}
		if (event.getKeyCode() == DOWN) {
			gradient3.prevColSpace();
			surface.setTitle(String.valueOf(nthPixel) + " " + gradient3.colorSpace);
			return;
		}

		if (event.getKeyCode() == BACKSPACE) {
			hue = 255;
			gradient3 = new Gradient(Palette.randomRandomcolors(Functions.randomInt(3, 3)));
//			gradient3.primeAnimation();
			println(gradient3.toString());
//			println(gradient3.toJavaConstructor());

		}

		if (event.getKey() == 'n') {
			newNoise = !newNoise;
		}

	}

	@Override
	public void mouseClicked(MouseEvent event) {
		if (mouseButton == LEFT) {
			gradient3.nextInterpolationMode();
		} else {
			gradient3.prevInterpolationMode();
//			gradient3.setColorSpace(ColorSpaces.DIN99);
		}

		surface.setTitle(gradient3.interpolationMode.toString() + " " + gradient3.colorSpace);
	}

	@Override
	public void mouseWheel() {
		hue--;
		hue = max(0, hue);
		hue = min(255, hue);
		gradient3.setStopColor(0, color(hue, 0, 0));
		gradient3.setStopColor(1, color(0, hue, 0));
		gradient3.setStopColor(2, color(0, 0, hue));
//		gradient3 = new Gradient(color(hue, 0, 0), color(0, hue, 0), color(0, 0, hue));
	}

}
