package com.fifty50.computer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.Timer;

/**
 * Created by samuel on 02.04.15.
 */
public class Starter extends JFrame implements Runnable {

    private static final int COG_SMOOTHING_VALUE = 1;
    private static final int DELAY = 20;

    private HandDetector detector;
    private JButton start;
    private HandPanel handPanel;
    private int cogSmoothX = 0;
    private int cogSmoothY = 0;
    private java.util.Timer timer;
    private int timerIteration = 0;
    private boolean timerRunning = false;

    public Starter(String hsvPath) {

        super("Fifty50 Racing");

        Toolkit tk = Toolkit.getDefaultToolkit();
        int width = tk.getScreenSize().width;
        int height = tk.getScreenSize().height;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(getExtendedState()|MAXIMIZED_BOTH);

        ImageIcon icon = new ImageIcon("/home/samuel/fifty50/start.png");
        start = new JButton(icon);
        //put the start button in the center of the screen
        start.setBounds(width/2 - icon.getIconWidth()/2, height/2 - icon.getIconHeight()/2, icon.getIconWidth(), icon.getIconHeight());
        start.setContentAreaFilled(false);
        start.setBorderPainted(false);
        start.setOpaque(false);
        //start.setRolloverEnabled(true);
        //start.setRolloverIcon(new ImageIcon("/home/samuel/fifty50/start_fokussiert.png"));

        getContentPane().setBackground(Color.BLACK);
        JLayeredPane root = new JLayeredPane();
        root.setLayout(null);
        root.setBackground(Color.BLACK);
        root.setBounds(0, 0, width, height);
        add(root);

        //init the hand detector and add the hand panel to the window
        handPanel = new HandPanel(hsvPath, width, height, 0, 0, false, true, Color.BLACK);
        detector = handPanel.getDetector();
        /*handPanel.setOpaque(false);
        handPanel.setBounds(0, 0, width, height);

        detector = new HandDetector(hsvPath, width, height, 0, 0);

        root.add(handPanel, 2, 0);*/
        root.add(start, 1, 0);

        //start the hand detection
        new Thread(handPanel).start();
        new Thread(this).start();

        pack();
        setVisible(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);

        Graphics2D g2d = (Graphics2D) g;

        //draw the cog point
        Point cog = cogMovingAverage(detector.getCogFlipped());
        g2d.setColor(Color.BLUE);
        g2d.fillOval(cog.x - 8, cog.y - 8, 16, 16);

        //check if the cog is inside the start button
        if (start.getBounds().contains(cog)) {

            //check if the timer is already running, else start it
            if (!timerRunning) {
                start.setIcon(new ImageIcon("/home/samuel/fifty50/start_fokussiert.png"));
                timerIteration = 0;
                timer = new Timer();
                timerRunning = true;

                //display the "animation" of the button click being prepared
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        if (timerIteration <= 3) {
                            timerIteration++;
                            start.setIcon(new ImageIcon("/home/samuel/fifty50/start_fokussiert_" + timerIteration + ".png"));
                            repaint();
                        } else {
                            //finished, now perform the button click
                            start.setIcon(new ImageIcon("/home/samuel/fifty50/start_fokussiert_3.png"));
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
            start.setIcon(new ImageIcon("/home/samuel/fifty50/start.png"));
            timerRunning = false;
            timer.cancel();
        }
    }

    public Point cogMovingAverage(Point cog) {
        int x = (COG_SMOOTHING_VALUE * cogSmoothX + cog.x) / (COG_SMOOTHING_VALUE + 1);
        int y = (COG_SMOOTHING_VALUE * cogSmoothY + cog.y) / (COG_SMOOTHING_VALUE + 1);

        cogSmoothX = x;
        cogSmoothY = y;

        return new Point(x, y);
    }

    public static void main(String[] args) {
        new Starter(args[0]);
    }

    @Override
    public void run() {

        while (true) {
            repaint();

            try {
                Thread.sleep(DELAY);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
