\~work in progress\~

# PeasyGradients

PeasyGradients for Processing
A library make drawing gradients in Processing easy-peasy.

## Overview
Renders many different types of gradients with a choice of ~10 colour spaces and ~10 interpolation functions. By default, the library draws into the sketch. when a gradient is called. You can pass it 

## Installation

https://github.com/processing/processing/wiki/How-to-Install-a-Contributed-Library

## Gradients

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

PeasyGradients supports many colour spaces. Different colour spaces can have a noticable effect on how a gradient transitions between colours so experimentation is encouraged. A rule of thumb: avoid the `RGB`, `RYB` and `HSB` colour spaces.

 `myGradient.setColSpace(ColourSpaces.RGB);`

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

A gradient consists of only a few defined colour stops. All other colours in the gradient's spectrum are constructed by interpolating between any two adjacent colour stops.
By their very nature, gradients must interpolate between colours. With PeasyGradients you can... In RGB, calculate a new colour by interpolating between each of the red, green and blue components of the gradient colours. Colours can be represented in different colour spaces (so colours comprise of different components) -- when we interpolate 

## Interpolation: Easing Functions
Certain easing functions have 
Interpolation

* Linear
* Identity
* Smooth Step [**gradient default**]
* Smoother Step
* Exponential
* Cubic
* Circular
* Sine
* Parabola
* Gain (1)
* Gain (2)
* Exponential Impulse

Easing explained: Gradient with 2 colours and we want to find colour in the middle: 0.5\*color1 + 0.5\*colour2. Easy. A colour at 25% position?  0.75\*color1 + 0.25\*colour2. THis is linear interpolation. f(0.75)\*colour1 + f(0.25)\*colour2, leads to different results. In all, you can think of easing as affecting the curve, or ram of the gradient (or how quickly it transitions from one colour the next). Default Smoother step (means you keep the specified colour for longer and transition between more quickly).

## PShape Masking
Filling a PShape with a gradient

## Optimisation
PeasyGradients has been written to target the CPU (as opposed to the GPU) as to not be dependent on OPENGL libraries. To this end, there have been many internal optimisations to make the library suitable for dynamic animation and interaction, rather than just static rendering; gradients at >=60fps at 1080p are achievable on modern processors.
