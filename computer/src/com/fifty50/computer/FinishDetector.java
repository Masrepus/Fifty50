package com.fifty50.computer;

import com.charliemouse.cambozola.Viewer;
import com.fifty50.computer.HSVDetector.ColorRectDetector;
import com.googlecode.javacv.cpp.opencv_core;
import sun.awt.image.ToolkitImage;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by samuel on 10.10.15.
 */
public class FinishDetector {

    private static final int MIN_BOX_SIZE = 40000;
    // default HSV initial slider ranges
    private static final int HUE_LOWER = 0;
    private static final int HUE_UPPER = 179;
    // the Hue component ranges from 0 to 179 (not 255)

    private static final int SAT_LOWER = 0;
    private static final int SAT_UPPER = 255;

    private static final int BRI_LOWER = 0;
    private static final int BRI_UPPER = 255;
    private ColorRectDetector detector;
    private int width, height;
    private Viewer carCam;
    private boolean isRunning;
    private int hueLower, hueUpper, satLower, satUpper, briLower, briUpper;

    private Main main;

    public FinishDetector(Main main, Viewer carCam, int width, int height, String hsvPath) {
        this.width = width;
        this.height = height;
        this.carCam = carCam;
        this.main = main;

        detector = new ColorRectDetector(width, height);
        readHSVRanges(hsvPath);

        // update detectors HSV settings
        detector.setHueRange(hueLower, hueUpper);
        detector.setSatRange(satLower, satUpper);
        detector.setBriRange(briLower, briUpper);
    }

    public void start() {
        isRunning = true;
        new Analyzer().start();
    }

    public void stop() {
        isRunning = false;
    }

    private void readHSVRanges(String fnm)
    // read three lines for the lower/upper HSV ranges
    {
        try {
            BufferedReader in = new BufferedReader(new FileReader(new File(fnm)));
            int[] vals = extractInts(in.readLine(), HUE_LOWER, HUE_UPPER);     // get hues
            hueLower = vals[0];
            hueUpper = vals[1];

            vals = extractInts(in.readLine(), SAT_LOWER, SAT_UPPER);     // get saturations
            satLower = vals[0];
            satUpper = vals[1];

            vals = extractInts(in.readLine(), BRI_LOWER, BRI_UPPER);     // get brightnesses
            briLower = vals[0];
            briUpper = vals[1];

            in.close();
            System.out.println("Read HSV ranges from " + fnm);
        } catch (IOException e) {
            System.out.println("Could not read HSV ranges from " + fnm + "; using defaults");
            hueLower = HUE_LOWER;
            hueUpper = HUE_UPPER;
            satLower = SAT_LOWER;
            satUpper = SAT_UPPER;
            briLower = BRI_LOWER;
            briUpper = BRI_UPPER;
        }
    }

    private int[] extractInts(String line, int lower, int upper)
  /*  Format of line <word>:  lower upper
  */ {
        int[] vals = new int[2];
        vals[0] = lower;
        vals[1] = upper;

        String[] toks = line.split("\\s+");
        try {
            vals[0] = Integer.parseInt(toks[1]);
            vals[1] = Integer.parseInt(toks[2]);
        } catch (NumberFormatException e) {
            System.out.println("Error reading line \"" + line + "\"");
        }
        return vals;
    }

    private class Analyzer extends Thread {

        @Override
        public void run() {

            while (isRunning) {

                Image img = carCam.getImage();

                //wait until the viewer has images
                if (img == null) continue;

                //convert the image to buffered image and then to iplimage
                /*BufferedImage bufferedImage = new BufferedImage(640, 480,
                        BufferedImage.TYPE_INT_ARGB);

                Graphics g = bufferedImage.createGraphics();
                g.drawImage(img, 0, 0, null);
                g.dispose();*/
                opencv_core.IplImage image = opencv_core.IplImage.createFrom(((ToolkitImage) img).getBufferedImage());

                //now pass this image to the rect detector
                boolean foundRect = detector.findRect(image);

                if (foundRect) {

                    //check if the found rectangle meets the minimum size
                    Rectangle bounds = detector.getBoundedBox().getBounds();

                    if (bounds.width * bounds.height >= MIN_BOX_SIZE) {
                        System.out.println("Finish sign found: " + bounds.width + "x" + bounds.height + ", finishing game...");
                        main.getHandler().gameFinished();
                        break;
                    }
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {}
            }
        }
    }
}
