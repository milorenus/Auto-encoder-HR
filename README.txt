This is the project for heart rate estimation using the head motion and an auto-encoder.

The MainActivity.java is the source code where everything happens.
The camera system runs in landspace(because of OpenCV's screen orientation problem) but the user should hold it in portrait mode.

The method onCameraFrame(line: 130) inside MainActivity.java handles each camera frame.

The class AEHRView.java handles regions tracking (only when 'start' button is pressed).
