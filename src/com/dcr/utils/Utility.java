package com.dcr.utils;

import java.util.ArrayList;
import java.util.List;

public class Utility {

    public static List<Byte> CloneByteArray(List<Byte> original) {
        List<Byte> result = new ArrayList<Byte>(original.size());
        for (int i = 0; i < original.size(); i++) {
            result.set(i, original.get(i));
        }
        return result;
    }
}
