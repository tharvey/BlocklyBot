# BlocklyBot

BlocklyBot is an Android controller application for the MOBBOB robot
created by Kevin Chan:  http://www.cevinius.com/mobbob/

Kevin's MOBBOB robot features:
 - Bi-ped robot
 - Simple 3D printed parts
 - 4x 9G servos
 - bluetooth Arduino controller (I use Bluno Beetle)
 - Proprietary Android application to control the robot

Kevin's Android application is very full featured but is closed source because
of some proprietary code and Unity assets.

My goal is to create a controller application that features:
 - fully opensource
 - provides base motor controller functions via blockly blocks

Future possible expansions:
 - blockly integration to add blocks for:
  - speech using Android Speech Synth
  - voice commands
  - sound file playback
  - facial expressions
  - OpenCV vision processing
 - save/load blockly code to/from cloud and cloud-sync
 - other programming language API's

This robot and project is intended to the be basis for a grade-school robotics
elective where students will learn the following:
 1. What is a biped robot - steps required for rudimentary walking
 2. CAD design of a simple custom phone holding bracket using online cloud
    based, browser based CAD (onshape.com)
 3. FDM 3D printing basics
 4. Soldering
 5. Light assembly
 6. Blockly programming
