package com.fifty50.computer;
// HandPanel.java
// Andrew Davison, July 2013, ad@fivedots.psu.ac.th

/* This panel repeatedly snaps a picture and draw it onto
   the panel. OpenCV is used, via the HandDetector class, to detect
   the user's gloved hand and label the fingers.

*/

import com.googlecode.javacv.FrameGrabber;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;


public class HandPanel extends JPanel implements Runnable {

    private static final int DELAY = 100;  // time (ms) between redraws of the panel

    private static final int CAMERA_ID = 0;


    private IplImage snapIm = null;
    private volatile boolean isRunning;
    private volatile boolean isFinished;

    // used for the average ms snap time information
    private int imageCount = 0;
    private long totalTime = 0;
    private Font msgFont;

    private HandDetector detector = null;   // for detecting hand and fingers
    private GestureDetector gestureDetector;

    private String extraMsg = "";
    private boolean isCalibrated;

    private int x, y, width, height;

    public HandPanel(int width, int height, int x, int y) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        setBackground(Color.white);
        msgFont = new Font("SansSerif", Font.BOLD, 18);
        detector = new HandDetector("gloveHSV.txt", width, height, x, y);
        // include the HSV color info about the user's gloved hand
    } // end of HandPanel()

    public int getRealHeight() {
        return height;
    }

    public void run()
  /* display the current webcam image every DELAY ms.
     Find the coloured rectangles in the image using HandDetector
     objects.
     The time statistics gathered here include the time taken to
     detect movement.
  */ {
        FrameGrabber grabber = initGrabber(CAMERA_ID);
        if (grabber == null)
            return;

        long duration;
        isRunning = true;
        isFinished = false;

        while (isRunning) {
            long startTime = System.currentTimeMillis();

            snapIm = picGrab(grabber, CAMERA_ID);
            imageCount++;
            detector.update(snapIm);
            repaint();

            duration = System.currentTimeMillis() - startTime;
            totalTime += duration;
            if (duration < DELAY) {
                try {
                    Thread.sleep(DELAY - duration);  // wait until DELAY time has passed
                } catch (Exception ex) {
                }
            }
        }
        closeGrabber(grabber, CAMERA_ID);
        System.out.println("Execution terminated");
        isFinished = true;
    }  // end of run()


    private FrameGrabber initGrabber(int ID) {
        FrameGrabber grabber = null;
        System.out.println("Initializing grabber for " + CAMERA_ID + " ...");
        try {
            grabber = FrameGrabber.createDefault(ID);
            grabber.setFormat("dshow");       // using DirectShow
            grabber.setImageWidth(width);     // default is too small: 320x240
            grabber.setImageHeight(height);
            grabber.start();
        } catch (Exception e) {
            System.out.println("Could not start grabber");
            System.out.println(e);
            System.exit(1);
        }
        return grabber;
    }  // end of initGrabber()


    private IplImage picGrab(FrameGrabber grabber, int ID) {
        IplImage im = null;
        try {
            im = grabber.grab();  // take a snap
        } catch (Exception e) {
            System.out.println("Problem grabbing image for camera " + ID);
        }
        return im;
    }  // end of picGrab()


    private void closeGrabber(FrameGrabber grabber, int ID) {
        try {
            grabber.stop();
            grabber.release();
        } catch (Exception e) {
            System.out.println("Problem stopping grabbing for camera " + ID);
        }
    }  // end of closeGrabber()


    public void paintComponent(Graphics g)
  /* Draw the image, the detected hand and finger info, and the 
     average ms snap time at the bottom left of the panel. 
  */ {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        //flip the canvas
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-width, 0);
        g2d.transform(tx);

        int x = this.x - width;

        if (snapIm != null)
            g2d.drawImage(snapIm.getBufferedImage(), x, y, this);

        //paint a triangle pointing to the direction where the car is currently steering to
        int centerVertical = y + (height / 2);

        if (gestureDetector.getCurrDirection() == GestureDetector.Direction.RIGHT) { //image flipped!
            g2d.setColor(Color.RED);
            g2d.fillPolygon(new int[]{x - 20, x, x}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
        } else if (gestureDetector.getCurrDirection() == GestureDetector.Direction.LEFT) {
            g2d.setColor(Color.RED);
            g2d.fillPolygon(new int[]{x + width + 20, x + width, x + width}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
        } else {
            g2d.setColor(Color.BLUE);
            g2d.fillPolygon(new int[]{x - 20, x, x}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
            g2d.fillPolygon(new int[]{x +width + 20, x + width, x + width}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
        }

        //paint a vertical line where the player has set his center point if calibration has been done already
        if (isCalibrated) {
            g2d.setColor(Color.RED);
            g2d.fillRect( x + gestureDetector.getCenter().x - 5, y, 10, height);

            //draw two lines at the ends of the threshold area
            g2d.setColor(Color.BLUE);
            g2d.fillRect(x + gestureDetector.getCenter().x - GestureDetector.CENTER_THRESHOLD, y, 1, height);
            g2d.fillRect(x + gestureDetector.getCenter().x + GestureDetector.CENTER_THRESHOLD, y, 1, height);

            //draw a horizontal line at the top of the brake area
            g2d.fillRect(x , y + height - gestureDetector.getBrakeZoneHeight() - 10, width, 10);
        }

        //now reset it to normal orientation
        g2d.transform(tx);

        if (detector != null)
            detector.draw(g2d);    // draws detected hand and finger info

        writeStats(g2d);
    } // end of paintComponent()


    private void writeStats(Graphics2D g2d)
  /* write statistics in bottom-left corner, or
     "Loading" at start time */ {
        //first draw a white rectangle where the text will be written onto
        g2d.setColor(Color.WHITE);
        g2d.fillRect(x, y + height - 50, width, 50);

        g2d.setColor(Color.BLUE);
        g2d.setFont(msgFont);
        if (imageCount > 0) {
            String statsMsg = String.format("Snap Avg. Time:  %.1f ms",
                    ((double) totalTime / imageCount));
            g2d.drawString(statsMsg + ", Contour angle: " + gestureDetector.getSmoothAngle() + "°, " + "Fingers: " + gestureDetector.getFingerCount() + ", " + gestureDetector.getCurrSpeed() + ", " +gestureDetector.getCurrDirection() + "    " + extraMsg, 5, y + height - 30);
            // write statistics in bottom-left corner
        } else  // no image yet
            g2d.drawString("Loading...", 5, y + height - 30);
    }  // end of writeStats()

    public void setExtraMsg(String extraMsg) {
        this.extraMsg = extraMsg;
    }

    // --------------- called from the top-level JFrame ------------------

    public void closeDown()
  /* Terminate run() and wait for it to finish.
     This stops the application from exiting until everything
     has finished. */ {
        isRunning = false;
        while (!isFinished) {
            try {
                Thread.sleep(DELAY);
            } catch (Exception ex) {
            }
        }
    } // end of closeDown()


    public HandDetector getDetector() {
        return detector;
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;

        new Thread(this).start();   // start updating the panel's image beacuse the gesture detector is ready
    }

    public void setIsCalibrated(boolean isCalibrated) {
        this.isCalibrated = isCalibrated;
    }
} // end of HandPanel class

