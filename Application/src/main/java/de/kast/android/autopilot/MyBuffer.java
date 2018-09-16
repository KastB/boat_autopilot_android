package de.kast.android.autopilot;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by bernd on 15.07.17.
 */

public class MyBuffer {

    private int mMaxSize;
    private HashMap<String, Double>[] mBuffer;
    private int mIndex;
    private int mMaxIndex;

    MyBuffer(int size) {
        mMaxSize = size;
        mMaxIndex = -1;
        mIndex = -1;
        mBuffer = new HashMap[mMaxSize];
    }

    public void add(HashMap<String, Double> s) {
        mIndex++;
        mMaxIndex++;
        if (mMaxIndex >= mMaxSize)
            mMaxIndex = mMaxSize - 1;
        if (mIndex >= mMaxSize) {
            mIndex = 0;
        }
        mBuffer[mIndex] = s;
    }

    ArrayList<HashMap<String, Double>> getAll() {
        if (mMaxIndex < 0) {
            return new ArrayList<>();
        }
        ArrayList<HashMap<String, Double>> res = new ArrayList<>();
        int index = mIndex;
        for (int i = 0; i <= mMaxIndex; i++) {
            res.add(mBuffer[index]);
            index--;
            if (index < 0) {
                index = mMaxIndex;
            }
        }
        return res;
    }

    synchronized ArrayList<HashMap<String, Double>> addGetAll(HashMap<String, Double> s) {
        this.add(s);
        return this.getAll();
    }
}
