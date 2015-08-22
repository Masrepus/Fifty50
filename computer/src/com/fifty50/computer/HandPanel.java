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
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;

import static com.googlecode.javacv.cpp.opencv_core.IplImage;


public class HandPanel extends JPanel implements Runnable {

    public static final int DELAY_DEFAULT = 100;  // time (ms) between redraws of the panel
    public static final int DELAY_HIGH_FREQ = 5;

    private static final int CAMERA_ID = 0;

    private boolean drawCOGOnly;


    private IplImage snapIm = null;
    private BufferedImage currImg;
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

    private boolean debug;

    private int delay;
    private GameHandler handler;
    private long duration = 0;
    private Starter starter;
    private Frame frame;
    private volatile boolean dialogShown;
    private Thread handPanelThread;

    public HandPanel(String hsvPath, int width, int height, int x, int y, boolean debug, boolean drawCOGOnly, Color background) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.debug = debug;
        this.drawCOGOnly = drawCOGOnly;

        if (drawCOGOnly) delay = DELAY_HIGH_FREQ;
        else delay = DELAY_DEFAULT;

        setBackground(background);
        msgFont = new Font("SansSerif", Font.BOLD, 18);

        //check the available webcam image size
        FrameGrabber grabber = initGrabber(CAMERA_ID);
        try {
            detector = new HandDetector(hsvPath, grabber.getImageWidth(), grabber.getImageHeight(), x, y, width, height);
            grabber.stop();
            grabber.release();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
            System.exit(404);
        }
        // include the HSV color info about the user's gloved hand
    } // end of HandPanel()

    public int getRealHeight() {
        return height;
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
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

        isRunning = true;
        isFinished = false;

        while (isRunning) {
            long startTime = System.currentTimeMillis();

            snapIm = picGrab(grabber, CAMERA_ID);
            if (snapIm != null && snapIm.getBufferedImage() != null) currImg = snapIm.getBufferedImage();
            imageCount++;
            if (snapIm != null) detector.update(snapIm);
            repaint();

            if (drawCOGOnly && starter != null) starter.repaint();

            duration = System.currentTimeMillis() - startTime;
            totalTime += duration;
            if (duration < delay) {
                try {
                    Thread.sleep(duration);  // wait the amount of time it took to shoot the last picture
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
            grabber = FrameGrabber.create("OpenCV", CAMERA_ID); //FrameGrabber.createDefault(ID);
            grabber.setFormat("mjpeg");       // using DirectShow
            grabber.setFrameRate(50);
            grabber.start();
            System.out.println("Image size: " + grabber.getImageWidth() + "x" + grabber.getImageHeight() + ", framerate: " + grabber.getFrameRate());
        } catch (Exception e) {
            System.out.println("Could not start grabber");
            e.printStackTrace();
            System.exit(1);
        }
        return grabber;
    }  // end of initGrabber()


    private IplImage picGrab(FrameGrabber grabber, int ID) {
        IplImage im = null;
        try {
            //im = grabber.grab();  // take a snap
            //hack the image out of the grabber!
            grabber.grab();
            Field f = grabber.getClass().getDeclaredField("return_image");
            f.setAccessible(true);
            im = (IplImage) f.get(grabber);
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

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        drawContent(g);
    }

    private void drawContent(Graphics g) {

        /* Draw the image, the detected hand and finger info, and the
     average ms snap time at the bottom left of the panel.
  */
        Graphics2D g2d = (Graphics2D) g;

        //flip the canvas
        AffineTransform tx = AffineTransform.getScaleInstance(-1, 1);
        tx.translate(-width, 0);
        g2d.transform(tx);

        //x offset calculations somehow not necessary anymore
        int x_offset = -x, y_offset = 0;

        //if requested, draw the image that was snapped as well
        if (snapIm != null && !drawCOGOnly) {
            g2d.setColor(Color.WHITE);
            g2d.drawImage(snapIm.getBufferedImage(), x_offset, 0, null);
        }

        //paint a triangle pointing to the direction where the car is currently steering to
        int centerVertical = y_offset + (height / 2);

        //if only the cog should be drawn, skip this section
        if (!drawCOGOnly && gestureDetector != null) {
            if (gestureDetector.getCurrDirection() == GestureDetector.Direction.RIGHT) { //image flipped!
                g2d.setColor(Color.RED);
                g2d.fillPolygon(new int[]{x_offset - 30, x_offset - 10, x_offset - 10}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
            } else if (gestureDetector.getCurrDirection() == GestureDetector.Direction.LEFT) {
                g2d.setColor(Color.RED);
                g2d.fillPolygon(new int[]{x_offset + width + 30, x_offset + width + 10, x_offset + width + 10}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillPolygon(new int[]{x_offset - 30, x_offset - 10, x_offset - 10}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
                g2d.fillPolygon(new int[]{x_offset + width + 30, x_offset + width + 10, x_offset + width + 10}, new int[]{centerVertical, centerVertical - 20, centerVertical + 20}, 3);
            }

            //paint a vertical line where the player has set his center point if calibration has been done already
            if (isCalibrated) {
                g2d.setColor(Color.RED);
                g2d.fillRect(x_offset + gestureDetector.getCenter().x - 5, y_offset, 10, height);

                //draw two lines at the ends of the threshold area
                g2d.setColor(Color.BLUE);
                g2d.fillRect(x_offset + gestureDetector.getCenter().x - GestureDetector.CENTER_THRESHOLD, y_offset, 1, height);
                g2d.fillRect(x_offset + gestureDetector.getCenter().x + GestureDetector.CENTER_THRESHOLD, y_offset, 1, height);

                //draw a horizontal line at the top of the brake area (center y level)
                g2d.fillRect(x_offset, y_offset + gestureDetector.getCenter().y - 10, width, 10);
            }
        }

        //now reset it to normal orientation
        g2d.transform(tx);

        if (detector != null)
            detector.draw(g2d, drawCOGOnly);    // draws detected hand and finger info

        if (!drawCOGOnly) {
            if (debug) writeStats(g2d); //write verbose stats if debug mode is active

            if (!extraMsg.isEmpty()) {
                g2d.setFont(msgFont);
                int stringLen = (int) g2d.getFontMetrics().getStringBounds(extraMsg, g2d).getWidth();
                int start = width / 2 - stringLen / 2;

                g2d.setColor(Color.WHITE);
                g2d.fillRect(-x_offset + start - 5, y_offset + height - 50, stringLen + 5, 50);

                g2d.setColor(Color.BLUE);
                g2d.drawString(extraMsg, start - x_offset, y_offset + height - 30);
            }
        }
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        drawContent(g);
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
            g2d.drawString(statsMsg + ", Contour angle: " + gestureDetector.getSmoothAngle() + "°, " + "Fingers: " + gestureDetector.getFingerCount() + ", " + gestureDetector.getCurrSpeed() + ", " + gestureDetector.getCurrDirection() + "    " + extraMsg, 5, y + height - 30);
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
                Thread.sleep(delay);
            } catch (Exception ex) {
            }
        }
    } // end of closeDown()


    public HandDetector getDetector() {
        return detector;
    }

    public void setGestureDetector(GestureDetector gestureDetector) {
        this.gestureDetector = gestureDetector;

        isCalibrated = false;

        handPanelThread = new Thread(this);
        // start updating the panel's image beacuse the gesture detector is ready
        handPanelThread.start();
    }

    public void setIsCalibrated(boolean isCalibrated) {
        this.isCalibrated = isCalibrated;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setGameHandler(GameHandler handler) {
        this.handler = handler;
    }

    public double getDuration() {
        return duration;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    public void setStarter(Starter starter) {
        this.starter = starter;
    }

    public Thread getHandPanelThread() {
        return handPanelThread;
    }

    public BufferedImage getCurrImg() {
        return currImg;
    }
} // end of HandPanel class

