package com.fifty50.car;

import com.pi4j.io.gpio.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * Created by samuel on 30.01.15.
 */
public class Main {

    private DataOutputStream out;

    public static enum Speed {SLOW, FAST}
    public static enum Mode {FORWARD, BACKWARD, LEFT, RIGHT}

	private ServerSocket serverSocket;
    private GpioController gpio;
    private GpioPinDigitalOutput forward_fast, backward_fast, left_fast, right_fast;
    private GpioPinDigitalOutput forward_slow, backward_slow, left_slow, right_slow;
    private GpioPinDigitalInput inductive_in;

    private boolean finish = false;
    private boolean useFinishSensor;

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
            else if (command.toUpperCase().contentEquals("F")) main.sendFinish();

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

    private void sendFinish() {
        try {
            //notify the client
            out.writeUTF("finish");
            System.out.println("finish gesendet!");
        } catch (IOException e) {
            System.out.println("Konnte Nachricht nicht übermitteln!\n");
            e.printStackTrace();
        }
    }

    public Main() {
        gpio = GpioFactory.getInstance();

        //provision gpio pins 0 to 3 as output pins for fast/strong commands
        forward_fast = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_00, "Forward fast", PinState.LOW);
        backward_fast = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_07, "Backward fast", PinState.LOW);
        left_fast = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02, "Left strong", PinState.LOW);
        right_fast = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_03, "Right strong", PinState.LOW);

        //provision gpio 4 to 7 as output pins for slow/light commands
        forward_slow = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_04, "Forward slow", PinState.LOW);
        backward_slow = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_05, "Backward slow", PinState.LOW);
        left_slow = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06, "Left slow", PinState.LOW);
        right_slow = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_01, "Right slow", PinState.LOW);

        //provision gpio 27 as inductive sensor input pin
        inductive_in = gpio.provisionDigitalInputPin(RaspiPin.GPIO_27, "Inductive in");

        //start listening for sensor input
        new SensorListener().start();
    }

	private void startServer(int port) {
        try {
            new Server(this, port).start();
        } catch (IOException e) {
            System.out.println("Fehler beim Start des Servers auf Port " + port);
            e.printStackTrace();
        }
    }

    public boolean finish() {
        return finish;
    }

    private void forward(Speed speed) {
        String intensity = "";
        //turn on the correct forward pin
        switch (speed) {
            case SLOW:
                if (forward_slow.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("fwd slow high");
                        out.writeUTF("fwd fast low");
                    } catch (IOException ignored) {}
                }
                forward_slow.high();
                forward_fast.low();
                backward_fast.low();
                backward_slow.low();
                intensity = "schwach";
                break;
            case FAST:
                if (forward_fast.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("fwd fast high");
                        out.writeUTF("fwd slow low");
                    } catch (IOException ignored) {}
                }
                forward_fast.high();
                forward_slow.low();
                backward_slow.low();
                backward_fast.low();
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'vorwärts' gesendet (" + intensity + ")");
    }

    private void backward(Speed speed) {
        String intensity = "";
        //turn on the correct backward pin
        switch (speed) {
            case SLOW:
                if (backward_slow.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("bwd slow high");
                        out.writeUTF("bwd fast low");
                    } catch (IOException ignored) {}
                }
                backward_slow.high();
                backward_fast.low();
                forward_slow.low();
                forward_fast.low();
                intensity = "schwach";
                break;
            case FAST:
                if (backward_fast.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("bwd fast high");
                        out.writeUTF("bwd slow low");
                    } catch (IOException ignored) {}
                }
                backward_fast.high();
                backward_slow.low();
                forward_fast.low();
                forward_slow.low();
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'rückwärts' gesendet (" + intensity + ")");
    }

    private void left(Speed speed) {
        String intensity = "";
        //turn on the correct left pin
        switch (speed) {
            case SLOW:
                if (left_slow.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("left slow high");
                        out.writeUTF("left fast low");
                    } catch (IOException ignored) {}
                }
                left_slow.high();
                left_fast.low();
                right_fast.low();
                right_slow.low();
                intensity = "schwach";
                break;
            case FAST:
                if (left_fast.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("left fast high");
                        out.writeUTF("left slow low");
                    } catch (IOException ignored) {}
                }
                left_fast.high();
                left_slow.low();
                right_slow.low();
                right_fast.low();
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'links' gesendet (" + intensity + ")");
    }

    private void right(Speed speed) {
        String intensity = "";
        //turn on the correct right pin
        switch (speed) {
            case SLOW:
                if (right_slow.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("right slow high");
                        out.writeUTF("right fast low");
                    } catch (IOException ignored) {}
                }
                right_slow.high();
                right_fast.low();
                left_fast.low();
                left_slow.low();
                intensity = "schwach";
                break;
            case FAST:
                if (right_fast.isHigh()) return;
                if (out != null) {
                    try {
                        out.writeUTF("right fast high");
                        out.writeUTF("right slow low");
                    } catch (IOException ignored) {}
                }
                right_fast.high();
                right_slow.low();
                left_fast.low();
                left_slow.low();
                intensity = "stark";
                break;
        }
        System.out.println("Befehl 'rechts' gesendet (" + intensity + ")");
    }

    private void brake() {
        //turn off all accelerating pins
        if (out != null) {
            try {
                out.writeUTF("fwd slow low");
                out.writeUTF("fwd fast low");

                out.writeUTF("bwd slow low");
                out.writeUTF("bwd fast low");
            } catch (IOException ignored) {}
        }

        forward_slow.low();
        forward_fast.low();

        backward_slow.low();
        backward_fast.low();
        System.out.println("Bremse...");
    }

    private void straight() {
        //turn off all steering pins
        if (out != null) {
            try {
                out.writeUTF("right slow low");
                out.writeUTF("right fast low");

                out.writeUTF("left slow low");
                out.writeUTF("left fast low");
            } catch (IOException ignored) {}
        }

        right_slow.low();
        right_fast.low();

        left_slow.low();
        left_fast.low();
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
            serverSocket.setReuseAddress(true);
            this.main = main;
        }

        public void run() {
            while (!finish) {
                try {
                    //stop all commands
                    brake();
                    straight();

                    System.out.println("Warte auf Client (Port " +
                            serverSocket.getLocalPort() + ")");

                    Socket client = serverSocket.accept();
                    System.out.println("Verbunden mit "
                            + client.getRemoteSocketAddress());

                    DataInputStream in = new DataInputStream(client.getInputStream());
                    out = new DataOutputStream(client.getOutputStream());

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

                        else if (command.contentEquals("straight")) main.straight();

                        else if (command.contentEquals("enable-finish-sensor")) new SensorListener().start();
                        else if (command.contentEquals("disable-finish-sensor")) useFinishSensor = false;

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

    private class SensorListener extends Thread {

        @Override
        public void run() {

            while (useFinishSensor) {

                if (inductive_in.isLow()) {

                    //if the induction sensor is on 0V it has detected metal => finish line
                    try {
                        //notify the client
                        if (out != null) out.writeUTF("finish");
                    } catch (IOException e) {
                        System.err.println("Konnte Nachricht nicht übermitteln!\n");
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
