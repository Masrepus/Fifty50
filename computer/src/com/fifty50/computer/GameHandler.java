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
    private volatile BufferedImage image;
    private volatile BufferedImage red, yellow, green;
    private boolean hasShownCountdown;

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

                //now repaint
                main.repaint();
            }
        }, 1000, 1000);
    }

    public void paint(Graphics2D g2d) {
        //if there is something to display, draw it
        if (image != null) {
            g2d.drawImage(image, 0, 0, null);
        } else {
            //paint a border between the two camera images
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, main.getHeight() / 2 - 5, main.getWidth(), 10);
        }
    }

    public void paint(Graphics2D g2d, Point origin) {
        //if there is something to display, draw it at the right place (hand panel x=0 is not window x=0)
        if (image != null) {
            g2d.drawImage(image, -origin.x, -origin.y, null);
        }
    }
}
