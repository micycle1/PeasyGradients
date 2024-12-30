<h1 align="center">
  <a href="https://github.com/micycle1/PeasyGradients">
  <img src="resources/logo/Banner-2.png" alt="PeasyGradients"/></a>
</h1>

[![](https://jitpack.io/v/micycle1/PeasyGradients.svg)](https://jitpack.io/#micycle1/PeasyGradients)
 [![Lines of Code](https://sonarcloud.io/api/project_badges/measure?project=micycle1_PeasyGradients&metric=ncloc)](https://sonarcloud.io/summary/new_code?id=micycle1_PeasyGradients)


# PeasyGradients
A Processing library for creating and rendering color gradients.

### Key Features
* 11 Types of 2D Gradients
* 13 Color Spaces
* 14 Color Interpolation Functions
* Fast!

_This `README` provides an overview of the library — what it's capable of and how to use it. Access examples (Processing sketches) [here](https://github.com/micycle1/PeasyGradients/tree/master/examples); access documentation (online Javadocs) [here](https://micycle1.github.io/PeasyGradients/)._

## Core Concepts

PeasyGradients operates on two distinct levels:

1. **Gradient Definition (1D)**: A mathematical function that maps positions along a single axis [0,1] to colors. Think of it as a recipe for color transitions.
   - Defined by color stops (color + position pairs)
   - Specifies interpolation method (how colors blend)
   - Sets color space for calculations

2. **Gradient Rendering (2D)**: The process of sampling the 1D gradient definition to create a 2D visualization
   - Different sampling patterns (linear, radial, conic)
   - Controls for position, angle, and spread
   - Direct output to Processing sketches

## Quick Start

```java
// 1. Define your 1D gradient
Gradient blackToWhite = new Gradient(color(0), color(255));
// This creates the mathematical definition: position 0.0 = black, position 1.0 = white

// 2. Create the renderer
PeasyGradients renderer = new PeasyGradients(this);

// 3. Render the gradient using different sampling patterns
// renders into the Processing sketch by default
renderer.linearGradient(blackToWhite, 0);  // as a horizontal fill
renderer.radialGradient(blackToWhite, width/2, height/2, 200);  // As a circular gradient
```

See the *Gradients* section below for a showcase of each (2D) gradient type.

## Installation

<details><summary>Processing IDE — Quick</summary>
<p>

Download the latest *peasyGradients.jar* from [releases](https://github.com/micycle1/PeasyGradients/releases/) and simply drag-and-drop it onto the [Processing IDE](https://processing.org/reference/environment/).
</p>
</details>

<details><summary>Processing IDE — Permanently</summary>
<p>

Download the latest *peasyGradients.jar* from [releases](https://github.com/micycle1/PeasyGradients/releases/) and save it to `Documents\Processing\libraries\peasyGradients\library`.

Result: `Documents\Processing\libraries\peasyGradients\library\peasyGradients.jar`.

(Note the *.jar* and the folder **must** be called `peasyGradients` — rename the .jar if this is not the case).
</p>
</details>

<details><summary>Maven/Gradle</summary>
<p>

PeasyGradients is hosted as an artifact for use in Maven or Gradle projects via [Jitpack](https://jitpack.io/#micycle1/PeasyGradients) — follow the instructions there (very easy). 
</p>
</details>

## Gradients

PeasyGradients provides methods to render 11 types of 2D gradients.

Each gradient type is shown below. The screenshots are taken using the `LUV` color space with `SMOOTH_STEP` interpolation, and rotation set to 0 where applicable.

| **Linear**![](resources/gradient_type_examples/linear.png) | **Radial**![](resources/gradient_type_examples/radial.png) | **Conic**![](resources/gradient_type_examples/conic.png) |
|:---:|:---:|:---:|
| **Spiral**![](resources/gradient_type_examples/spiral.png) | **Diamond**![](resources/gradient_type_examples/diamond.png) | **Cross**![](resources/gradient_type_examples/cross.png) |
| **Polygon (n-sided)**![](resources/gradient_type_examples/polygon(6).png) | **Hourglass**![](resources/gradient_type_examples/hourglass.png) | **Spotlight**![](resources/gradient_type_examples/spotlight.png) |
| **Fractal Noise**![](resources/gradient_type_examples/noise_fractal.png) | **Noise**![](resources/gradient_type_examples/noise.png) |  

_Zoom and rotation can be adjusted for most gradient types; certain gradient types offer additional parameters — for example, the polygon gradient requires a parameter specifying the number of polygon sides._

_For many of the gradient types, there are multiple methods available to call, offering varying degrees of customisation of the gradient's spectrum shape._


## Interpolation: Easing Functions
In the parts of a `Gradient` between color stops, colors are composed via interpolating the neighbouring two color stops. Easing functions affect how these two adjacent stops contribute to the color of each point between them.

For example, with *linear* interpolation, a point in a `Gradient` which is midway between 2 color stops is composed of 50% of the first color and 50% of the second color — there is a linear relationship between its position and the weighting of color it receives from each color stop. Other easing functions are non-linear (for example a point closer to one color stop may in some cases receive more color from the second color stop) which can result in more interesting gradients.
 
Certain easing functions suit some gradient types better than others — for example, the `BOUNCE` function works well with polygon gradients but rather more poorly with linear gradients. Therefore, and as with color spaces, experimentation with different interpolation functions is encouraged.

Set the interpolation (easing) function for a given `Gradient` with `.setInterpolationMode()`, like so:

```
myGradient.setInterpolationMode(Interpolation.SMOOTH_STEP);
```

See for a comparison of all available easing functions modes in expandable section below:

| **Linear**![](resources/interpolation_examples/linear.png ) | **Identity**![](resources/interpolation_examples/identity.png) | **Smooth Step**![](resources/interpolation_examples/smooth_step.png ) | **Smoother Step**![](resources/interpolation_examples/smoother_step.png) |
|:---:|:---:|:---:|:---:|
| **Exponential**![](resources/interpolation_examples/exponential.png ) | **Cubic**![](resources/interpolation_examples/cubic.png) | **Circular**![](resources/interpolation_examples/circular.png ) | **Bounce**![](resources/interpolation_examples/bounce.png) |
| **Sine**![](resources/interpolation_examples/sine.png ) | **Parabola**![](resources/interpolation_examples/parabola.png) | **Gain 1**![](resources/interpolation_examples/gain1.png ) | **Gain 2**![](resources/interpolation_examples/gain2.png) |
| **Exponential Impulse**![](resources/interpolation_examples/expimpulse.png ) | **Heartbeat**![](resources/interpolation_examples/heartbeat.png ) | | |

<details><summary style="font-size:135%; color:blue">💥See Interpolation Comparison...</summary>

| **Linear**![](resources/interpolation_examples/linear.png ) | **Identity**![](resources/interpolation_examples/identity.png) |
|:---:|:--:|
| **Smooth Step**![](resources/interpolation_examples/smooth_step.png ) | **Smoother Step**![](resources/interpolation_examples/smoother_step.png) |
| **Exponential**![](resources/interpolation_examples/exponential.png ) | **Cubic**![](resources/interpolation_examples/cubic.png) |
| **Circular**![](resources/interpolation_examples/circular.png ) | **Bounce**![](resources/interpolation_examples/bounce.png) |
| **Sine**![](resources/interpolation_examples/sine.png ) | **Parabola**![](resources/interpolation_examples/parabola.png) |
| **Gain 1**![](resources/interpolation_examples/gain1.png ) | **Gain 2**![](resources/interpolation_examples/gain2.png) |
| **Exponential Impulse**![](resources/interpolation_examples/expimpulse.png ) | **Heartbeat**![](resources/interpolation_examples/heartbeat.png )

</details>

## Color Spaces

Color spaces define how the color value at each color stop is represented.

Remember that a 1D `Gradient` consists of only a few defined color stops; all other colors in a `Gradient`'s spectrum are **composed** by **interpolating** between any two adjacent color stops. Representing color stops differently (in different color spaces) affects the results of interpolation, and this can have a noticeable effect on the overall spectrum of a gradient (so experimentation with different color spaces is encouraged). A rule of thumb: avoid the `RGB`, `RYB` and `HSB` color spaces as they don't interpolate luminosity well.


 Set the color space for a given `Gradient` with `.setColorSpace()`, like so:
 
 ```
 myGradient.setColorSpace(ColorSpace.RGB);
 ```

 PeasyGradients supports many different color spaces — all possible color spaces are accessible via `ColorSpace.class` and examples of each are shown in the expandable section below:

<details><summary style="font-size:135%; color:blue">💥See Color Space Comparison...</summary>

*Note that with the chosen gradient, the spectrum differences aren't too apparent between many of the colorspaces in the images below. Other gradients (i.e. using different colors) may exhibit more substantial differences between the different colorspaces.*

| **RGB**![](resources/color_space_examples/RGB.png ) | **RYB**![](resources/color_space_examples/RYB.png) |
|:---:|:---:|
| **HSB**![](resources/color_space_examples/HSB.png ) | **XYZ (CIE 1931)**![](resources/color_space_examples/XYZ.png) |
| **LAB (CIE L\*a\*b\*)**![](resources/color_space_examples/LAB.png ) | **HLAB (Hunter LAB)**![](resources/color_space_examples/HLAB.png) |
| **DLAB**![](resources/color_space_examples/DIN99.png ) | **ITP (ICtCp)**![](resources/color_space_examples/ITP.png) |
| **LUV (CIE 1976 L\*, u\*, v\*)**![](resources/color_space_examples/DIN99.png ) | **JAB (JzAzBz)**![](resources/color_space_examples/JAB.png) |
| **XYB**![](resources/color_space_examples/XYZ.png) | **IPT**![](resources/color_space_examples/IPT.png)

</details>

## Animating Gradients

### Animating Color Offset

The position of all color stops within a `Gradient` can be offset using `.setOffset(amount)`.

Furthermore, the `.animate(amount)` method changes this offset by the given `amount` each time it is called; with this you can create an gradient spectrum scrolling effect by calling `.animate(0.01f)` each frame for example.

### Priming a Gradient for Animation

Naively animating a gradient may lead to an ugly and undesirable seam in the gradient where the first and last color stops (at positions 0.00 and 1.00 respectively) bump right up against each other, like in the linear gradient below:

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/with_seam.gif" alt="PeasyGradients" /></a><br></p>

To avoid this, call `.primeAnimation()` on a `Gradient` (once) before animating it. This pushes a copy of the first color stop of the `Gradient` to its end (scaling all other color stops accordingly), to ensure a **seamless gradient spectrum**, regardless of offset.

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/seamless.gif" alt="PeasyGradients"/></a><br></p>

Calling `.primeAnimation()` on a `Gradient` before rendering it as a **conic** or **spiral** gradient has the added benefit of smoothing the transition between the first and last colors, regardless of whether you wish to animate the gradient, as below:

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/other_examples/all_smooth.png" alt="PeasyGradients" width="500" height="500"/></a><br></p>

### Animating Color

The **color** of `Colorstop`s within a `Gradient` can be modified after the `Gradient` has been instantiated.

Set the color of a specific color stop with `.setStopColor()`, shown below:

```
angle += PI / 180; // increment angle

colorMode(HSB, 360, 100, 100); // switch Processing to HSB mode to set hue more easily

myGradient.setStopColor(0, color(frameCount % 360, 100, 100)); // set color stop at index 0
myGradient.setStopColor(1, color((frameCount + 180) % 360, 100, 100)); // set color stop at index 1

peasyGradients.linearGradient(myGradient, angle); // render gradient
```

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/color_animate.gif" alt="PeasyGradients"/></a><br></p>

### Animating Color Stop Position

The **position** of `Colorstop`s within a `Gradient` can also be modified after the `Gradient` has been instantiated.

Set the position (0...1.0) of a specific color stop with `.setStopPosition()`, shown below:

```
myGradient.setStopPosition(1, map(mouseX, 0, width, 0, 1)); // set position of middle color (index = 1)

peasyGradients.linearGradient(myGradient, 0);
```

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/color_stop_pos_animate.gif" alt="PeasyGradients"/></a><br></p>

## Other Stuff

### Posterisation

Use posterisation to define the maximum number of colors the PeasyGradient renderer uses to render `Gradients`. Smaller numbers are more restrictive and increase the color banding effect — there may be times when this artistic effect is desirable.

```
peasyGradients.posterise(10); // renderer will now render gradients with 10 colors at most 
```

| **No Posterisation (default)**![](resources/posterise_examples/posterise_none.png) | **Posterisation = 10**![](resources/posterise_examples/posterise_10.png) |  **Posterisation = 25**![](resources/posterise_examples/posterise_25.png) |
|:---:|:---:|:---:|

Use `.clearPosterisation()` to clear any posterisation setting and render gradients with in full color.


### Generating Random Gradients

The `Palette` class provides some helper methods for generating random `Gradient` color palettes.

Note this palette generation is not entirely random -- the class produces gradients spectrums that are more aesthetic than what a truly random method would generate.

```
randomGradient = new Gradient(Palette.complementary()); // two random colors that are on opposite sides of the color wheel
randomGradient = new Gradient(Palette.triadic()); // 3 random colors that are evenly spaced on the color wheel
randomGradient = new Gradient(Palette.tetradic()); // 4 random colors that are evenly spaced on the color wheel
randomGradient = new Gradient(Palette.randomcolors(7)); // N random colors with hue distributed according to the golden ratio
randomGradient = new Gradient(Palette.randomRandomcolors(8)); // N random colors also distributed randomly
```

### Masking

Strictly speaking, this functionality isn't defined by PeasyGradients (rather, it's defined by Processing itself). But I think it's worthwhile to show how masking can be used in conjunction with PeasyGradients. An example which uses text ("P") to mask a spiral gradient is provided below:

<details><summary>💥See Code Example...</summary>

```
void setup() {

  gradientLayer = createGraphics(WIDTH, HEIGHT); // PGraphics layer containing gradient
  textLayer = createGraphics(WIDTH, HEIGHT); //  PGraphics layer containing text (used as alpha mask)

  peasyGradients = new PeasyGradients(this);

  textLayer.beginDraw();
  textLayer.fill(255); // set to white to set pixels to keep during alpha mask
  textLayer.textSize(150);
  textLayer.textAlign(CENTER, CENTER);
  textLayer.text("N", width / 2, height / 2 - textLayer.textDescent() / 2); // draw text in image center
  textLayer.endDraw();
}

void draw() {

  PVector mousePos = new PVector(mouseX, mouseY);

  background(0);
  
  // render gradient into PGraphics layer
  peasyGradients.setRenderTarget(gradientLayer);
  peasyGradients.spiralGradient(gradient, mousePos, map(mouseY, 0, height, 0.5f, PI * 6), 2); // map spiral rotation to mouse position
  gradientLayer.mask(textLayer.get()); // mask gradient layer using text (keep only the text)

  image(gradientLayer, 0, 0); // draw (now masked) gradient image
}
```

</details>

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/masking_example.gif" alt="PeasyGradients"/></a><br></p>

## **Library Optimisation**
PeasyGradients targets the **CPU** (as opposed to the GPU) as to not be dependent on `OPENGL` libraries. To this end, there have been many internal optimisations to make the library suitable for dynamic animation and interaction rather than just static rendering. Care was taken to optimise color interpolation and gradient rasterization, and finally multithreading was added to naively parallelise the workload. Therefore, rendering (most) gradients at 60fps at high resolutions (1080p+) is more than achievable on modern processors.

## **Improvements**

* Implement [four-corner](https://graphicdesign.stackexchange.com/questions/19477/4-colors-corners-gradient-with-illustrator-or-photoshop) gradients (or a more general form which distributes *N* colors, placing them around the edge and interpolating towards the middle).
* Implement [this](https://www.filterforge.com/wiki/index.php/Spiral_Gradient) type of spiral gradient.
* Allow easing functions to be passed in to render methods to affect the curve of whole spectrum not just the curve between each successive pair of color stops.
* Add preset color scales (rainbow, warm, cool, etc.) to `Palettes`.
* Add a gradient [contour plot](https://observablehq.com/@rreusser/locally-scaled-domain-coloring-part-1-contour-plots) overlay feature.
* Implement the CAM16/CAM16‐UCS Color Appearance Model as a colorspace.
