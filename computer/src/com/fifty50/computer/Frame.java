package com.fifty50.computer;

import javax.swing.*;
import java.awt.*;

/**
 * Created by samuel on 02.05.15.
 */
public class Frame extends JFrame {

    private int width, height;
    private String[] args;
    private Starter starter;
    private Main main;
    private boolean gameHasRun;

    public enum Mode {STARTSCREEN, GAME, GAMEOVER}

    public Frame(/*Starter starter,*/ String[] args) {

        super("Fifty50 Racing");

        this.args = args;

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setExtendedState(getExtendedState() | Frame.MAXIMIZED_BOTH);

        //display the start screen panel
        Toolkit tk = Toolkit.getDefaultToolkit();
        width = tk.getScreenSize().width;
        height = tk.getScreenSize().height;

        //init the start screen
        starter = new Starter(args);
        starter.setBounds(0, 0, width, height);
        starter.setVisible(true);
        starter.setFrame(this);
        getContentPane().add(starter);

        //init the main screen but leave it invisible
        main = new Main(args);
        main.setBounds(0, 0, width, height);
        main.setVisible(false);
        main.setFrame(this);
        getContentPane().add(main);

        setUndecorated(true);
        pack();
        getContentPane().revalidate();
        getContentPane().repaint();

        //go fullscreen
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        device.setFullScreenWindow(this);

        setVisible(true);
    }

    public void switchMode(Mode mode) {

        switch (mode) {

            case GAME:
                //start the game
                starter.setVisible(false);
                starter.pause();
                main.setVisible(true);
                if (gameHasRun) main.restart();
                else main.start();

                gameHasRun = true;
                break;
            case STARTSCREEN:
                main.setVisible(false);
                main.pause();
                starter.setVisible(true);
                starter.restart();
        }
    }

    public static void main(String[] args) {
        new Frame(args);
    }
}
