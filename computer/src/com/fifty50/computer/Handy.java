package com.fifty50.computer;
// Handy.java
// Andrew Davison, July 2013, ad@fivedots.psu.ac.th

/* Detect the user's gloved hand and fingers, drawing information
   on top of a webcam image.

   Usage:
   > run Handy
*/

import com.googlecode.javacpp.Loader;
import com.googlecode.javacv.cpp.opencv_objdetect;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;


public class Handy extends JFrame {
    // GUI components
    private HandPanel handPanel;


    public Handy() {
        super("Hand Detector");

        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        // Preload the opencv_objdetect module to work around a known bug.
        Loader.load(opencv_objdetect.class);

        handPanel = new HandPanel(); // the webcam pictures and drums appear here
        c.add(handPanel, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                handPanel.closeDown();    // stop snapping pics, and any drum playing
                System.exit(0);
            }
        });

        setResizable(false);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    } // end of Handy()


    // -------------------------------------------------------

    public static void main(String args[]) {
        new Handy();
    }

} // end of Handy class
