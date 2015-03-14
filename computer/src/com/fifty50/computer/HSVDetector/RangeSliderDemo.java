package com.fifty50.computer.HSVDetector;
// RangeSliderDemo.java
// demo of range slider


import com.fifty50.computer.HSVDetector.rslider.RangeSlider;
import com.fifty50.computer.HSVDetector.rslider.RangeSliderPanel;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;


public class RangeSliderDemo extends JFrame {
    private RangeSliderPanel rsPanel;
    private RangeSlider slider;


    public RangeSliderDemo() {
        super("Range Slider Demo");

        Container c = getContentPane();
        c.setLayout(new BorderLayout());

        rsPanel = new RangeSliderPanel("Test Slider", 0, 20, 2, 18);
        // title; slider min, max; current lower, upper settings

        c.add(rsPanel, BorderLayout.CENTER);

        slider = rsPanel.getRangeSlider();
        slider.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int lower = slider.getValue();
                int upper = slider.getUpperValue();
                rsPanel.updateLabels(lower, upper);
                System.out.println("Current lower-upper: " + lower + " - " + upper);
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        pack();
        setResizable(false);
        setVisible(true);
    }  // end of RangeSliderDemo()


    // ---------------------------------

    public static void main(String[] args) {
        new RangeSliderDemo();
    }

}  // end of RangeSliderDemo class
