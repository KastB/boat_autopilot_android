package com.example.android.autopilot;

/**
 * Created by bernd on 15.07.17.
 */

public class MyBuffer {

    int mMaxSize;
    String[] mBuffer;
    int mIndex;
    int mMaxIndex;

    MyBuffer(int size) {
        mMaxSize = size;
        mBuffer = new String[size];
        mMaxIndex = -1;
        mIndex = -1;
    }

    public void add(String s) {
        mIndex++;
        mMaxIndex++;
        if(mMaxIndex >= mMaxSize)
            mMaxIndex = mMaxSize-1;
        if(mIndex >= mMaxSize)
        {
            mIndex = 0;
        }
        mBuffer[mIndex] = s;
    }

    String[] getAll()
    {
        if (mMaxIndex < 0)
            return new String[0];
        String[] res = new String[mMaxIndex+1];
        int index = mIndex ;
        for(int i = 0; i <= mMaxIndex ; i++)
        {
            res[i] = mBuffer[index];
            index--;
            if(index < 0)
            {
                index = mMaxIndex;
            }
        }
        return res;
    }
}
