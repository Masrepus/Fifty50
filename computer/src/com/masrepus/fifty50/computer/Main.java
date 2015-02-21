package com.masrepus.fifty50.computer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;

/**
 * Created by samuel on 02.02.15.
 */
public class Main {

    public static enum Speed {SLOW, FAST}

    private DataOutputStream out;
    private GameWindow window;
    private Socket client;
    private OutputStream outToServer;

    public void attachWindow(GameWindow window) {
        this.window = window;
    }

    public static void main(String[] args) {

        Main main = null;
        try {
            main = new Main();
        } catch (Exception e) {
            System.out.println("Parameter benötigt: Server adresse, Server port");
            System.exit(1);
        }

        GameWindow window = new GameWindow();
        window.init(main);
        JFrame frame = new JFrame("GameWindow");
        frame.setContentPane(window.panel1);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setExtendedState(Frame.MAXIMIZED_BOTH);
        frame.pack();
        frame.setVisible(true);
        main.attachWindow(window);

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
        main.connectToServer(args[0], Integer.parseInt(args[1]));

        try {
            //create a mediaplayer for the live stream

        } catch (Exception e) {
            System.out.println("Fehler beim Abspielen des Streams! " + e.getClass());
        }

        //start listening for console input
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));

        String command = "";
        while (true) {
            try {
                command = console.readLine();
            } catch (IOException e) {
                System.out.println("----FEHLER; beende das Programm...----");
                break;
            }

            //check if this is a legitimate command
            if (command.toUpperCase().contentEquals("W")) main.forward(Speed.SLOW);
            else if (command.toUpperCase().contentEquals("S")) main.backward(Speed.SLOW);
            else if (command.toUpperCase().contentEquals("A")) main.left(Speed.SLOW);
            else if (command.toUpperCase().contentEquals("D")) main.right(Speed.SLOW);

            else if (command.toUpperCase().contentEquals("I")) main.forward(Speed.FAST);
            else if (command.toUpperCase().contentEquals("K")) main.backward(Speed.FAST);
            else if (command.toUpperCase().contentEquals("J")) main.left(Speed.FAST);
            else if (command.toUpperCase().contentEquals("L")) main.right(Speed.FAST);

            else if (command.contentEquals("")) {
                main.brake();
                main.straight();
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
            window.changeCommandText("Bremsen");
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
            window.changeCommandText("Befehl: " + command);
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
            window.changeCommandText("Befehl: " + command);
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
            window.changeCommandText("Befehl: " + command);
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
            window.changeCommandText("Befehl: " + command);
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls " + command);
        }
    }

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
                    client = new Socket(serverName, port);
                    System.out.println("Verbunden mit " + client.getRemoteSocketAddress());

                    outToServer = client.getOutputStream();
                    out = new DataOutputStream(outToServer);
                    connected = true;
                } catch (IOException e) {
                    System.out.println("Verbindung fehlgeschlagen, warte...");
                    try {
                        sleep(100);
                    } catch (InterruptedException e1) {}
                    connected = false;
                }
            }
        }
    }
}
