package com.dcr.statistics;

public class Threshold {

    private static double _value;

    public static double getValue() {
        return _value;
    }

    public static void setValue(double newValue) {
        // TODO: Find magic Java-way to fire static event, alarming everyone of the threshold having changed!
        _value = newValue;
    }
}
