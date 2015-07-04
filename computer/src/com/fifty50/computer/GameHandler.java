package com.fifty50.computer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 07.04.15.
 */
public class GameHandler implements OnCalibrationFininshedListener {

    private final Main main;
    private boolean isRunning = false;
    private volatile BufferedImage image;
    private volatile BufferedImage red, yellow, green;
    private boolean hasShownCountdown;
    private JDialog countdownDialog;
    private JLabel label;

    public GameHandler(Main main, String path) {
        this.main = main;

        //pre-load the traffic light images
        try {
            red = ImageIO.read(new File(path + "countdown1.png"));
            yellow = ImageIO.read(new File(path + "countdown2.png"));
            green = ImageIO.read(new File(path + "countdown3.png"));
        } catch (IOException e) {
            System.out.println("image not found!");
            e.printStackTrace();
            System.exit(404);
        }
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void calibrationFinished(Point center) {
        displayCountdown();
    }

    private void displayCountdown() {

        //display a new colour every second but wait until second 6 before displaying the green light
        final java.util.Timer timer = new Timer();
        final int[] seconds = new int[1];

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                seconds[0]++;

                switch (seconds[0]) {

                    case 1:
                        image = red;
                        hasShownCountdown = true;
                        break;
                    case 2:
                        image = yellow;
                        break;
                    case 6:
                        image = green;
                        break;
                    case 7:
                        image = null;
                        timer.cancel();
                        break;
                }

                if (image != null) {

                    //display the countdown images in a dialog
                    if (countdownDialog == null) {
                        countdownDialog = new JDialog();
                        label = new JLabel();
                        countdownDialog.setSize(image.getWidth(), image.getHeight());
                        countdownDialog.setLocationRelativeTo(null);
                        countdownDialog.setUndecorated(true);
                        countdownDialog.add(label);
                    }

                    label.setIcon(new ImageIcon(image));
                    label.revalidate();
                    countdownDialog.setVisible(true);
                } else countdownDialog.setVisible(false);

                //now repaint
                main.repaint();

                //start the game!
                isRunning = true;
            }
        }, 1000, 1000);
    }
}
