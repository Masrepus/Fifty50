package com.fifty50.computer;

import java.util.Collections;

/**
 * Created by samuel on 27.02.15.
 */
public class GestureDetector extends Thread {

    private static final int ANGLE_SMOOTHING_VALUE = 10;
    private static final int FINGER_SMOOTHING_VALUE = 0;
    private static final int DIR_CACHE_SIZE = 15;
    private Main main;
    private HandDetector detector;
    private int smoothAngle = 0;
    private int smoothFingers = 0;
    private Direction currDirection = Direction.BRAKE;
    private Cache<Direction> lastDirections = new Cache<Direction>(DIR_CACHE_SIZE);
    private Direction lastSentCommand = Direction.BRAKE;
    public GestureDetector(Main main, HandPanel panel) {
        this.main = main;
        this.detector = panel.getDetector();
        panel.setGestureDetector(this);
    }

    @Override
    public void run() {

        while (isAlive()) {

            try {
                smoothFingers = fingersMovingAverage(detector.getFingerTips().size());
                //accelerate if there are fingers being shown, else brake
                if (smoothFingers > 0) {
                    currDirection = dirMovingAverage(Direction.STRAIGHT);
                } else currDirection = dirMovingAverage(Direction.BRAKE);


                //check the angle
                smoothAngle = angleMovingAverage(detector.getContourAxisAngle());

                //steer to the left or to the right if the is pointing either left or right

                System.out.println("Axis angle: " + smoothAngle + "°");

                if (smoothAngle > -20 && smoothAngle < 85) {
                    currDirection = dirMovingAverage(Direction.LEFT);
                } else if (smoothAngle > 95 && smoothAngle < 180) {
                    currDirection = dirMovingAverage(Direction.RIGHT);
                }

                //now send the current direction to the car, if it isn't the same as last time
                if (currDirection != lastSentCommand) {
                    switch (currDirection) {

                        case STRAIGHT:
                            main.forward(Main.Speed.FAST);
                            break;
                        case BRAKE:
                            main.brake();
                            break;
                        case LEFT:
                            main.left(Main.Speed.FAST);
                            break;
                        case RIGHT:
                            main.right(Main.Speed.FAST);
                            break;
                    }
                }
            } catch (Exception ignored) {
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

    public Direction dirMovingAverage(Direction direction) {

        //add the new command to the cache and then count each direction's number
        lastDirections.add(direction);

        int[] counts = new int[4];
        counts[0] = Collections.frequency(lastDirections, Direction.LEFT);
        counts[1] = Collections.frequency(lastDirections, Direction.RIGHT);
        counts[2] = Collections.frequency(lastDirections, Direction.STRAIGHT);
        counts[3] = Collections.frequency(lastDirections, Direction.BRAKE);

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

    public static enum Direction {LEFT, RIGHT, STRAIGHT, BRAKE}
}
