package com.fifty50.computer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Created by samuel on 02.05.15.
 */
public class Frame extends JFrame {

    public static final String FWD_FAST = "fwdFast";
    public static final String FWD_SLOW = "fwdSlow";
    public static final String BWD_FAST = "bwdFast";
    public static final String BWD_SLOW = "bwdSlow";
    public static final String LEFT_FAST = "leftFast";
    public static final String LEFT_SLOW = "leftSlow";
    public static final String RIGHT_FAST = "rightFast";
    public static final String RIGHT_SLOW = "rightSlow";
    public static final String BRAKE = "brake";
    public static final String STRAIGHT = "straight";
    public static final String FINISH = "finish";
    private static final String TOGGLE_KEYBOARD_MODE = "keyboard";
    private int width, height;
    private String[] args;
    private Starter starter;
    private Main main;
    private GameOver gameOver;
    private boolean gameHasRun;
    private boolean keyboardActive = false;

    public Car.Direction getCurrDirection() {
        return main.getCurrDirection();
    }

    public Car.DrivingMode getCurrDrivingMode() {
        return main.getCurrDrivingMode();
    }

    public enum Mode {STARTSCREEN, GAME, GAMEOVER}

    public Frame(String[] args) {

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
        background.setBackground(Color.BLACK);
        background.setBounds(0, 0, width, height);
        getContentPane().add(background);

        //init the main screen but leave it invisible
        main = new Main(args);
        main.setBounds(0, 0, width, height);
        main.setBackground(Color.BLACK);
        main.setVisible(false);
        main.setFrame(this);
        main.init();
        background.add(main, 0);
        background.revalidate();

        //init the start screen
        starter = new Starter(main, args);
        starter.setBounds(0, 0, width, height);
        starter.setVisible(true);
        starter.setFrame(this);
        background.add(starter, 0);
        background.revalidate();

        //init the game over screen
        gameOver = new GameOver(this, args[3]);
        background.add(gameOver, 0);
        background.revalidate();

        setUndecorated(true);
        pack();
        background.repaint();

        //set key bindings
        addKeyBindings();

        //go fullscreen
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        device.setFullScreenWindow(this);

        setVisible(true);
    }

    private void addKeyBindings() {

        //key controls have to work all the time, no matter which component is in focus
        InputMap input = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap action = getRootPane().getActionMap();
        //forward
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), FWD_FAST);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0, true), BRAKE); //true -> fires on key release
        action.put(FWD_FAST, new ForwardAction(Car.Speed.FAST));
        input.put(KeyStroke.getKeyStroke('w'), FWD_SLOW);
        input.put(KeyStroke.getKeyStroke("released w"), BRAKE);
        action.put(FWD_SLOW, new ForwardAction(Car.Speed.SLOW));
        //backward
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), BWD_FAST);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0, true), BRAKE);
        action.put(BWD_FAST, new BackwardAction(Car.Speed.FAST));
        input.put(KeyStroke.getKeyStroke('s'), BWD_SLOW);
        input.put(KeyStroke.getKeyStroke("released s"), BRAKE);
        action.put(BWD_SLOW, new BackwardAction(Car.Speed.SLOW));
        //left
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), LEFT_FAST);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, true), STRAIGHT);
        action.put(LEFT_FAST, new LeftAction(Car.Speed.FAST));
        input.put(KeyStroke.getKeyStroke('a'), LEFT_SLOW);
        input.put(KeyStroke.getKeyStroke("released a"), STRAIGHT);
        action.put(LEFT_SLOW, new LeftAction(Car.Speed.SLOW));
        //right
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), RIGHT_FAST);
        input.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, true), STRAIGHT);
        action.put(RIGHT_FAST, new RightAction(Car.Speed.FAST));
        input.put(KeyStroke.getKeyStroke('d'), RIGHT_SLOW);
        input.put(KeyStroke.getKeyStroke("released d"), STRAIGHT);
        action.put(RIGHT_SLOW, new RightAction(Car.Speed.SLOW));

        //brake and straight
        action.put(BRAKE, new BrakeAction());
        action.put(STRAIGHT, new StraightAction());

        //finish game
        input.put(KeyStroke.getKeyStroke('f'), FINISH);
        action.put(FINISH, new FinishAction());

        //activate keyboard control mode
        input.put(KeyStroke.getKeyStroke('k'), TOGGLE_KEYBOARD_MODE);
        action.put(TOGGLE_KEYBOARD_MODE, new ToggleKeyboardModeAction());
    }

    public void switchMode(Mode mode) {

        switch (mode) {

            case GAME:
                //start the game
                starter.setVisible(false);
                starter.pause();
                repaint();
                main.setVisible(true);
                if (gameHasRun) main.restart();
                else main.start();

                gameHasRun = true;
                break;
            case STARTSCREEN:
                gameOver.setVisible(false);
                starter.setVisible(true);
                starter.restart();
                repaint();
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

    @Override
    public void paint(Graphics g) {
        super.paint(g);
    }

    public boolean hasGameRun() {
        return gameHasRun;
    }

    private class ToggleKeyboardModeAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (keyboardActive) {
                main.disableGestureDetector();
                keyboardActive = false;
            }
            else {
                main.enableGestureDetector();
                keyboardActive = true;
            }
        }
    }

    private class ForwardAction extends AbstractAction {

        private Car.Speed speed;

        public ForwardAction(Car.Speed speed) {
            this.speed = speed;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.forward(speed);
        }
    }

    private class BackwardAction extends AbstractAction {

        private Car.Speed speed;

        public BackwardAction(Car.Speed speed) {
            this.speed = speed;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.backward(speed);
        }
    }

    private class LeftAction extends AbstractAction {

        private Car.Speed speed;

        public LeftAction(Car.Speed speed) {
            this.speed = speed;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.left(speed);
        }
    }

    private class RightAction extends AbstractAction {

        private Car.Speed speed;

        public RightAction(Car.Speed speed) {
            this.speed = speed;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            main.right(speed);
        }
    }

    private class BrakeAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            main.brake();
        }
    }

    private class StraightAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            main.straight();
        }
    }

    private class FinishAction extends AbstractAction {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (main.getHandler().isRunning()) main.getHandler().gameFinished();
            System.out.println("finish called; game now running: " + main.getHandler().isRunning());
        }
    }
}
