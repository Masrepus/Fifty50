package com.fifty50.computer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 07.04.15.
 */
public class GameHandler implements OnCalibrationFininshedListener {

    public static final int MAX_TIME_MILLIS = 60000;
    public static final int POINTS_PER_SEC = 100;
    private final Main main;
    private boolean isRunning = false;
    private volatile BufferedImage image;
    private volatile BufferedImage red, yellow, green;
    private JDialog countdownDialog, timerDialog;
    private JLabel imgLabel, timerLabel;
    private volatile int millis;
    private int score;
    private String photoFnm;
    private Timer raceTimer;

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
                        new GameFlow().start();
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
                } else countdownDialog.dispose();

                //now repaint
                main.repaint();
            }
        }, 1000, 1000);
    }

    public void gameFinished() {

        //calculate the score based on the elapsed time
        score = (MAX_TIME_MILLIS - millis) * POINTS_PER_SEC/1000;

        //stop the countdown
        raceTimer.cancel();
        raceTimer.purge();
        timerDialog.dispose();

        //display the game-over panel
        main.getFrame().switchMode(Frame.Mode.GAMEOVER);
    }

    public int getScore() {
        return score;
    }

    public String getPhotoFnm() {
        return photoFnm;
    }

    public void reset() {

        millis = 0;
        score = 0;
        photoFnm = "";
    }

    private class GameFlow extends Thread {

        @Override
        public void run() {

            showTimer();
        }

        private void showTimer() {

            millis = 0;

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
            raceTimer = new Timer();
            //save the current time for consistent time measuring
            final long startTime = System.currentTimeMillis();

            final SimpleDateFormat format = new SimpleDateFormat("mm:ss.SSS");
            raceTimer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {

                    //update the elapsed time
                    millis = (int) (System.currentTimeMillis() - startTime);

                    //after 10 seconds take the action photo
                    if (millis == 2000) takePhoto();


                    timerLabel.setText(format.format(millis));
                }
            }, 10, 10);
        }

        private void takePhoto() {

            //get the current image from handpanel and save it
            BufferedImage actionImg = main.getHandpanel().getCurrImg();

            SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy-HH-mm-ss");
            Date date = new Date();
            photoFnm = format.format(date) + ".jpg";

            if (actionImg != null) try {

                //scale the img down to half the size in order to save space
                BufferedImage actionImgScaled = new BufferedImage(actionImg.getWidth()/2, actionImg.getHeight()/2, actionImg.getType());
                AffineTransform at = new AffineTransform();
                at.scale(0.5, 0.5);

                AffineTransformOp scaleOp =
                        new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
                actionImgScaled = scaleOp.filter(actionImg, actionImgScaled);

                ImageIO.write(actionImgScaled, "JPEG", new File(main.getPath() + photoFnm));
            } catch (IOException e) {
                System.out.println("Konnte Actionfoto nicht speichern!");
            }
        }
    }
}
