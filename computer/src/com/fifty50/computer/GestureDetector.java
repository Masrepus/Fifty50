package com.fifty50.computer;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by samuel on 27.02.15.
 */
public class GestureDetector extends Thread {

    private static final int ANGLE_SMOOTHING_VALUE = 10;
    private static final int DIR_CACHE_SIZE = 1;
    public static final int CENTER_THRESHOLD = 75;

    private Main main;
    private HandDetector detector;
    private HandPanel panel;
    private GameHandler handler;

    private int smoothAngle = 0;
    private int smoothFingers = 0;
    private Point handPosition;
    private volatile Point center;
    private int brakeZoneHeight;

    private Car.Direction currDirection = Car.Direction.STRAIGHT;
    private Car.Direction lastSentCommand = Car.Direction.STRAIGHT;

    private Car.DrivingMode currDrivingMode = Car.DrivingMode.BRAKE;
    private Car.DrivingMode lastSentDrivingMode = Car.DrivingMode.BRAKE;

    final ArrayList<Point> pointsCache = new ArrayList<Point>();
    private ArrayList<OnCalibrationFininshedListener> listeners = new ArrayList<OnCalibrationFininshedListener>();

    private boolean isRunning, keyboardMode;

    private Thread detectorThread;


    public GestureDetector(Main main, HandPanel panel, GameHandler handler) {
        this.main = main;
        this.detector = panel.getDetector();
        this.panel = panel;
        this.handler = handler;

        brakeZoneHeight = panel.getHeight() / 3;

        //callback to panel
        panel.setGestureDetector(this);
    }

    @Override
    public void run() {

        while (isRunning) {

            //only start sending commands after calibration
            if (center != null) {
                try {
                    handPosition = detector.getCogPoint();
                    smoothFingers = detector.getFingerTips().size();

                    //accelerate if there are fingers being shown and the hand is not below the horizontal line, else brake
                    if (smoothFingers == 0) {
                        currDrivingMode = Car.DrivingMode.BRAKE;
                    } else if (handPosition.y > center.y + 10) {
                        //go backwards if the hand is below the horizontal line and either left or right from the brake zone, else brake
                        if (handPosition.getX() > (center.getX() + CENTER_THRESHOLD) || handPosition.getX() < (center.getX() - CENTER_THRESHOLD)) currDrivingMode = Car.DrivingMode.BACKWARD;
                        else currDrivingMode = Car.DrivingMode.BRAKE;
                    } else currDrivingMode = Car.DrivingMode.FORWARD;

                    //check the angle
                    smoothAngle = angleMovingAverage(detector.getContourAxisAngle());

                    //steer to the left or to the right if the hand is pointing either left or right and it is right/left of the center of the player

                    if (smoothAngle > -20 && smoothAngle < 80 && handPosition.getX() > (center.getX() + CENTER_THRESHOLD) && currDrivingMode != Car.DrivingMode.BRAKE) {
                        currDirection = Car.Direction.LEFT;
                    } else if (smoothAngle > 100 && smoothAngle < 200 && handPosition.getX() < (center.getX() - CENTER_THRESHOLD) && currDrivingMode != Car.DrivingMode.BRAKE) {
                        currDirection = Car.Direction.RIGHT;
                    } else currDirection = Car.Direction.STRAIGHT;

                    //now send the current direction to the car, if it isn't the same as last time
                        switch (currDirection) {

                            case LEFT:
                                main.left(Car.Speed.FAST);
                                break;
                            case RIGHT:
                                main.right(Car.Speed.FAST);
                                break;
                            case STRAIGHT:
                                main.straight();
                                break;
                        }

                    //now send the current speed mode
                        switch (currDrivingMode) {

                            case FORWARD:
                                main.forward(Car.Speed.FAST);
                                break;
                            case BACKWARD:
                                main.backward(Car.Speed.FAST);
                                break;
                            case BRAKE:
                                main.brake();
                                break;
                        }
                } catch (Exception ignored) {
                }
            }
        }

        System.out.println("Gesture detector terminated");
        if (keyboardMode) main.notifyKeyboardModeActive();
    }

    public int angleMovingAverage(int angle) {
        return ((ANGLE_SMOOTHING_VALUE * smoothAngle + angle) / (ANGLE_SMOOTHING_VALUE + 1));
    }

    public int getSmoothAngle() {
        return smoothAngle;
    }

    public Car.Direction getCurrDirection() {
        return currDirection;
    }

    public Car.DrivingMode getCurrDrivingMode() {
        return currDrivingMode;
    }

    public Point getCenter() {
        return center;
    }

    public int getFingerCount() {
        return smoothFingers;
    }

    public void calibrate(OnCalibrationFininshedListener... listeners) {

        this.listeners.addAll(Arrays.asList(listeners));
        new Calibrator().start();
    }

    public int getBrakeZoneHeight() {
        return brakeZoneHeight;
    }

    public void close() {
        isRunning = false;
        //reset center
        center = null;
    }

    public void startThread(Thread detectorThread) {
        this.detectorThread = detectorThread;
        isRunning = true;
        keyboardMode = false;

        //start it
        this.detectorThread.start();
    }

    public Thread getDetectorThread() {
        return detectorThread;
    }

    public void disable(Thread dummyThread) {
        //set detectorThread to dummyThread and let it finish. Like that there won't be any exceptions when close() is called
        detectorThread = dummyThread;
        isRunning = false;
        keyboardMode = true;
        detectorThread.start();
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
                    } else {
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

                        center = new Point((int) averageX, (int) averageY);

                        //notify the registered listeners
                        for (OnCalibrationFininshedListener listener : listeners) {
                            listener.calibrationFinished(center);
                        }
                    }
                }
            }, 500, 500);
        }
    }

}
