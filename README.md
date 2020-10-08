<h1 align="center">
  <a href="https://github.com/micycle1/PeasyGradients">
  <img src="resources/logo-small.png" alt="PeasyGradients"/></a><br>
PeasyGradients
<br></br>
</h1>
<p align="center">🚧<em>~~A Work in Progress~~</em>🚧</p>




PeasyGradients is a library for Processing to make drawing colour gradients easy-peasy. This library was inspired by Jeremy Behreandt's [*Color Gradients in Processing*](https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2) but has greatly developed the concepts and functionality presented there.

## Overview

### Key Features:
* ### 10 Gradient Types
* ### 10 Colour Spaces
* ### 13 Colour Interpolation Functions
* ### Fast!

### Creating a 1D gradient

The `PeasyGradients` class renders 1D `Gradients` as 2D spectrums in your Processing sketch. A 1D `Gradient` consists solely of colour stops — these define the colours and the discrete position (percentage) each colour occurs at on the 1D axis.

A simple black-to-white gradient is created as such:

```
Gradient blackToWhite = new Gradient(colour(0), colour(255));
```

Here, `blackToWhite` is a 1D gradient with two equidistant colour stops — black is at *0.00*, and white is at *1.00*.

### Rendering a 2D gradient
Merely instantiating a 1D gradient doesn't draw anything. How should this 1D spectrum be drawn? Do we want to render a black-to-white linear gradient? A radial gradient? Or something else? This is where the *PeasyGradients* class comes in... We pass a `Gradient` (here, the `blackToWhite` 1D gradient) to one of a variety of methods to draw a 2D gradient.

```
PeasyGradients peasyGradients;


void setup() {
    peasyGradients = new PeasyGradients(this);
}

void draw() {
    peasyGradients.linearGradient(blackToWhite, 0); // angle = 0 (horizontal)
}
```

That's it! Now a horizontal black-to-white linear gradient will be drawn in the sketch (by default, the library draws directly into the sketch; you can give it a specific `PGraphics` pane to draw into with the `.setRenderTarget()` method).

See the *Gradients* section below for a showcase of each (2D) gradient type.

## Installation

