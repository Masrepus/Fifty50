package com.fifty50.computer;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.Timer;

/**
 * Created by samuel on 06.07.15.
 */
public class GameOver extends JLayeredPane implements Runnable {

    private int width, height;
    private JLabel imgLabel, scoreLabel;
    private String path;
    private String photoFnm;
    private Frame frame;

    public GameOver(Frame frame, String path) {

        Toolkit tk = Toolkit.getDefaultToolkit();
        width = tk.getScreenSize().width;
        height = tk.getScreenSize().height;

        this.frame = frame;

        this.path = path;

        setBackground(Color.BLACK);

        setLayout(null);
        setBackground(Color.BLACK);
        setBounds(0, 0, width, height);


        //set bg image
        String bgImg = "gameover";
        if (tk.getScreenSize().width / tk.getScreenSize().height == 16 / 9) bgImg += "_16-9.png";
        else bgImg += "_4-3.png";
        try {
            BackgroundPanel background = new BackgroundPanel(ImageIO.read(new File(path + bgImg)), width, height);
            background.setBounds(0, 0, width, height);
            background.setVisible(true);
            add(background, 1, 0);
        } catch (IOException e) {
            System.out.println("Hintergrundbild nicht gefunden");
        }

        //init the labels
        imgLabel = new JLabel("xxx");
        imgLabel.setBounds(width / 2 - 160, height / 2 - 120, 320, 240);
        add(imgLabel, 2, 0);
        imgLabel.setVisible(true);

        scoreLabel = new JLabel("ooo");
        Font font = new Font(null, Font.BOLD, 30);
        scoreLabel.setFont(font);
        Rectangle2D stringBounds = getFontMetrics(font).getStringBounds("xxxx Punkte, das ist momentan Platz x!!", getGraphics());
        int stringLen = (int) stringBounds.getWidth();
        int stringHeight = (int) stringBounds.getHeight();
        int start = width / 2 - stringLen / 2;
        scoreLabel.setBounds(start, imgLabel.getY() + imgLabel.getHeight() + 30, stringLen, stringHeight);
        add(scoreLabel, 3, 0);
        scoreLabel.setVisible(true);

        setVisible(false);
        revalidate();
    }

    public void display(int score, String photoFnm) {

        setVisible(true);

        this.photoFnm = photoFnm;
        displayActionImg();
        displayScore(score);

        revalidate();
        repaint();

        setFocusable(true);
        requestFocus();

        //start timer
        java.util.Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                frame.switchMode(Frame.Mode.STARTSCREEN);
            }
        }, 10000);
    }

    private void displayScore(int score) {

        //display the player's score and place in highscore rank
        int rank = 1;

        //first read the current ranking
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path + "ranking.txt"));
            String line;
            ArrayList<Integer> ranks = new ArrayList<Integer>();

            while ((line = reader.readLine()) != null) {
                try {
                    //add the score (first part of the line) to the ranks list if this is a parseable int
                    ranks.add(Integer.parseInt(line.split(",")[0]));
                } catch (NumberFormatException ignored) {}
            }

            reader.close();

            //now sort the list
            ranks.sort(new Comparator<Integer>() {
                @Override
                public int compare(Integer o1, Integer o2) {
                    return o2.compareTo(o1);
                }
            });

            //find out what rank the current player holds
            for (int currScore : ranks) {
                if (currScore > score) rank++;
                else break;
            }

            //now add the current player's score to the list, including a path to his action photo
            BufferedWriter writer = new BufferedWriter(new FileWriter(path + "ranking.txt", true));
            writer.write(score + "," + photoFnm);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("Zugriffsfehler für ranking.txt");
        }

        //add !! if the rank is 3 or better
        String text = score + " Punkte, das ist momentan Platz " + rank + ((rank <= 3) ? "!!" : "");
        scoreLabel.setText(text);
        Font font = new Font(null, Font.BOLD, 30);
        scoreLabel.setFont(font);

        Rectangle2D stringBounds = getFontMetrics(font).getStringBounds(text, getGraphics());
        int stringLen = (int) stringBounds.getWidth();
        int stringHeight = (int) stringBounds.getHeight();
        int start = width / 2 - stringLen / 2;

        scoreLabel.setBounds(start, imgLabel.getY() + imgLabel.getHeight() + 30, stringLen, stringHeight);
        scoreLabel.setForeground(Color.WHITE);

        scoreLabel.setVisible(true);
    }

    private void displayActionImg() {

        //display the action photo in the center at 440x440 px (if screen = 1920x1080, else scale accordingly)
        try {
            BufferedImage actionImg = ImageIO.read(new File(path + File.separator + "actionImgs" + File.separator + photoFnm));
            int width = actionImg.getWidth();
            int height = actionImg.getHeight();

            AffineTransform at = new AffineTransform();
            //calculate the scale factor: width/screenwidth = 440/1920
            double newWidth = this.width * (double)440/1920;
            double scale = newWidth / width;
            BufferedImage actionImgScaled = new BufferedImage((int)(width*scale), (int)(height*scale), BufferedImage.TYPE_INT_ARGB);
            at.scale(scale, scale);

            AffineTransformOp scaleOp =
                    new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
            actionImgScaled = scaleOp.filter(actionImg, actionImgScaled);

            //now display the label containing the img
            imgLabel.setIcon(new ImageIcon(actionImgScaled));
            imgLabel.setText("");
            imgLabel.setVisible(true);
        } catch (IOException e) {
            System.out.println("Actionfoto nicht gefunden!");
        }
    }

    @Override
    public void run() {

    }
}
