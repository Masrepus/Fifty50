package com.masrepus.fifty50.computer;

import java.io.*;
import java.net.Socket;

/**
 * Created by samuel on 02.02.15.
 */
public class Main {

    public static enum Speed {SLOW, FAST}

    private Socket client;
    private DataOutputStream out;

    public Main(String serverName, int port) {
        try {
            System.out.println("Verbinde mit " + serverName + " am Port " + port + "...");
            client = new Socket(serverName, port);
            System.out.println("Verbunden mit " + client.getRemoteSocketAddress());

            OutputStream outToServer = client.getOutputStream();
            out = new DataOutputStream(outToServer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {

        Main main = new Main(args[0], Integer.parseInt(args[1]));

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
        System.out.println("----Warte auf Befehle----");

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

    private void straight() {
        brake();
    }

    private void brake() {
        try {
            out.writeUTF("");
        } catch (IOException e) {
            System.out.println("Fehler beim Senden des Befehls");
        }
    }

    private void right(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case SLOW:
            default:
                command = "L";
                break;
            case FAST:
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

    private void left(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case SLOW:
            default:
                command = "J";
                break;
            case FAST:
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

    private void backward(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case SLOW:
            default:
                command = "K";
                break;
            case FAST:
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

    private void forward(Speed speed) {

        String command;

        //select the command according to the speed
        switch (speed) {

            case SLOW:
            default:
                command = "I";
                break;
            case FAST:
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


}