* To use PeasyGradients in a Java IDE, simply download the most recent .jar from [releases](https://github.com/micycle1/PeasyGradients/releases/) and include it in your project's classpath.

* To use it in the Processing IDE (for it to appear in the contribution manager), download the .jar as above and then see [this](https://github.com/processing/processing/wiki/How-to-Install-a-Contributed-Library).

## Gradients

PeasyGradients provides methods to render 10 types of 2D gradients.

Zoom and rotation can be adjusted for most gradient types; certain gradient types offer additional parameters — for example, the polygon gradient requires a parameter specifying the number of polygon sides.

Each gradient type is shown below. The screenshots are taken using the `LUV` colour space with `SMOOTH_STEP` interpolation, and rotation set to 0 where applicable.

| **Linear**![](resources/gradient_type_examples/linear.png ) | **Radial**![](resources/gradient_type_examples/radial.png) |
|:---:|:---:|
| **Conic**![](resources/gradient_type_examples/conic.png) | **Spiral**![](resources/gradient_type_examples/spiral.png) |
| **Diamond**![](resources/gradient_type_examples/diamond.png) | **Cross**![](resources/gradient_type_examples/cross.png) |
| **Polygon**![](resources/gradient_type_examples/polygon(6).png) | **Hourglass**![](resources/gradient_type_examples/hourglass.png) |
| **Noise**![](resources/gradient_type_examples/noise.png) |


## Colour Spaces

Colour spaces define how the colour value at each colour stop is represented.

Remember that a 1D `Gradient` consists of only a few defined colour stops; all other colours in a `Gradient`'s spectrum are **composed** by **interpolating** between any two adjacent colour stops. Representing colour stops differently (in different colour spaces) affects the results of interpolation, and this can have a noticeable effect on the overall spectrum of a gradient (so experimentation with different colour spaces is encouraged). A rule of thumb: avoid the `RGB`, `RYB` and `HSB` colour spaces as they don't interpolate luminosity well.


 The colour space for a given `Gradient`  is set with `.setColSpace()`, like so:
 
 ```
 myGradient.setColSpace(ColourSpaces.RGB);
 ```

 PeasyGradients supports many different colour spaces — all possible colour spaces are accessible via `ColourSpaces.class` and examples of each are shown below:

<details><summary style="font-size:135%; color:blue">💥See Colour Space Comparison...</summary>

| **RGB**![](resources/colour_space_examples/RGB.png ) | **RYB**![](resources/colour_space_examples/RYB.png) |
|:---:|:---:|
| **HSB**![](resources/colour_space_examples/HSB.png ) | **XYZ (CIE 1931)**![](resources/colour_space_examples/XYZ.png) |
| **LAB (CIE L\*a\*b\*)**![](resources/colour_space_examples/LAB.png ) | **HLAB (Hunter LAB)**![](resources/colour_space_examples/HLAB.png) |
| **DIN99**![](resources/colour_space_examples/DIN99.png ) | **ITP (ICtCp)**![](resources/colour_space_examples/ITP.png) |
| **LUV (CIE 1976 L\*, u\*, v\*)**![](resources/colour_space_examples/DIN99.png ) | **JAB (JzAzBz)**![](resources/colour_space_examples/JAB.png) |

</details>


## Interpolation: Easing Functions
In areas between colour stops, colours are composed via interpolation. Easing functions affect how the colours between two adjacent colour stops are composed. 
affect how colour stops are interpolated to generate colours between them

For example, with *linear* interpolation, a point in a `Gradient` which is midway between 2 colour stops is composed of 50% of the first colour and 50% of the second colour — there is a linear relationship between its position and the weighting of colour it receives from each colour stop. Other easing functions are non-linear (for example a point closer to one colour stop may in some cases receive more colour from the second colour stop) which can result in more interesting gradients.
 
Certain easing functions suit some gradient types better than others — for example, the `BOUNCE` function works well with polygon gradients but rather more poorly with linear gradients. Therefore, as with colour spaces, experimentation with different interpolation functions is encouraged.

```
todo code xample
```

These are the the available interpolation easing functions in PeasyGradients (accessible via the `Interpolation` enum):

<details><summary style="font-size:135%; color:blue">💥See Interpolation Comparison...</summary>

| **Linear**![](resources/interpolation_examples/linear.png ) | **Identity**![](resources/interpolation_examples/identity.png) |
|:---:|:--:|
| **Smooth Step**![](resources/interpolation_examples/smooth_step.png ) | **Smoother Step**![](resources/interpolation_examples/smoother_step.png) |
| **Exponential**![](resources/interpolation_examples/exponential.png ) | **Cubic**![](resources/interpolation_examples/cubic.png) |
| **Circular**![](resources/interpolation_examples/circular.png ) | **Bounce**![](resources/interpolation_examples/bounce.png) |
| **Sine**![](resources/interpolation_examples/sine.png ) | **Parabola**![](resources/interpolation_examples/parabola.png) |
| **Gain 1**![](resources/interpolation_examples/gain1.png ) | **Gain 2**![](resources/interpolation_examples/gain2.png) |
| **Exponential Impulse**![](resources/interpolation_examples/expimpulse.png ) |

</details>

## Animating Gradients

### Animating Colour Stops

The position of all colour stops within a `Gradient` can be offset using `.setOffset(amount)`.

Furthermore, the `.animate(amount)` method changes this offset by the given `amount` each time it is called; with this you can animate a `Gradient`'s colour by calling `.animate(0.01f)` each frame for example.

### Priming a Gradient for Animation

Naively animating a gradient may lead to an ugly and undesirable seam in the gradient where the first and last colour stops (at positions 0.00 and 1.00 respectively) bump right up against each other, like in the linear gradient below:

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/with_seam.gif" alt="PeasyGradients" /></a><br></p>

To avoid this, call `.primeAnimation()` on a `Gradient` (once) before animating it. This pushes a copy of the first colour stop of the `Gradient` to its end (scaling all other colour stops accordingly), to produce a **seamless gradient spectrum**, regardless of offset.

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/animation_examples/seamless.gif" alt="PeasyGradients"/></a><br></p>

Calling `.primeAnimation()` on a `Gradient` before rendering it as a **conic** or **spiral** gradient has the added benefit of smoothing the transition between the first and last colours, regardless of whether you wish to animate the gradient, as below:

<p align="center"><a href="https://github.com/micycle1/PeasyGradients">
<img src="resources/other_examples/all_smooth.png" alt="PeasyGradients" width="500" height="500"/></a><br></p>

### Animating Colour 2

```
.mutate()
```

## Other Stuff

### Posterisation

Use posterisation to define the maximum number of colours the PeasyGradient renderer will use to render `Gradients`. Smaller numbers are more restrictive and increase the colour banding effect — there may be times when this artistic effect is desirable.

```
peasyGradients.posterise(10); // renderer will now render gradients with 10 colours at most 
```

| **No Posterisation**![](resources/posterise_examples/posterise_none.png ) | **Posterisation = 10**![](resources/posterise_examples/posterise_10.png) | **Posterisation = 25**![](resources/posterise_examples/posterise_25.png) |
|:---:|:---:|:---:|

Use `.clearPosterisation()` to clear any posterisation setting and reset the renderer.


### Generating Random Gradients

The `Palette` class provides some helper methods for generating `Gradient` colour palettes.

```
randomGradient = new Gradient(Palette.complementary()); // two random colours that are on opposite sides of the color wheel
randomGradient = new Gradient(Palette.triadic()); // 3 random colors that are evenly spaced on the color wheel
randomGradient = new Gradient(Palette.tetradic()); // 4 random colors that are evenly spaced on the color wheel
randomGradient = new Gradient(Palette.randomColors(7)); // N random colours distributed according to the golden ratio
randomGradient = new Gradient(Palette.randomRandomColors(8)); // N random colours also distributed randomly
```

## Library Optimisation
PeasyGradients targets the CPU (as opposed to the GPU) as to not be dependent on OPENGL libraries. To this end, there have been many internal optimisations to make the library suitable for dynamic animation and interaction rather than just static rendering. Rendering (most) gradients at 60fps at 1080p is more than achievable on modern processors.