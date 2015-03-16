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

import static com.googlecode.javacv.cpp.opencv_core.IplImage;


public class HandPanel extends JPanel implements Runnable {
    /* dimensions of each image; the panel is the same size as the image */
    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;

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

    private String extraMsg;
    private boolean isCalibrated;


    public HandPanel() {
        setBackground(Color.white);
        msgFont = new Font("SansSerif", Font.BOLD, 18);
        detector = new HandDetector("gloveHSV.txt", WIDTH, HEIGHT);
        // include the HSV color info about the user's gloved hand
    } // end of HandPanel()


    public Dimension getPreferredSize()
    // make the panel wide enough for an image
    {
        return new Dimension(WIDTH, HEIGHT);
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
            grabber.setImageWidth(WIDTH);     // default is too small: 320x240
            grabber.setImageHeight(HEIGHT);
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

        if (snapIm != null)
            g2d.drawImage(snapIm.getBufferedImage(), 0, 0, this);

        if (detector != null)
            detector.draw(g2d);    // draws detected hand and finger info

        //paint a triangle pointing to the direction where the car is currently steering to
        int centerVertical = HEIGHT / 2;

        if (gestureDetector.getCurrDirection() == GestureDetector.Direction.LEFT) {
            g2d.setColor(Color.RED);
            g2d.fillPolygon(new int[]{0, 20, 20}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
        } else if (gestureDetector.getCurrDirection() == GestureDetector.Direction.RIGHT) {
            g2d.setColor(Color.RED);
            g2d.fillPolygon(new int[]{WIDTH, WIDTH - 20, WIDTH - 20}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
        } else {
            g2d.setColor(Color.BLUE);
            g2d.fillPolygon(new int[]{0, 20, 20}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
            g2d.fillPolygon(new int[]{WIDTH, WIDTH - 20, WIDTH - 20}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
        }

        writeStats(g2d);

        //paint a vertical line where the player has set his center point if calibration has been done already
        if (isCalibrated) {
            g2d.setColor(Color.RED);
            g2d.fillRect(gestureDetector.getCenter().x - 5, 0, 10, HEIGHT);
        }
    } // end of paintComponent()


    private void writeStats(Graphics2D g2d)
  /* write statistics in bottom-left corner, or
     "Loading" at start time */ {
        g2d.setColor(Color.BLUE);
        g2d.setFont(msgFont);
        if (imageCount > 0) {
            String statsMsg = String.format("Snap Avg. Time:  %.1f ms",
                    ((double) totalTime / imageCount));
            g2d.drawString(statsMsg + ", Contour angle: " + gestureDetector.getSmoothAngle() + "°, " + "Fingers: " + gestureDetector.getFingerCount() + ", " + gestureDetector.getCurrDirection() + "    " + extraMsg, 5, HEIGHT - 10);
            // write statistics in bottom-left corner
        } else  // no image yet
            g2d.drawString("Loading...", 5, HEIGHT - 10);
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

