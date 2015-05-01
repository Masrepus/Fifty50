package com.fifty50.computer;

import javax.imageio.ImageIO;
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
    private BufferedImage image;

    public GameHandler(Main main) {
        this.main = main;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void calibrationFinished(Point center) {

        //start the game
        isRunning = true;

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
                    case 2:
                        try {
                            //display the correct image
                            image = ImageIO.read(new File("/home/samuel/fifty50/countdown" + seconds[0] + ".png"));
                        } catch (IOException ignored) {}
                        break;
                    case 6:
                        try {
                            //display the correct image
                            image = ImageIO.read(new File("/home/samuel/fifty50/countdown3.png"));
                        } catch (IOException ignored) {}
                        break;
                    case 7:
                        image = null;
                        timer.cancel();
                        break;
                }

                //now repaint
                main.repaint();
            }
        }, 1000, 1000);
    }

    public void paint(Graphics2D g2d) {
        //if there is something to display, draw it
        if (image != null) g2d.drawImage(image, 0, 0, null);
    }
}
