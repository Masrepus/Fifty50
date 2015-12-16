package com.fifty50.computer;

import com.charliemouse.cambozola.Viewer;

import javax.swing.*;
import javax.swing.plaf.basic.BasicInternalFrameUI;
import java.applet.AppletContext;
import java.applet.AppletStub;
import java.awt.*;
import java.awt.geom.Rectangle2D;
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
    private JInternalFrame popup;
    private Connector connector;
    private FinishDetector finishDetector;

    private volatile Car.PinState fwdFast, fwdSlow, bwdFast, bwdSlow, leftFast, leftSlow, rightFast, rightSlow;
    private boolean keyboardMode = true;
    private JOptionPane pane;
    private JInternalFrame keyboardPopup;

    public Main(String[] args) {
        this.args = args;
        fwdFast = fwdSlow = bwdFast = bwdSlow = leftFast = leftSlow = rightFast = rightSlow = Car.PinState.LOW;
    }

    public void init() {

        hasRun = true;

        path = args[3] + File.separator;
        String hsvPath = path + "hand.txt";
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
        handPanel.setBackground(Color.BLACK);
        handPanel.setFrame(frame);

        add(handPanel, 1);
        revalidate();

        finishDetector = new FinishDetector(this, viewer, 640, 480, path + "finish.txt");
    }

    public void pause() {
        //stop everything and join the threads for save stopping
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
            detector.getDetectorThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        viewer.stop();
        try {
            //wait for the viewer to close
            viewer.getStreamerThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        viewer.clearStreamer();

        finishDetector.stop();
        try {
            //wait for the detector to close
            finishDetector.getAnalyzerThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        setVisible(false);
    }

    public void start() {

        //init the GestureDetector and start everything
        if (popup != null) popup.dispose();
        if (keyboardPopup != null) keyboardPopup.dispose();
        detector = new GestureDetector(this, handPanel, handler);
        handPanel.setVisible(true);
        setVisible(true);

        //only start the gesture detector if keyboard mode is inactive! Else the detector overrides the commands the keyboard actions send to the car
        if (!keyboardMode) detector.startThread(new Thread(detector));
        else detector.disable(new Thread(detector));

        viewer.init();

        //immediately start calibration
        requestCalibration();

        repaint();
        frame.repaint();
    }

    public void restart() {

        //restart the hand panel and the other components
        if (popup != null) popup.dispose();
        handPanel.setIsCalibrated(false);
        handPanel.startHandPanelThread(new Thread(handPanel));
        handPanel.setVisible(true);
        finishDetector.start();

        setVisible(true);

        detector.startThread(new Thread(detector));
        viewer.init();

        handler.reset();

        repaint();
        frame.repaint();

        requestCalibration();
    }

    public void connectToServer(String serverName, int port) {
        connector = new Connector(serverName, port);
        connector.start();
        //start listening for server messages
        new ServerListener().start();
    }

    public void straight() {
        if (leftFast == Car.PinState.LOW && rightFast == Car.PinState.LOW && leftSlow == Car.PinState.LOW && rightSlow == Car.PinState.LOW) return;
        try {
            out.writeUTF("straight");
            leftFast = rightFast = leftSlow = rightSlow = Car.PinState.LOW;
            System.out.println("gerade");
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls");
        }
    }

    public void brake() {
        if (fwdFast == Car.PinState.LOW && fwdSlow == Car.PinState.LOW && bwdSlow == Car.PinState.LOW && bwdFast == Car.PinState.LOW) return;
        try {
            out.writeUTF("");
            fwdSlow = fwdFast = bwdSlow = bwdFast = Car.PinState.LOW;
            System.out.println("bremsen");
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls");
        }
    }

    public void right(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (rightFast == Car.PinState.HIGH) return;
                rightFast = Car.PinState.HIGH;
                rightSlow = leftFast = leftSlow = Car.PinState.LOW;
                command = "L";
                break;
            case SLOW:
                if (rightSlow == Car.PinState.HIGH) return;
                rightSlow = Car.PinState.HIGH;
                rightFast = leftFast = leftSlow = Car.PinState.LOW;
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

    public void left(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (leftFast == Car.PinState.HIGH) return;
                leftFast = Car.PinState.HIGH;
                leftSlow = rightFast = rightSlow = Car.PinState.LOW;
                command = "J";
                break;
            case SLOW:
                if (leftSlow == Car.PinState.HIGH) return;
                leftSlow = Car.PinState.HIGH;
                leftFast = rightFast = rightSlow = Car.PinState.LOW;
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

    public void backward(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (bwdFast == Car.PinState.HIGH) return;
                bwdFast = Car.PinState.HIGH;
                bwdSlow = fwdSlow = fwdFast = Car.PinState.LOW;
                command = "K";
                break;
            case SLOW:
                if (bwdSlow == Car.PinState.HIGH) return;
                bwdSlow = Car.PinState.HIGH;
                bwdFast = fwdSlow = fwdFast = Car.PinState.LOW;
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

    public void forward(Car.Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case FAST:
            default:
                if (fwdFast == Car.PinState.HIGH) return;
                fwdFast = Car.PinState.HIGH;
                fwdSlow = bwdFast = bwdSlow = Car.PinState.LOW;
                command = "I";
                break;
            case SLOW:
                if (fwdSlow == Car.PinState.HIGH) return;
                fwdSlow = Car.PinState.HIGH;
                fwdFast = bwdSlow = bwdFast = Car.PinState.LOW;
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
            if (command.toUpperCase().contentEquals("W")) forward(Car.Speed.SLOW);
            else if (command.toUpperCase().contentEquals("S")) backward(Car.Speed.SLOW);
            else if (command.toUpperCase().contentEquals("A")) left(Car.Speed.SLOW);
            else if (command.toUpperCase().contentEquals("D")) right(Car.Speed.SLOW);

            else if (command.toUpperCase().contentEquals("I")) forward(Car.Speed.FAST);
            else if (command.toUpperCase().contentEquals("K")) backward(Car.Speed.FAST);
            else if (command.toUpperCase().contentEquals("J")) left(Car.Speed.FAST);
            else if (command.toUpperCase().contentEquals("L")) right(Car.Speed.FAST);

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

    public FinishDetector getFinishDetector() {
        return finishDetector;
    }

    public void disableGestureDetector() {
        detector.close();

        //disable it every round until this is changed
        keyboardMode = true;
    }

    public void enableGestureDetector() {
        //start gesture detector next round
        keyboardMode = false;
    }

    public void notifyKeyboardModeActive() {
        if (keyboardPopup != null) keyboardPopup.dispose();

        //now show an updated popup with keyboard mode active
        showKeyboardPopup("Tastaturmodus");
    }

    public void showKeyboardPopup(String message) {

        //wait until frame is shown
        while (!frame.isVisible()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {}
        }

        if (pane == null) pane = new JOptionPane("", JOptionPane.PLAIN_MESSAGE,
                JOptionPane.DEFAULT_OPTION, null, null, null);

        //if there is already an old popup, dispose it
        if (keyboardPopup != null) keyboardPopup.dispose();

        keyboardPopup = pane.createInternalFrame(Main.this, "");
        ((BasicInternalFrameUI) keyboardPopup.getUI()).setNorthPane(null);
        keyboardPopup.setBackground(Color.BLACK);

        //set up the label that will display the message
        JLabel label = new JLabel(message, JLabel.CENTER);
        label.setForeground(Color.WHITE);
        label.setBackground(Color.BLACK);
        label.setOpaque(true);

        Rectangle2D stringBounds = label.getFontMetrics(label.getFont()).getStringBounds(message, null);
        int stringLen = (int) stringBounds.getWidth();
        int stringHeight = (int) stringBounds.getHeight();

        keyboardPopup.getRootPane().removeAll();
        keyboardPopup.getRootPane().add(label);
        //center the label
        label.setBounds(17, (50-stringHeight)/2, stringLen, stringHeight);

        //bottom left corner
        keyboardPopup.setBounds(20, height - 70, stringLen + 40, 50);
        label.setVisible(true);
        keyboardPopup.setVisible(true);
    }

    private class Connector extends Thread {

        private String serverName;
        private int port;
        private boolean connected;
        private Socket client;

        public Connector(String serverName, int port) {
            this.serverName = serverName;
            this.port = port;
        }

        @Override
        public void run() {

            //try to establish a two-way connection to the car; sleep and retry if connection fails
            connected = false;

            while (!connected) {
                try {
                    System.out.println("Verbinde mit " + serverName + " am Port " + port + "...");
                    client = new Socket(serverName, port);
                    System.out.println("Verbunden mit " + client.getRemoteSocketAddress());

                    OutputStream outToServer = client.getOutputStream();
                    out = new DataOutputStream(outToServer);

                    InputStream inFromServer = client.getInputStream();
                    in = new DataInputStream(inFromServer);
                    connected = true;

                    //show a "connected" internal frame popup
                    showPopup("Verbunden mit Auto: " + client.getRemoteSocketAddress());
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

        public boolean isConnected() {
            return connected;
        }

        public Socket getClient() {
            return client;
        }

        public void showPopup(String message) {

            //wait until frame is shown
            while (!frame.isVisible()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ignored) {}
            }

            if (pane == null) pane = new JOptionPane("", JOptionPane.PLAIN_MESSAGE,
                    JOptionPane.DEFAULT_OPTION, null, null, null);

            //if there is already an old popup, dispose it
            if (popup != null) popup.dispose();

            popup = pane.createInternalFrame(Main.this, "");
            ((BasicInternalFrameUI) popup.getUI()).setNorthPane(null);
            popup.setBackground(Color.BLACK);

            //set up the label that will display the message
            JLabel label = new JLabel(message, JLabel.CENTER);
            label.setForeground(Color.WHITE);
            label.setBackground(Color.BLACK);
            label.setOpaque(true);

            Rectangle2D stringBounds = label.getFontMetrics(label.getFont()).getStringBounds(message, null);
            int stringLen = (int) stringBounds.getWidth();
            int stringHeight = (int) stringBounds.getHeight();

            popup.getRootPane().removeAll();
            popup.getRootPane().add(label);
            //center the label
            label.setBounds(17, (50-stringHeight)/2, stringLen, stringHeight);

            popup.setBounds(width/2 - stringLen/2 - 20, 5, stringLen + 40, 50);
            label.setVisible(true);
            popup.setVisible(true);
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
                    else if (message.contains("fwd fast")) fwdFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("fwd slow")) fwdSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd fast")) bwdFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("bwd slow")) bwdSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("left fast")) leftFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("left slow")) leftSlow = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("right fast")) rightFast = Car.PinState.parse(message.split("\\s+")[2]);
                    else if (message.contains("right slow")) rightSlow = Car.PinState.parse(message.split("\\s+")[2]);

                    System.out.println("Nachricht vom Auto: " + message);

                } catch (Exception ignored) {}
            }
        }
    }
}
