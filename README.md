# PeasyGradients

PeasyGradients for Processing
A library make drawing gradients in Processing easy-peasy.

PeasyGradients was inspired by Jeremy Behreandt's [*Color Gradients in Processing*](https://medium.com/@behreajj/color-gradients-in-processing-v-2-0-e5c0b87cdfd2) but has greatly developed the concepts and functionality there.

## Overview
With PeasyGradients, you can draw many different types of gradients with a choice of ~10 colour spaces and ~10 interpolation functions. By default, the library draws directly into the sketch; you can pass your own PGraphics object to draw into with `setRenderTarget()`.

### Creating a 1D gradient

The `PeasyGradients` class renders 1D `Gradients` as 2D images in your sketch. A `Gradient` consists of 
...
### Rendering a 2D gradient
...

## Installation

* To use PeasyGradients in a Java IDE, simply download the most recent .jar from [releases](https://github.com/micycle1/PeasyGradients/releases/) and include it in your project's classpath.

* To use it in the Processing IDE (to appear in the contribution manager), see [this](https://github.com/processing/processing/wiki/How-to-Install-a-Contributed-Library).

## Gradients

With PeasyGradients, there are a number of different gradients 

Zoom and rotation can be ; certain gradient types might offer additional parameters.

### Linear
<details><summary>More...</summary>
  * `linearGradient(Gradient gradient, float angle)`
</details>

### Radial
### Conic
### Conic Smooth
### Spiral
### Diamond
### Cross
### Polygon
### Cone
### Hourglass
### Noise

## Colour Spaces

Remember that a 1D gradient consists of only a few defined colour stops; all other colours in a gradient's spectrum are constructed by **interpolating** between two adjacent colour stops. Colour spaces define how colour stops are represented and can have a noticeable effect o how the interpolated colours are generated.

the colour is represented during **interpolation** and can have a noticeable effect on how a gradient transitions between colours (so experimentation with different colour spaces is encouraged). A rule of thumb: avoid the `RGB`, `RYB` and `HSB` colour spaces as they don't interpolate luminosity well.


 The colour space for a given `Gradient`  is set with `setColSpace()` as such:
 
 `myGradient.setColSpace(ColourSpaces.RGB);`

 PeasyGradients supports many different colour spaces — these are the possible colour spaces (accessible via `ColourSpaces.class`):

* `RGB`
* `RYB`
* `HSB`
* `XYZ` (CIE 1931) [**gradient default**]
* `LAB` (CIE L\*a\*b*)
* `DIN99`
* `ITP` (ICtCp)
* `HLAB` (Hunter LAB)
* `LUV` (CIE 1976 L*, u*, v*)
* `JAB` (JzAzBz)



## Interpolation: Easing Functions
Easing functions affect the behaviour of the gradient ramp between 2 adjacent colour stops. For a given percentage between two colour stops, an easing function maps this initial percentage to another, usually in some kind of non-linear relationship; this can result in more interesting gradients.
 
Certain easing functions suit some gradient types better than others — for example, `BOUNCE` works well with polygon gradients but rather more poorly with linear gradients.

These are results:

These are the the available interpolation easing functions in PeasyGradients (accessible via the `Interpolation` enum):

* `LINEAR`
* `IDENTITY`
* `SMOOTH_STEP` [**gradient default**]
* `SMOOTHER_STEP` (Ken Perlin’s smoother step)
* `EXPONENTIAL`
* `CUBIC`
* `BOUNCE`
* `CIRCULAR`
* `SINE`
* `PARABOLA`
* `GAIN1` (gain function with a certain constant)
* `GAIN2` (as above, but with different constant)
* `EXPIMPULSE` (Exponential Impulse)

## Library Optimisation
PeasyGradients has been written to target the CPU (as opposed to the GPU) as to not be dependent on OPENGL libraries. To this end, there have been many internal optimisations to make the library suitable for dynamic animation and interaction (rather than just static rendering). Rendering (most) gradients at no less than 60fps at 1080p is more than achievable on modern processors.