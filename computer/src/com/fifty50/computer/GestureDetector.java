package com.fifty50.computer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 27.02.15.
 */
public class GestureDetector extends Thread {

    private static final int ANGLE_SMOOTHING_VALUE = 10;
    private static final int FINGER_SMOOTHING_VALUE = 0;
    private static final int DIR_CACHE_SIZE = 1;
    public static final int CENTER_THRESHOLD = 75;

    private Main main;
    private HandDetector detector;
    private HandPanel panel;

    private int smoothAngle = 0;
    private int smoothFingers = 0;
    private Point handPosition;
    private Point center;
    private int brakeZoneHeight;

    private Direction currDirection = Direction.STRAIGHT;
    private Cache<Direction> lastDirections = new Cache<Direction>(DIR_CACHE_SIZE);
    private Direction lastSentCommand = Direction.STRAIGHT;

    private Speed currSpeed = Speed.BRAKE;
    private Speed lastSentSpeed = Speed.BRAKE;

    final ArrayList<Point> pointsCache = new ArrayList<Point>();
    private ArrayList<OnCalibrationFininshedListener> listeners = new ArrayList<OnCalibrationFininshedListener>();


    public GestureDetector(Main main, HandPanel panel) {
        this.main = main;
        this.detector = panel.getDetector();
        this.panel = panel;

        brakeZoneHeight = HandPanel.HEIGHT / 3;

        //callback to panel
        panel.setGestureDetector(this);
    }

    @Override
    public void run() {

        while (isAlive()) {

            //only start sending commands after calibration
            if (center != null) {
                try {
                    handPosition = detector.getCogPoint();
                    smoothFingers = fingersMovingAverage(detector.getFingerTips().size());

                    //accelerate if there are fingers being shown, else brake
                    if (smoothFingers == 0 || handPosition.y > (HandPanel.HEIGHT - brakeZoneHeight)) {
                        currSpeed = Speed.BRAKE;
                    } else currSpeed = Speed.ACCELERATE;


                    //check the angle
                    smoothAngle = angleMovingAverage(detector.getContourAxisAngle());

                    //steer to the left or to the right if the hand is pointing either left or right and it is right/left of the center of the player

                    if (smoothAngle > -20 && smoothAngle < 80 && handPosition.getX() > (center.getX() + CENTER_THRESHOLD) && currSpeed != Speed.BRAKE) {
                        currDirection = Direction.LEFT;
                    } else if (smoothAngle > 100 && smoothAngle < 200 && handPosition.getX() < (center.getX() - CENTER_THRESHOLD) && currSpeed != Speed.BRAKE) {
                        currDirection = Direction.RIGHT;
                    } else currDirection = Direction.STRAIGHT;

                    //now send the current direction to the car, if it isn't the same as last time
                    if (currDirection != lastSentCommand) {
                        switch (currDirection) {

                            case LEFT:
                                main.left(Main.Speed.FAST);
                                break;
                            case RIGHT:
                                main.right(Main.Speed.FAST);
                                break;
                            case STRAIGHT:
                                main.straight();
                                break;
                        }
                    }

                    //now send the current speed mode
                    if (currSpeed != lastSentSpeed) {
                        switch (currSpeed) {

                            case ACCELERATE:
                                main.forward(Main.Speed.FAST);
                                break;
                            case BRAKE:
                                main.brake();
                                break;
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public int angleMovingAverage(int angle) {
        return ((ANGLE_SMOOTHING_VALUE * smoothAngle + angle) / (ANGLE_SMOOTHING_VALUE + 1));
    }

    public int getSmoothAngle() {
        return smoothAngle;
    }

    public Direction getCurrDirection() {
        return currDirection;
    }

    public Speed getCurrSpeed() {
        return currSpeed;
    }

    public Point getCenter() {
        return center;
    }

    public Direction dirMovingAverage(Direction direction) {

        //add the new command to the cache and then count each direction's number
        lastDirections.add(direction);

        int[] counts = new int[3];
        counts[0] = Collections.frequency(lastDirections, Direction.LEFT);
        counts[1] = Collections.frequency(lastDirections, Direction.RIGHT);
        counts[2] = Collections.frequency(lastDirections, Direction.STRAIGHT);

        //get the highest number and return the direction belonging to that ordinal
        int maxValue = 0;
        int maxIndex = 0;

        for (int i = 0; i < 4; i++) {
            if (counts[i] > maxValue) {
                maxValue = counts[i];
                maxIndex = i;
            }
        }

        //now check if the max value's popularity is more than 50%
        if (maxValue > (lastDirections.size() / 2)) {
            //use it
            return Direction.values()[maxIndex];
        } else {
            //stay with the last sent command
            return lastSentCommand;
        }
    }

    public int fingersMovingAverage(int fingers) {
        return ((FINGER_SMOOTHING_VALUE * smoothFingers + fingers) / (FINGER_SMOOTHING_VALUE + 1));
    }

    public int getFingerCount() {
        return smoothFingers;
    }

    public void calibrate(OnCalibrationFininshedListener listener) {
        listeners.add(listener);
        new Calibrator().start();
    }

    public int getBrakeZoneHeight() {
        return brakeZoneHeight;
    }

    private class Calibrator extends Thread {

        @Override
        public void run() {
            final Timer timer = new Timer();

            //calibrate for 3 seconds
            final int[] count = new int[1];
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if (count[0] < 6) {
                        count[0]++;
                        pointsCache.add(detector.getCogPoint());
                    }
                    else {
                        pointsCache.add(detector.getCogPoint());
                        timer.cancel();

                        //now calculate the average point
                        double totalX = 0, totalY = 0;
                        for (Point point : pointsCache) {
                            totalX += point.getX();
                            totalY += point.getY();
                        }
                        double averageX, averageY;
                        averageX = totalX / pointsCache.size();
                        averageY = totalY / pointsCache.size();

                        center = new Point((int)averageX, (int)averageY);

                        //notify the registered listeners
                        for (OnCalibrationFininshedListener listener : listeners) {
                            listener.calibrationFinished(center);
                        }
                    }
                }
            }, 500, 500);
        }
    }

    public static enum Direction {LEFT, RIGHT, STRAIGHT}
    public static enum Speed {ACCELERATE, BRAKE}
}
