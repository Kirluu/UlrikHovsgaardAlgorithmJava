package com.dcr.utils;

public class Utility {

    public static Byte[] CloneByteArray(Byte[] original) {
        Byte[] result = new Byte[original.length];
        for (int i = 0; i < original.length; i++) {
            result[i] = new Byte(original[i]);
        }
        return result;
    }
}
