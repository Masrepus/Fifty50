# Fifty50
Control a Lego Racer from your computer by using Hand gestures or a computer keyboard.</br>
https://youtu.be/H75F7KaKuV0 </br>
Screenshot: </br>
![](https://raw.githubusercontent.com/Masrepus/Fifty50/master/screenshot.jpg) </br>
Install the car software on a Raspberry Pi and set up the device to provide a WiFi hotspot. The client software on your computer then connects to this hotspot and sends steering commands to the RPi.</br>
When the RPi receives those commands, it sets GPIO-pins accordingly to pass the commands on to the Lego Racer's original remote control, which then controls the car. The car control had to be done in this indirect way, because there is no easily available direct access to the car's motors. But when opening the original remote control, we figured out which of the contacts on its PCB have to be connected for each steering command. So in the current setup, the signal being output by the RPi triggers switches connected to the remote control, which in turn connect the desired contacts on the PCB electrically. The remote control then sends the intended command to the car.</br>
A colored glove should be worn so that hand gestures are properly recognized. A tool to calibrate to the glove's color is provided.</br>
Alternatively you can control the car and view the livestream via <a href="http://github.com/Masrepus/Fifty50Cardboard">Fifty50 Cardboard App</a> for Google Cardboard. </br>
Photos of the car and the RasPi wiring:
![](https://raw.githubusercontent.com/Masrepus/Fifty50/master/car.jpg) </br>
![](https://raw.githubusercontent.com/Masrepus/Fifty50/master/car-open.jpg) </br>
![](https://raw.githubusercontent.com/Masrepus/Fifty50/master/remote-control-wiring.jpg) </br>
