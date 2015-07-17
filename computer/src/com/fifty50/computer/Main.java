package com.fifty50.computer;

import com.charliemouse.cambozola.Viewer;
import com.googlecode.javacv.FrameGrabber;

import javax.swing.*;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 02.02.15.
 */
public class Main extends JPanel implements OnCalibrationFininshedListener, Runnable {

    private GestureDetector detector;
    private DataOutputStream out;
    private DataInputStream in;
    private GameWindow window;
    private HandPanel handPanel;
    private GameHandler handler;
    private String[] args;
    private Viewer viewer;
    private Frame frame;
    private boolean hasRun = false;
    private int width, height;
    private String path;
    private enum State {LOW, HIGH;
        public static State parse(String s) {
            return (s.toUpperCase().contentEquals("LOW")) ? LOW : HIGH;
        }
    }
    private volatile State fwdFast, fwdSlow, bwdFast, bwdSlow, leftFast, leftSlow, rightFast, rightSlow;

    public Main(String[] args) {
        this.args = args;
        fwdFast = fwdSlow = bwdFast = bwdSlow = leftFast = leftSlow = rightFast = rightSlow = State.LOW;
    }

    public void init() {

        hasRun = true;

        path = args[3];
        String hsvPath = args[3] + "hand.txt";
        boolean debug = false;
        if (args.length == 5) debug = Boolean.parseBoolean(args[4]);

        window = new GameWindow();
        window.init(this);

        //init the game handler
        handler = new GameHandler(this, args[3]);

        //add the livestream panel
        Toolkit tk = Toolkit.getDefaultToolkit();
        width = tk.getScreenSize().width;
        height = tk.getScreenSize().height;

        setLayout(null);
        window.panel1.setLayout(null);
        window.panel1.setBounds(0, 0, width, height / 2);
        //add(window.panel1);

        System.out.println("----Fifty50 Racing© Fernsteuerung gestartet---- \n \n");
        System.out.println("Befehle: \n" +
                " \n" +
                "LANGSAM: \n" +
                "'W' = vorwärts (Pin 8 rechts)\n" +
                "'S' = rückwärts (Pin 9 rechts)\n" +
                "'A' = links (Pin 11 rechts)\n" +
                "'D' = rechts (Pin 6 rechts)\n" +
                " \n" +
                "SCHNELL: \n" +
                "'I' = vorwärts (Pin 6 links)\n" +
                "'K' = rückwärts (Pin 4 links)\n" +
                "'J' = links (Pin 7 links)\n" +
                "'L' = rechts (Pin 8 links)\n" +
                " \n" +
                "Bremsen: 'ENTER' \n \n" +
                "Zum beenden 'X' drücken \n \n");
        System.out.println("----Warte auf Befehle---- \n");

        //connect to the car
        connectToServer(args[0], Integer.parseInt(args[1]));

        //create a new instance of the cambozola mjpg player applet for the live stream
        final String url = args[2];
        viewer = new Viewer(width, height / 2 - 5, handler);
        AppletStub stub = new AppletStub() {
            @Override
            public boolean isActive() {
                return true;
            }

            @Override
            public URL getDocumentBase() {
                URL base = null;
                try {
                    base = new URL(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return base;
            }

            @Override
            public URL getCodeBase() {
                URL base = null;
                try {
                    base = new URL(url);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
                return base;
            }

            @Override
            public String getParameter(String name) {
                if (name.contentEquals("url")) return url;
                else return "";
            }

            @Override
            public AppletContext getAppletContext() {
                return null;
            }

            @Override
            public void appletResize(int width, int height) {

            }
        };
        viewer.setStub(stub);
        viewer.setBounds(0, 0, width, height / 2 - 5);
        add(viewer, 0);
        revalidate();

        //init the gesture detection
        handPanel = new HandPanel(hsvPath, 640, height / 2 - 5, width / 2 - 320, height / 2 + 5, debug, false, Color.WHITE);
        handPanel.setGameHandler(handler);
        handPanel.setBounds(0, height / 2 + 5, width, height / 2 - 5);
        handPanel.addKeyListener(window);
        handPanel.setFrame(frame);

        add(handPanel, 1);
        revalidate();
    }

    public void pause() {
        //stop everything
        handPanel.closeDown();
        try {
            //wait for the panel to close
            handPanel.getHandPanelThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        detector.close();
        try {
            //wait for the detector to close
            detector.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        viewer.stop();
        setVisible(false);
    }

    public void start() {

        //init the GestureDetector and start everything
        detector = new GestureDetector(this, handPanel, handler);
        handPanel.setVisible(true);
        setVisible(true);
        detector.start();
        viewer.init();
        handPanel.setFocusable(true);
        handPanel.requestFocus();

        repaint();
        frame.repaint();
    }

    public void restart() {

        //restart the hand panel and the other components
        handPanel.setIsCalibrated(false);
        new Thread(handPanel).start();
        handPanel.setVisible(true);

        setVisible(true);

        new Thread(detector).start();
        viewer.init();
        handPanel.setFocusable(true);
        handPanel.requestFocus();

        handler.reset();

        repaint();
        frame.repaint();
    }

    public void connectToServer(String serverName, int port) {
        new Connector(serverName, port).start();
        //start listening for server messages
        new ServerListener().start();
    }

    public void straight() {
        if (leftFast == State.LOW && rightFast == State.LOW && leftSlow == State.LOW && rightSlow == State.LOW) return;
        try {
            out.writeUTF("straight");
            leftFast = rightFast = leftSlow = rightSlow = State.LOW;
            System.out.println("gerade");
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls");
        }
    }

    public void brake() {
        if (fwdFast == State.LOW && fwdSlow == State.LOW && bwdSlow == State.LOW && bwdFast == State.LOW) return;
        try {
            out.writeUTF("");
            fwdSlow = fwdFast = bwdSlow = bwdFast = State.LOW;
            System.out.println("bremsen");
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls");
        }
    }

    public void right(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (rightFast == State.HIGH) return;
                rightFast = State.HIGH;
                rightSlow = leftFast = leftSlow = State.LOW;
                command = "L";
                break;
            case SLOW:
                if (rightSlow == State.HIGH) return;
                rightSlow = State.HIGH;
                rightFast = leftFast = leftSlow = State.LOW;
                command = "D";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            System.out.println(command);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls " + command);
        }
    }

    public void left(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (leftFast == State.HIGH) return;
                leftFast = State.HIGH;
                leftSlow = rightFast = rightSlow = State.LOW;
                command = "J";
                break;
            case SLOW:
                if (leftSlow == State.HIGH) return;
                leftSlow = State.HIGH;
                leftFast = rightFast = rightSlow = State.LOW;
                command = "A";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            System.out.println(command);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls " + command);
        }
    }

    public void backward(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (bwdFast == State.HIGH) return;
                bwdFast = State.HIGH;
                bwdSlow = fwdSlow = fwdFast = State.LOW;
                command = "K";
                break;
            case SLOW:
                if (bwdSlow == State.HIGH) return;
                bwdSlow = State.HIGH;
                bwdFast = fwdSlow = fwdFast = State.LOW;
                command = "S";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            System.out.println(command);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls " + command);
        }
    }

    public void forward(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (fwdFast == State.HIGH) return;
                fwdFast = State.HIGH;
                fwdSlow = bwdFast = bwdSlow = State.LOW;
                command = "I";
                break;
            case SLOW:
                if (fwdSlow == State.HIGH) return;
                fwdSlow = State.HIGH;
                fwdFast = bwdSlow = bwdFast = State.LOW;
                command = "W";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
            System.out.println(command);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls " + command);
        }
    }

    public void requestCalibration() {
        //wait for 3 seconds
        final java.util.Timer timer = new Timer();
        final int[] count = new int[1];
        handPanel.setExtraMsg("Kalibrieren in 3");
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (count[0] < 3) {
                    //display a countdown message
                    count[0]++;
                    handPanel.setExtraMsg("Kalibrieren in " + String.valueOf(2 - (count[0] / 2)));
                } else {
                    //the calibration button was pressed, pass this to the gesture detector
                    handPanel.setExtraMsg("Kalibrieren...");
                    timer.cancel();
                    detector.calibrate(Main.this, handler);
                }
            }
        }, 1000, 1000);
    }

    @Override
    public void calibrationFinished(Point center) {
        //handPanel.setExtraMsg("Mittelpunkt: (" + center.x + "/" + center.y + ")");
        handPanel.setExtraMsg("");
        handPanel.setIsCalibrated(true);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public void setFrame(Frame frame) {
        this.frame = frame;
    }

    public Frame getFrame() {
        return frame;
    }

    public GameHandler getHandler() {
        return handler;
    }

    @Override
    public void run() {

        //start listening for console input
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        String command;
        while (true) {
            try {
                command = console.readLine();
            } catch (IOException e) {
                System.out.println("----FEHLER; beende das Programm...----");
                break;
            }

            //check if this is a legitimate command
            if (command.toUpperCase().contentEquals("W")) forward(Speed.SLOW);
            else if (command.toUpperCase().contentEquals("S")) backward(Speed.SLOW);
            else if (command.toUpperCase().contentEquals("A")) left(Speed.SLOW);
            else if (command.toUpperCase().contentEquals("D")) right(Speed.SLOW);

            else if (command.toUpperCase().contentEquals("I")) forward(Speed.FAST);
            else if (command.toUpperCase().contentEquals("K")) backward(Speed.FAST);
            else if (command.toUpperCase().contentEquals("J")) left(Speed.FAST);
            else if (command.toUpperCase().contentEquals("L")) right(Speed.FAST);

            else if (command.contentEquals("")) {
                brake();
                straight();
            } else if (command.toUpperCase().contentEquals("X")) {
                System.out.println("----Beende das Programm...----");
                break;
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(width, height);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(width, height);
    }

    public HandPanel getHandpanel() {
        return handPanel;
    }

    public String getPath() {
        return path;
    }

    public enum Speed {SLOW, FAST}

    private class Connector extends Thread {

        String serverName;
        int port;

        public Connector(String serverName, int port) {
            this.serverName = serverName;
            this.port = port;
        }

        @Override
        public void run() {

            //try to establish a two-way connection to the car; sleep and retry if connection fails
            boolean connected = false;

            while (!connected) {
                try {
                    System.out.println("Verbinde mit " + serverName + " am Port " + port + "...");
                    Socket client = new Socket(serverName, port);
                    System.out.println("Verbunden mit " + client.getRemoteSocketAddress());

                    OutputStream outToServer = client.getOutputStream();
                    out = new DataOutputStream(outToServer);

                    InputStream inFromServer = client.getInputStream();
                    in = new DataInputStream(inFromServer);
                    connected = true;

                    //message!
                    JOptionPane.showMessageDialog(Main.this, "Verbunden mit " + client.getRemoteSocketAddress());
                } catch (IOException e) {
                    System.out.println("Verbindung fehlgeschlagen, warte...");
                    try {
                        sleep(100);
                    } catch (InterruptedException ignored) {
                    }
                    connected = false;
                }
            }
        }
    }

    private class ServerListener extends Thread {

        @Override
        public void run() {

            //listen for incoming messages from the car
            String message;
            while(true) {

                try {
                    message = in.readUTF();

                    if (message.contentEquals("finish")) {
                        System.out.println("finish empfangen!");
                        handler.gameFinished();
                    }

                    //listen for pin state changes
                    else if (message.contains("fwd fast")) fwdFast = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("fwd slow")) fwdSlow = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd fast")) bwdFast = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd slow")) bwdSlow = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("left fast")) leftFast = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("left slow")) leftSlow = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("right fast")) rightFast = Main.State.parse(message.split("\\s+")[2]);
                    else if (message.contains("right slow")) rightSlow = Main.State.parse(message.split("\\s+")[2]);

                } catch (Exception ignored) {}
            }
        }
    }
}
