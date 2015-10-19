package com.fifty50.computer;

import com.charliemouse.cambozola.Viewer;
import com.fifty50.computer.HSVDetector.ColorRectDetector;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_core;
import com.googlecode.javacv.cpp.opencv_objdetect;
import sun.awt.image.ToolkitImage;

import javax.swing.*;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 15.10.15.
 */
public class FinishCalibrator extends JFrame {

    private static final int HUE_LOWER = 0;
    private static final int HUE_UPPER = 179;
    // the Hue component ranges from 0 to 179 (not 255)

    private static final int SAT_LOWER = 0;
    private static final int SAT_UPPER = 255;

    private static final int BRI_LOWER = 0;
    private static final int BRI_UPPER = 255;

    private static final int WIDTH = 640;
    private static final int HEIGHT = 480;
    private volatile boolean calibrated, timerRunning = false;
    private int iterations;
    private Viewer viewer;
    private JLabel sizeLabel;
    private DecimalFormat format = new DecimalFormat("##.#");

    private Timer timer = new Timer();
    private String path;

    private Rectangle boundedBox;

    public static void main(String[] args) {
        new FinishCalibrator(args);
    }

    public FinishCalibrator(final String[] args) {

        super("Ziellinie Kalibrieren");
        path = args[0];

        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        viewer = initViewer(args[1]);
        viewer.setBounds(0, 0, WIDTH, HEIGHT);
        viewer.init();
        c.add(viewer, BorderLayout.NORTH);

        // Preload the opencv_objdetect module to work around a known bug.
        Loader.load(opencv_objdetect.class);

        //second row: save button and label for size
        JPanel ctrlPanel = new JPanel();
        ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.LINE_AXIS));
        c.add(ctrlPanel, BorderLayout.SOUTH);

        sizeLabel = new JLabel("Bitte stelle das Auto zum Kalibrieren an die Ziellinie", SwingConstants.CENTER);
        ctrlPanel.add(sizeLabel);

        //button that starts calibration
        JButton startBut = new JButton("Start");
        startBut.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new CalibratorThread().start();
            }
        });
        ctrlPanel.add(startBut);

        setResizable(true);
        setSize(WIDTH, 600);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        //viewer.init();
        //viewer.setVisible(true);
    }

    private Viewer initViewer(final String url) {

        //create a new instance of the cambozola mjpg player applet for the live stream

        Viewer v = new Viewer(WIDTH, HEIGHT, null);
        AppletStub stub = new AppletStub() {
            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public URL getDocumentBase() {
                URL base = null;
                try {
                    base = new URL(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return base;
            }

            @Override
            public URL getCodeBase() {
                URL base = null;
                try {
                    base = new URL(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return base;
            }

            @Override
            public String getParameter(String name) {
                if (name.contentEquals("url")) return url;
                else return "";
            }

            @Override
            public AppletContext getAppletContext() {
                return null;
            }

            @Override
            public void appletResize(int WIDTH, int HEIGHT) {

            }
        };
        v.setStub(stub);

        return v;
    }

    private void saveSize(double avgSize) {

        //save the size of the finish flag in percent of the total size
        try {
            PrintWriter out = new PrintWriter(new FileWriter(new File(path + "finishSize.txt")));
            out.println("size: " + format.format(avgSize));
            out.close();
            System.out.println("Saved relative size to " + path + "finishSize.txt");
        } catch (IOException e) {
            System.out.println("Could not save relative size to " + path + "finishSize.txt");
        }
    }

    private void startTimer() {

        //schedule a timer for 3 seconds -> when done set calibrated to true so that the calibration process stops
        iterations = 0;
        timerRunning = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (iterations == 3) {
                    calibrated = true;
                    timer.cancel();
                }
                else iterations++;
            }
        }, 1000, 1000);
    }

    @Override
    public void paintComponents(Graphics g) {
        super.paintComponents(g);

        Graphics2D g2d = (Graphics2D) g;

        if (boundedBox != null) {
            //draw the found box
            g2d.setColor(Color.YELLOW);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2d.drawRect(boundedBox.x, boundedBox.y, boundedBox.width, boundedBox.height);
        }
    }

    private class CalibratorThread extends Thread {

        private ColorRectDetector detector;
        private int hueLower, hueUpper, satLower, satUpper, briLower, briUpper;

        public CalibratorThread() {
            detector = new ColorRectDetector(WIDTH, HEIGHT);
            initDetector();
        }

        private void initDetector() {

            readHSVRanges(path + "finish.txt");

            // update detectors HSV settings
            detector.setHueRange(hueLower, hueUpper);
            detector.setSatRange(satLower, satUpper);
            detector.setBriRange(briLower, briUpper);
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

        @Override
        public void run() {

            //wait until the livestream is working
            while (viewer.getImage() == null) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {
                }
            }

            //wait for a correctly colored rectangle to appear
            calibrated = false;
            timerRunning = false;
            double avgSize = 0;
            boolean foundRect;

            //wait for a correctly colored rectangle to appear and then calibrate the size
            while (!calibrated) {
                //convert the image and resize it to match detector width, height
                Image img = viewer.getImage();
                if (img == null) continue;

                BufferedImage buf = ((ToolkitImage) img).getBufferedImage();
                if (buf == null) continue;

                opencv_core.IplImage image = opencv_core.IplImage.createFrom(/*((ToolkitImage) buf.getScaledInstance(WIDTH, HEIGHT, Image.SCALE_SMOOTH)).getBufferedImage()*/ buf);

                foundRect = detector.findRect(image);

                if (foundRect) {

                    //start the timer
                    if (!timerRunning) startTimer();

                    //save the box locally for drawing
                    boundedBox = detector.getBoundedBox().getBounds();

                    double currSize = (double)(boundedBox.height * boundedBox.width) / (double)(WIDTH * HEIGHT);

                    //calculate the average
                    avgSize = (avgSize == 0) ? currSize : (avgSize + currSize) / 2;

                    //display the size

                    sizeLabel.setText("Größe: " + format.format(avgSize*100) + "% des Gesamtbildes " + boundedBox);

                    repaint();
                }
                try {
                    Thread.sleep(200);
                } catch (InterruptedException ignored) {
                }
            }

            timerRunning = false;
            sizeLabel.setText("Kalibrieren beendet: Größe = " + format.format(avgSize*100) + "% des Gesamtbildes");

            //save the final value
            saveSize(avgSize);
        }
    }
}
