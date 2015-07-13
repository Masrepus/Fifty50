package com.fifty50.computer;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.Timer;

/**
 * Created by samuel on 02.05.15.
 */
public class Frame extends JFrame {

    private int width, height;
    private String[] args;
    private Starter starter;
    private Main main;
    private GameOver gameOver;
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

        getContentPane().setLayout(null);
        JLayeredPane background = new JLayeredPane();
        background.setLayout(null);
        background.setBounds(0, 0, width, height);
        getContentPane().add(background);

        //init the main screen but leave it invisible
        main = new Main(args);
        main.setBounds(0, 0, width, height);
        main.setVisible(false);
        main.setFrame(this);
        main.init();
        background.add(main, 0);
        background.revalidate();

        //init the start screen
        starter = new Starter(args);
        starter.setBounds(0, 0, width, height);
        starter.setVisible(true);
        starter.setFrame(this);
        background.add(starter, 0);
        background.revalidate();

        //init the game over screen
        gameOver = new GameOver(args[3]);
        background.add(gameOver, 0);
        background.revalidate();

        setUndecorated(true);
        pack();
        background.repaint();

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
                repaint();
                main.setVisible(true);
                main.start();

                gameHasRun = true;
                break;
            case STARTSCREEN:
                main.setVisible(false);
                main.pause();
                gameOver.setVisible(false);
                starter.setVisible(true);
                starter.restart();
                break;
            case GAMEOVER:
                main.setVisible(false);
                main.pause();
                gameOver.display(main.getHandler().getScore(), main.getHandler().getPhotoFnm());
                break;
        }
    }

    public static void main(String[] args) {
        new Frame(args);
    }
}
