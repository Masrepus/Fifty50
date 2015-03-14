package com.fifty50.computer;
// FingerName.java
// Andrew Davison, ad@fivedots.coe.psu.ac.th, November 2012

/* Names of the fingers, ordered counter-clockwise for the left hand

*/


public enum FingerName {
    KLEINER, RING, MITTEL, ZEIGE, DAUMEN,
    UNBEKANNT;


    public FingerName getNext() {
        int nextIdx = ordinal() + 1;
        if (nextIdx == (values().length))
            nextIdx = 0;
        return values()[nextIdx];
    }  // end of getNext()


    public FingerName getPrev() {
        int prevIdx = ordinal() - 1;
        if (prevIdx < 0)
            prevIdx = values().length - 1;
        return values()[prevIdx];
    }  // end of getPrev()

}  // end of FingerName enum



