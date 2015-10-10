package com.fifty50.computer;

/**
 * Created by samuel on 10.10.15.
 */
public class Car {
    public enum PinState {LOW, HIGH;
        public static PinState parse(String s) {
            return (s.toUpperCase().contentEquals("LOW")) ? LOW : HIGH;
        }
    }

    public enum Speed {SLOW, FAST}
}
