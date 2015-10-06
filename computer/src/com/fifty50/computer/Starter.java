package com.fifty50.computer;

import com.fifty50.computer.HSVDetector.HSVSelector;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 02.04.15.
 */
public class Starter extends JLayeredPane implements Runnable, ActionListener, KeyListener {

    private Main main;
    private HandDetector detector;
    private JButton start;
    private HandPanel handPanel;
    private java.util.Timer timer = new Timer();
    private int timerIteration = 0;
    private boolean timerRunning = false;
    private String[] argsMain;
    private boolean isRunning, isFinished;
    private Frame frame;
    private String path;
    private int width, height;
    private double smoothing = 0.05, cogSmoothX, cogSmoothY;
    private double lastCogX, lastCogY;
    private Thread handPanelThread;
    private HighscoreLoop highscoreLoop;

    public Starter(Main main, String[] argsMain) {

        this.main = main;
        this.argsMain = argsMain;
        isRunning = true;

        if (argsMain.length < 4) {
            System.out.println("Parameter benötigt: Server adresse, Server port, URL zum Webcam-Stream, Pfad zu den zusätzlichen Dateien, [debug: 'true' oder 'false']\n\n" +
                    "Alternativ: einzige Parameter 'hsvSelector' und Pfad zu den zusätzlichen Dateien, um den HSV Selector zur Auswahl der Handschuhfarbe zu starten");
            System.exit(1);
        }

        path = argsMain[3];
        String hsvPath = path + "hand.txt";

        Toolkit tk = Toolkit.getDefaultToolkit();
        width = tk.getScreenSize().width;
        height = tk.getScreenSize().height;

        ImageIcon icon = new ImageIcon(path + "start.png");
        start = new JButton(icon);
        //put the start button in the center of the screen
        start.setBounds(width / 2 - icon.getIconWidth() / 2, height / 2 - icon.getIconHeight() / 2, icon.getIconWidth(), icon.getIconHeight());
        start.setContentAreaFilled(false);
        start.setBorderPainted(false);
        start.setOpaque(false);
        start.addActionListener(this);
        start.setVisible(true);

        setBackground(Color.GREEN);

        setLayout(null);
        setBackground(Color.RED);
        setBounds(0, 0, width, height);


        //init the hand detector and add the hand panel to the window
        handPanel = new HandPanel(hsvPath, width, height, 0, 0, false, true, Color.BLACK);
        detector = handPanel.getDetector();
        handPanel.setStarter(this);

        String bgImg = "hintergrund";
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

        add(start, 2, 0);

        highscoreLoop = new HighscoreLoop();
        highscoreLoop.init();

        setVisible(true);

        //start the hand detection
        handPanelThread = new Thread(handPanel);
        handPanelThread.start();
        new Thread(this).start();

        addKeyListener(this);
        setFocusable(true);
        requestFocus();
        requestFocusInWindow();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (!isFinished) {
            highscoreLoop.paintLabels(g);

            Graphics2D g2d = (Graphics2D) g;

            Point cog = detector.getCogFlipped();

            cogSmoothX = (cog.x * smoothing) + (cogSmoothX * (1.0 - smoothing));
            cogSmoothY = (cog.y * smoothing) + (cogSmoothY * (1.0 - smoothing));

            g2d.setColor(Color.BLUE);
            g2d.fillOval((int) cogSmoothX - 8, (int) cogSmoothY - 8, 16, 16);

            //check if the cogCircle is inside the start button
            if (start.getBounds().contains(cog)) {

                //check if the timer is already running, else start it
                if (!timerRunning) {
                    start.setIcon(new ImageIcon(path + "start_fokussiert.png"));
                    timerIteration = 0;
                    timer = new Timer();
                    timerRunning = true;

                    //display the "animation" of the button click being prepared
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run() {
                            if (timerIteration < 3) {
                                timerIteration++;
                                start.setIcon(new ImageIcon(path + "start_fokussiert_" + timerIteration + ".png"));
                                repaint();
                            } else {
                                //finished, now perform the button click
                                start.setIcon(new ImageIcon(path + "start_fokussiert_3.png"));
                                repaint();
                                start.doClick();
                                timerRunning = false;
                                timer.cancel();
                            }
                        }
                    }, 1000, 1000);
                }
            } else {
                //reset to the old image and cancel the timer
                start.setIcon(new ImageIcon(path + "start.png"));
                if (timerRunning) {
                    timer.cancel();
                    timerRunning = false;
                }
            }
        }
    }

    public static void main(String[] args) {

        if (args.length == 2 && args[0].contentEquals("hsvSelector")) new HSVSelector(args[1]);
        else new Starter(new Main(args), args);
    }

    @Override
    public void run() {

        while (isRunning) {
            repaint();

            try {
                Thread.sleep(17);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        isFinished = true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        //start the main game screen
        isRunning = false;
        handPanel.closeDown();

        //wait for the hand detector to finish
        while (!handPanel.isFinished() && !isFinished) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }

        //tell the frame to start the game
        frame.switchMode(Frame.Mode.GAME);
    }

    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            //keyboard controls for debugging
            case KeyEvent.VK_W:
                main.forward(Main.Speed.SLOW);
                break;
            case KeyEvent.VK_A:
                main.left(Main.Speed.SLOW);
                break;
            case KeyEvent.VK_S:
                main.backward(Main.Speed.SLOW);
                break;
            case KeyEvent.VK_D:
                main.right(Main.Speed.SLOW);
                break;

            case KeyEvent.VK_UP:
                main.forward(Main.Speed.FAST);
                break;
            case KeyEvent.VK_LEFT:
                main.left(Main.Speed.FAST);
                break;
            case KeyEvent.VK_DOWN:
                main.backward(Main.Speed.FAST);
                break;
            case KeyEvent.VK_RIGHT:
                main.right(Main.Speed.FAST);
                break;

            case KeyEvent.VK_ENTER:
                main.brake();
                break;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W:
            case KeyEvent.VK_UP:
            case KeyEvent.VK_S:
            case KeyEvent.VK_DOWN:
                main.brake();
                break;
            case KeyEvent.VK_A:
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_D:
            case KeyEvent.VK_RIGHT:
                main.straight();
                break;
        }

    }

    public void pause() {

        //stop the repainting
        isFinished = true;

        removeKeyListener(this);

        //stop the handpanel
        handPanel.closeDown();
        try {
            //wait for the panel thread to finish before continuing
            handPanelThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        setVisible(false);
    }

    public void restart() {

        isFinished = false;
        isRunning = true;
        //restart the handpanel and the repainting
        detector = handPanel.getDetector();
        handPanelThread = new Thread(handPanel);
        handPanelThread.start();
        new Thread(this).start();

        //re-init the highscore loop
        highscoreLoop.reset();

        addKeyListener(this);
        setFocusable(true);
        requestFocus();
        setVisible(true);
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    private class HighscoreLoop {

        private double SPEED = 0.1;
        private ArrayList<BufferedImage> top10 = new ArrayList<BufferedImage>();
        private ArrayList<String> scores = new ArrayList<String>();
        private double x_offset;
        private long lastPaint;

        public void init() {

            try {
                //read the ranking file
                BufferedReader reader = new BufferedReader(new FileReader(path + "ranking.txt"));
                String line;
                ArrayList<String> players = new ArrayList<String>();

                while ((line = reader.readLine()) != null) {
                    try {
                        //add the whole line to the list
                        players.add(line);
                    } catch (NumberFormatException ignored) {
                    }
                }

                reader.close();

                //now sort the list
                players.sort(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {

                        //sort by score
                        Integer score1 = Integer.parseInt(o1.split(",")[0]);
                        Integer score2 = Integer.parseInt(o2.split(",")[0]);

                        return score2.compareTo(score1);
                    }
                });

                //fill the array with the top 10 action photos
                for (int i = 0; i < players.size(); i++) {

                    //load the correct image or the null image
                    if (players.get(i).split(",")[1].contentEquals("null")) top10.add(ImageIO.read(new File(path + "null.png")));
                    else top10.add(ImageIO.read(new File(path + File.separator + "actionImgs" + File.separator + players.get(i).split(",")[1])));

                    scores.add(players.get(i).split(",")[0]);
                }

            } catch (IOException e) {
                System.out.println("Fehler beim Erstellen der Top10 Foto-Liste");
            }
        }

        private void paintLabels(Graphics g) {

            Graphics2D g2d = (Graphics2D) g;

            //calculate where the labels should be at the moment
            if (lastPaint == 0) lastPaint = System.currentTimeMillis();

            //increase the x offset
            x_offset += SPEED * (System.currentTimeMillis() - lastPaint);

            g2d.setColor(Color.white);
            g2d.setFont(new Font(null, Font.BOLD, 15));

            //now paint the images and the text where the labels are
            for (int i=0; i<top10.size(); i++) {

                int x = (int) Math.round(width + i*340 - x_offset);
                BufferedImage image = top10.get(i);
                g2d.drawImage(image, x + (320 - image.getWidth())/2, height - 300 + (240 - image.getHeight())/2, image.getWidth(), image.getHeight(), null);

                //draw score and rank centered underneath/above the image
                String score = scores.get(i) + " Punkte";
                g2d.drawString(score, x + 160 - stringLength(score, g2d)/2, height - 340);

                String rank = "Platz " + String.valueOf(i + 1);
                g2d.drawString(rank, x + 160 - stringLength(rank, g2d)/2, height - 40);
            }

            lastPaint = System.currentTimeMillis();

            //reset x_offset once the last image has disappeared
            if (x_offset > width + top10.size()*340) x_offset = 0;
        }

        private int stringLength(String string, Graphics2D g2d) {

            Rectangle2D stringBounds = getFontMetrics(new Font(null, Font.BOLD, 15)).getStringBounds(string, getGraphics());

            return (int) stringBounds.getWidth();
        }

        public void reset() {

            top10.clear();
            scores.clear();

            init();
        }
    }

}
