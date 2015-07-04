package com.fifty50.computer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
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
    private JDialog countdownDialog, timerDialog;
    private JLabel imgLabel, timerLabel;
    private int millis;

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
                        //start the game!
                        isRunning = true;
                        new GameLoop().start();
                        break;
                }

                if (image != null) {

                    //display the countdown images in a dialog
                    if (countdownDialog == null) {
                        countdownDialog = new JDialog(main.getFrame());
                        imgLabel = new JLabel();
                        countdownDialog.setSize(image.getWidth(), image.getHeight());
                        countdownDialog.setLocationRelativeTo(null);
                        countdownDialog.setUndecorated(true);
                        countdownDialog.add(imgLabel);
                    }

                    imgLabel.setIcon(new ImageIcon(image));
                    imgLabel.revalidate();
                    countdownDialog.setVisible(true);
                } else countdownDialog.setVisible(false);

                //now repaint
                main.repaint();
            }
        }, 1000, 1000);
    }

    private class GameLoop extends Thread {

        @Override
        public void run() {

            //display a dialog in the upper right corner that shows the elapsed time
            timerDialog = new JDialog(main.getFrame());
            timerDialog.setUndecorated(true);

            //set up the label that will display the time
            timerLabel = new JLabel("00:00.0000", JLabel.CENTER);
            timerLabel.setFont(new Font(null, Font.BOLD, 30));
            timerLabel.setForeground(Color.WHITE);
            timerDialog.add(timerLabel);

            //calculate where the dialog should be located and how big it should be
            int stringLen = (int) timerLabel.getFontMetrics(timerLabel.getFont()).getStringBounds("88:88:8888", null).getWidth();
            timerDialog.setBounds(main.getWidth() - 10 - stringLen - 40, 10, stringLen + 40, 50);
            timerDialog.setVisible(true);

            //start the time measurements
            Timer raceTimer = new Timer();
            raceTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    //update the elapsed time
                    millis += 10;
                    SimpleDateFormat format = new SimpleDateFormat("mm:ss.SSS");
                    timerLabel.setText(format.format(millis));
                }
            }, 10, 10);
        }
    }
}
