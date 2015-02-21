package com.fifty50.car.test;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by samuel on 30.01.15.
 */
public class Main {

    public static enum Speed {SLOW, FAST}
    public static enum Mode {FORWARD, BACKWARD, LEFT, RIGHT}

    private ServerSocket serverSocket;
    private boolean finish = false;

    public static void main(String[] args) {

        Main main = new Main();

        System.out.println("----Fifty50 Racing© Raspberry Pi controller gestartet---- \n \n");
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

        //start the server
        main.startServer(Integer.valueOf(args[0]));

        //start listening for console input
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        String command = "";

        while(!main.finish()) {
            try {
                command = console.readLine();
            } catch (IOException e) {
                System.out.println("----FEHLER; Programm beendet...----");
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
            }

            else if (command.toUpperCase().contentEquals("X")) {
                System.out.println("----Programm beendet...----");
                break;
            }
        }
        main.stop();
    }

    private void startServer(int port) {
        try {
            new Server(this, port).start();
        } catch (IOException e) {
            System.out.println("Fehler beim Start des Servers auf Port " + port);
        }
    }

    public boolean finish() {
        return finish;
    }

    private void forward(Speed speed) {
        String intensity = "";
        switch (speed) {
            case SLOW:
                intensity = "schwach";
                break;
            case FAST:
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'vorwärts' gesendet (" + intensity + ")");
    }

    private void backward(Speed speed) {
        String intensity = "";
        switch (speed) {
            case SLOW:
                intensity = "schwach";
                break;
            case FAST:
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'rückwärts' gesendet (" + intensity + ")");
    }

    private void left(Speed speed) {
        String intensity = "";
        switch (speed) {
            case SLOW:
                intensity = "schwach";
                break;
            case FAST:
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'links' gesendet (" + intensity + ")");
    }

    private void right(Speed speed) {
        String intensity = "";
        switch (speed) {
            case SLOW:
                intensity = "schwach";
                break;
            case FAST:
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'rechts' gesendet (" + intensity + ")");
    }

    private void brake() {
        System.out.println("Bremse...");
    }

    private void straight() {
        System.out.println("Lenken beendet");
    }

    public void stop() {
        System.exit(1);
    }

    public class Server extends Thread {

        private ServerSocket serverSocket;
        private Main main;

        public Server(Main main, int port) throws IOException
        {
            serverSocket = new ServerSocket(port);
            this.main = main;
        }

        public void run() {
            while (!finish) {
                try {
                    System.out.println("Warte auf Client (Port " +
                            serverSocket.getLocalPort() + ")");

                    Socket client = serverSocket.accept();
                    System.out.println("Verbunden mit "
                            + client.getRemoteSocketAddress());

                    DataInputStream in = new DataInputStream(client.getInputStream());
                    DataOutputStream out = new DataOutputStream(client.getOutputStream());

                    //wait for commands from the client
                    String command = "";
                    while (!client.isClosed()) {
                        command = in.readUTF();

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
                            finish = true;
                            break;
                        }
                    }
                } catch (SocketTimeoutException s) {}
                catch (IOException e) {}
            }
        }
    }
}
