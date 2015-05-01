package com.fifty50.computer;

import com.charliemouse.cambozola.Viewer;

import javax.swing.*;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.util.*;
import java.util.Timer;

/**
 * Created by samuel on 02.02.15.
 */
public class Main extends JFrame implements OnCalibrationFininshedListener {

    private GestureDetector detector;
    private DataOutputStream out;
    private GameWindow window;
    private HandPanel handPanel;
    private GameHandler handler;

    public static void main(String[] args) {

        if (args.length < 4) {
            System.out.println("Parameter benötigt: Server adresse, Server port, URL zum Webcam-Stream, Pfad zur .txt Datei mit HSV-Werten zur Handschuherkennung, [debug: 'true' oder 'false']");
            System.exit(1);
        }

        new Main(args);
    }

    public Main(String[] args) {

        super("Fifty50 Racing");

        String hsvPath = args[3];
        boolean debug = false;
        if (args.length == 5) debug = Boolean.parseBoolean(args[4]);

        window = new GameWindow();
        window.init(this);

        //init the game handler
        handler = new GameHandler(this);

        //add the livestream panel
        Toolkit tk = Toolkit.getDefaultToolkit();
        int width = tk.getScreenSize().width;
        int height = tk.getScreenSize().height;

        window.panel1.setBounds(0, 0, width, height / 2);
        add(window.panel1);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);

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
        Viewer viewer = new Viewer(width, height / 2 - 5, handler);
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
        window.panel1.add(viewer);

        //init the gesture detection
        handPanel = new HandPanel(hsvPath, 640, height / 2 -5, 320, height / 2 + 5, debug, false, Color.WHITE);
        handPanel.setGameHandler(handler);
        handPanel.setBounds(320, height / 2 + 5, width, height / 2 - 5);
        handPanel.setFocusable(true);
        handPanel.requestFocus();
        handPanel.addKeyListener(window);

        detector = new GestureDetector(this, handPanel, handler);
        detector.start();

        add(handPanel);

        //go fullscreen
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        //device.setFullScreenWindow(this);

        //everything is ready, show the jframe
        pack();
        setVisible(true);
        viewer.init();

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

    public void connectToServer(String serverName, int port) {
        new Connector(serverName, port).start();
    }

    public void straight() {
        brake();
    }

    public void brake() {
        try {
            out.writeUTF("");
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
                command = "L";
                break;
            case SLOW:
                command = "D";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
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
                command = "J";
                break;
            case SLOW:
                command = "A";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
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
                command = "K";
                break;
            case SLOW:
                command = "S";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
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
                command = "I";
                break;
            case SLOW:
                command = "W";
                break;
        }

        try {
            //send the command to the car
            out.writeUTF(command);
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
    public void paintAll(Graphics g) {
        super.paintAll(g);
        handler.paint((Graphics2D) g);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        //pass this to the game handler in case it needs to display something
        handler.paint((Graphics2D) g);
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

            //try to connect to the car; sleep and retry if connection fails
            boolean connected = false;

            while (!connected) {
                try {
                    System.out.println("Verbinde mit " + serverName + " am Port " + port + "...");
                    Socket client = new Socket(serverName, port);
                    System.out.println("Verbunden mit " + client.getRemoteSocketAddress());

                    OutputStream outToServer = client.getOutputStream();
                    out = new DataOutputStream(outToServer);
                    connected = true;
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
}
