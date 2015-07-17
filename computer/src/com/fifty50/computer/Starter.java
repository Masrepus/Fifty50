package com.fifty50.computer;

import com.fifty50.computer.HSVDetector.HSVSelector;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 02.04.15.
 */
public class Starter extends JLayeredPane implements Runnable, ActionListener, KeyListener {

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

    public Starter(String[] argsMain) {

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

        ImageIcon icon = new ImageIcon("/home/samuel/fifty50/start.png");
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
        if (tk.getScreenSize().width/tk.getScreenSize().height == 5/4) bgImg += "_54.png";
        else bgImg += ".png";
        try {
            BackgroundPanel background = new BackgroundPanel(ImageIO.read(new File(path + bgImg)), width, height);
            background.setBounds(0, 0, width, height);
            background.setVisible(true);
            add(background, 1, 0);
        } catch (IOException e) {
            System.out.println("Hintergrundbild nicht gefunden");
        }

        add(start, 2, 0);

        setVisible(true);

        //start the hand detection
        handPanelThread = new Thread(handPanel);
        handPanelThread.start();
        new Thread(this).start();

        addKeyListener(this);
        setFocusable(true);
        requestFocus();
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        if (!isFinished) {
            Graphics2D g2d = (Graphics2D) g;

            Point cog = detector.getCogFlipped();

            cogSmoothX = (cog.x * smoothing) + (cogSmoothX * (1.0 - smoothing));
            cogSmoothY = (cog.y * smoothing) + (cogSmoothY * (1.0 - smoothing));

            g2d.setColor(Color.BLUE);
            g2d.fillOval((int)cogSmoothX - 8, (int)cogSmoothY - 8, 16, 16);

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
        else new Starter(args);
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
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            //start the main game screen
            isFinished = true;
            handPanel.closeDown();

            //wait for the hand detector to finish
            while (!handPanel.isFinished()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
            }

            //tell the frame to start the game
            frame.switchMode(Frame.Mode.GAME);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

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

}
