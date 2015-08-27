package com.fifty50.computer.HSVDetector;
// HSVPanel.java
// Andrew Davison, July 2013, ad@fivedots.psu.ac.th

/* This panel repeatedly snaps a picture, and
   then convert into smaller images which are displayed side-by-side.
   The right image is a scaled HSV version of the webcam image.
   The left image is a threshold of the HSV image using HSV ranges
   obtained from the sliders in the top-level GUI. The bounded box is
   drawn on top of the threshold image.
*/

import com.googlecode.javacv.FrameGrabber;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;
import static com.googlecode.javacv.cpp.opencv_imgproc.cvResize;


public class HSVPanel extends JPanel implements Runnable {
    /* dimensions of each image; the panel displays two IMG_SCALE scaled WIDTHxHEIGHT images side-by-side */
    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;

    private static final int IMG_SCALE = 2;   // scaling applied to webcam image

    private static final int DELAY = 100;  // time (ms) between redraws of the panel

    private static final int CAMERA_ID = 0;


    private HSVSelector top;  // top-level GUI
    private volatile boolean isRunning;
    private volatile boolean isFinished;

    // used for the average ms snap time information
    private int imageCount = 0;
    private long totalTime = 0;
    private Font msgFont;


    private ColorRectDetector rectDetector;


    public HSVPanel(HSVSelector top) {
        this.top = top;
        setBackground(Color.white);

        msgFont = new Font("SansSerif", Font.BOLD, 18);

        // blob detection will be carried out on scaled images
        rectDetector = new ColorRectDetector(WIDTH / IMG_SCALE, HEIGHT / IMG_SCALE);

        new Thread(this).start();   // start updating the panel's image
    } // end of HSVPanel()


    public Dimension getPreferredSize()
    // the panel displays two IMG_SCALE scaled images side-by-side
    {
        return new Dimension(WIDTH, HEIGHT / IMG_SCALE);
    }


    public void run()
  /* display the current webcam image every DELAY ms.
     Find the largest bounded box in the image based on the HSV settings
     The time statistics gathered here include the time taken to
     detect movement.
  */ {
        FrameGrabber grabber = initGrabber(CAMERA_ID);
        if (grabber == null)
            return;

        IplImage snapIm;
        IplImage scaleImg = IplImage.create(WIDTH / IMG_SCALE, HEIGHT / IMG_SCALE, 8, 3);

        long duration;
        isRunning = true;
        isFinished = false;

        while (isRunning) {
            long startTime = System.currentTimeMillis();

            // update detectors HSV settings
            rectDetector.setHueRange(top.getHueLower(), top.getHueUpper());
            rectDetector.setSatRange(top.getSatLower(), top.getSatUpper());
            rectDetector.setBriRange(top.getBriLower(), top.getBriUpper());


            snapIm = picGrab(grabber, CAMERA_ID);   // take a snap
            imageCount++;
            cvResize(snapIm, scaleImg);
            rectDetector.findRect(scaleImg);
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
        System.out.println("Initializing grabber for " + ID + " ...");
        try {
            grabber = FrameGrabber.create("OpenCV", ID);
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


    // ------------------- rendering ----------------------------

    public void paintComponent(Graphics g)
  /* Draw the threshold and HSV images side-by-side. 
     Draw the bounded box polygon on top of the threshold image.
     Add the average ms snap time at the bottom left of the panel. 
  */ {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        if (rectDetector != null)
            drawImages(g2);

        writeStats(g2);
    } // end of paintComponent()


    private void drawImages(Graphics2D g2)
    // display threshold image with bounded box, and HSV image
    {
        int threshWidth = 0;
        BufferedImage threshIm = rectDetector.getThresholdImage();
        if (threshIm != null) {
            g2.drawImage(threshIm, 0, 0, this);   // draw threshold
            threshWidth = threshIm.getWidth();

            Polygon boxPoly = rectDetector.getBoundedBox();
            if (boxPoly != null) {
                g2.setPaint(Color.YELLOW);
                g2.drawPolygon(boxPoly);   // draw bounding box on top of threshold
            }
        }

        // display HSV image to the right of the threshold image
        BufferedImage hsvIm = rectDetector.getHSVImage();
        if (hsvIm != null)
            g2.drawImage(hsvIm, threshWidth, 0, this);
    }  // end of drawImages()


    private void writeStats(Graphics2D g2)
  /* write statistics in bottom-left corner, or
     "Loading" at start time */ {
        g2.setFont(msgFont);
        if (imageCount > 0) {
            g2.setColor(Color.YELLOW);
            String statsMsg = String.format("Snap Avg. Time:  %.1f ms",
                    ((double) totalTime / imageCount));
            g2.drawString(statsMsg, 5, HEIGHT / IMG_SCALE - 10);
        } else { // no image yet
            g2.setColor(Color.BLUE);
            g2.drawString("Loading...", 5, HEIGHT / IMG_SCALE - 10);
        }
    }  // end of writeStats()


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


} // end of HSVPanel class

