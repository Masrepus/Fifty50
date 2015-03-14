package com.fifty50.computer;

/**
 * Created by samuel on 27.02.15.
 */
public class GestureDetector extends Thread {

    private static final int SMOOTHING_VALUE = 5;
    private Main main;
    private HandDetector detector;
    private int smoothAngle = 0;
    public static enum Direction {LEFT, RIGHT, STRAIGHT, BRAKE}
    private Direction currDirection = Direction.STRAIGHT;

    public GestureDetector(Main main, HandPanel panel) {
        this.main = main;
        this.detector = panel.getDetector();
        panel.setGestureDetector(this);
    }

    @Override
    public void run() {

        while(isAlive()) {

            try {
                int fingers = detector.getFingerTips().size();
                //accelerate if no fingers are shown
                if (fingers == 0) {
                    main.forward(Main.Speed.FAST);
                    currDirection = Direction.STRAIGHT;
                }

                //brake if the full hand is shown
                if (fingers > 3) {
                    main.brake();
                    currDirection = Direction.BRAKE;
                }

                //check the angle
                smoothAngle = movingAverage(detector.getContourAxisAngle());

                //steer to the left or to the right if one finger is pointing either left or right
                if (fingers < 3) {

                    System.out.println("Axis angle: " + smoothAngle + "°");

                    if (smoothAngle > -20 && smoothAngle < 85) {
                        main.left(Main.Speed.FAST);
                        currDirection = Direction.LEFT;
                    }
                    if (smoothAngle > 95 && smoothAngle < 180) {
                        main.right(Main.Speed.FAST);
                        currDirection = Direction.RIGHT;
                    }
                }
            } catch (Exception e) {
                System.out.println(e);
            }
        }
    }

    private int movingAverage(int angle) {
        return ((SMOOTHING_VALUE*smoothAngle + angle) / (SMOOTHING_VALUE+1));
    }

    public int getSmoothAngle() {
        return smoothAngle;
    }

    public Direction getCurrDirection() {
        return currDirection;
    }
}
