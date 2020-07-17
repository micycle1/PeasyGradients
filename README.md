\~work in progress\~

# PeasyGradients0

PeasyGradients for Processing
A library make drawing gradients in Processing easy-peasy.

## Overview
Linear, conic, radial, quad gradients in ~10 colour spaces.

## Interpolation: Colour Spaces
By their very nature, gradients must interpolate between colours. With PeasyGradients you can... In RGB, calculate a new colour by interpolating between each of the red, green and blue components of the gradient colours. Colours can be represented in different colour spaces (so colours comprise of different components) -- when we interpolate 

## Interpolation: Easing Functions
Easing explained: Gradient with 2 colours and we want to find colour in the middle: 0.5\*color1 + 0.5\*colour2. Easy. A colour at 25% position?  0.75\*color1 + 0.25\*colour2. THis is linear interpolation. f(0.75)\*colour1 + f(0.25)\*colour2, leads to different results. In all, you can think of easing as affecting the curve, or ram of the gradient (or how quickly it transitions from one colour the next). Default Smoother step (means you keep the specified colour for longer and transition between more quickly).

## PShape Masking
Filling a PShape with a gradient

## Optimisation
PeasyGradients has been written to target the CPU (as opposed to the GPU, using OPENGL/shaders) as to not be dependent on OPENGL libraries. To this end, there have been many internal optimisations to make the library suitable for dynamic animation and interaction, rather than just static rendering (>30fps at reasonable resolutions).
