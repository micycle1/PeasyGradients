/************************************************************
* PeasyGradients example
*
* Basic white to black gradient rendered to the main PApplet
*************************************************************/ 

// Necessary imports
import micycle.peasygradients.*;
import micycle.peasygradients.gradient.*;

// Gradient context to be bound to a renderer
PeasyGradients peasyGradients;
// Basic black to white Gradient
Gradient blackToWhite = new Gradient(color(0), color(255));

void setup() {
  size(400, 400);
  
  // Binding the gradient context to the current PApplet
  peasyGradients = new PeasyGradients(this);
}

void draw() {
  peasyGradients.linearGradient(blackToWhite, 0); // angle = 0 (horizontal)
}
