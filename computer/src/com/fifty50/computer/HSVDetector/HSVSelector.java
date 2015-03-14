package com.fifty50.computer.HSVDetector;
// HSVSelector.java
// Andrew Davison, July 2013, ad@fivedots.psu.ac.th

/* This application uses JavaCV (OpenCV) to display webcam images, translated to
   HSV format, and a binary threshold version of the images. The user adjusts
   hue (H), saturation (S), and brightness (V) GUI sliders to choose HSV ranges 
   that best modify the threshold so that a bounding box is drawn around the object
   of interest in the threshold image

   The HSV ranges can be saved to SETTINGS_FNM (hsvSettings.txt), and intial 
   settings will be read from there.

   This code uses a slightly modified version of the RangeSlider class by
   Ernie Yu.

   Usage:
   > run HSVSelector

*/

import com.fifty50.computer.HSVDetector.rslider.RangeSlider;
import com.fifty50.computer.HSVDetector.rslider.RangeSliderPanel;
import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_objdetect;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

// import java.awt.Point;


public class HSVSelector extends JFrame {
    // default HSV initial slider ranges
    private static final int HUE_LOWER = 0;
    private static final int HUE_UPPER = 179;
    // the Hue component ranges from 0 to 179 (not 255)

    private static final int SAT_LOWER = 0;
    private static final int SAT_UPPER = 255;

    private static final int BRI_LOWER = 0;
    private static final int BRI_UPPER = 255;


    private static final int MIN_PIXELS = 100;
    private static final float SMALLEST_BOX = 100.0f;

    private static final String SETTINGS_FNM = "/home/samuel/gloveHSV.txt";

    // GUI components
    private HSVPanel hsvPanel;

    // HSV sliders and ranges
    private RangeSliderPanel huePanel, satPanel, brightPanel;
    private RangeSlider hueSlider, satSlider, brightSlider;
    private int hueLower, hueUpper, satLower, satUpper, briLower, briUpper;


    public HSVSelector() {
        super("HSV Selector");

        readHSVRanges(SETTINGS_FNM);
        makeGUI();

        // Preload the opencv_objdetect module to work around a known bug.
        Loader.load(opencv_objdetect.class);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                hsvPanel.closeDown();    // stop snapping pics
                System.exit(0);
            }
        });

        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    } // end of HSVSelector()

    public static void main(String args[]) {
        new HSVSelector();
    }

    private void makeGUI()
  /* display and controls panel; the controls panel is made up
     of two rows of three sliders and a button
  */ {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        hsvPanel = new HSVPanel(this); // the sequence of pictures appear here
        c.add(hsvPanel, BorderLayout.CENTER);

        JPanel ctrlPanel = new JPanel();
        ctrlPanel.setLayout(new BoxLayout(ctrlPanel, BoxLayout.Y_AXIS));
        c.add(ctrlPanel, BorderLayout.SOUTH);

        // two-row control panel
        JPanel p1 = new JPanel();
        ctrlPanel.add(p1);
        JPanel p2 = new JPanel();
        ctrlPanel.add(p2);

        // initialize 3 range sliders for the lower/upper HSV ranges
        initHueSlider(p1);
        initSatSlider(p1);
        initBrightSlider(p2);

        JButton saveBut = new JButton("Save Settings");
        saveBut.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveHSVRanges(SETTINGS_FNM);
            }
        });
        p2.add(saveBut);

    }  // end of makeGUI()

    private void initHueSlider(JPanel p)
    // initialize a range slider for hue (H) range of HSV
    {
        huePanel = new RangeSliderPanel("Hue", 0, 179, hueLower, hueUpper);
        p.add(huePanel);

        hueSlider = huePanel.getRangeSlider();
        hueSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                hueLower = hueSlider.getValue();
                hueUpper = hueSlider.getUpperValue();
                huePanel.updateLabels(hueLower, hueUpper);
            }
        });
    }  // end of initHueSlider()

    private void initSatSlider(JPanel p)
    // initialize a range slider for saturation (S) range of HSV
    {
        satPanel = new RangeSliderPanel("Saturation", 0, 255, satLower, satUpper);
        p.add(satPanel);

        satSlider = satPanel.getRangeSlider();
        satSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                satLower = satSlider.getValue();
                satUpper = satSlider.getUpperValue();
                satPanel.updateLabels(satLower, satUpper);
            }
        });
    }  // end of initSatSlider()


    // -------------- access slider settings --------------------
    // called by HSVPanel

    private void initBrightSlider(JPanel p)
    // initialize a range slider for brightness (V) range of HSV
    {
        brightPanel = new RangeSliderPanel("Brightness", 0, 255, briLower, briUpper);
        p.add(brightPanel);

        brightSlider = brightPanel.getRangeSlider();
        brightSlider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                briLower = brightSlider.getValue();
                briUpper = brightSlider.getUpperValue();
                brightPanel.updateLabels(briLower, briUpper);
            }
        });
    }  // end of initBrightSlider()

    public int getHueLower() {
        return hueLower;
    }

    public int getHueUpper() {
        return hueUpper;
    }

    public int getSatLower() {
        return satLower;
    }

    public int getSatUpper() {
        return satUpper;
    }

    public int getBriLower() {
        return briLower;
    }


    // ------ read and write HSV ranges to a file ---------------

    public int getBriUpper() {
        return briUpper;
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
    }  // end of readHSVRanges()

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
    }  // end of extractInts()


    // -------------------------------------------------------

    public void saveHSVRanges(String fnm)
    // write out three lines for the lower/upper HSV ranges
    {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(new File(fnm)));
            out.println("hue: " + hueLower + " " + hueUpper);
            out.println("sat: " + satLower + " " + satUpper);
            out.println("val: " + briLower + " " + briUpper);
            out.close();
            System.out.println("Saved HSV ranges to " + fnm);
        } catch (IOException e) {
            System.out.println("Could not save HSV ranges to " + fnm);
        }
    }  // end of saveHSVRanges()

} // end of HSVSelector class
