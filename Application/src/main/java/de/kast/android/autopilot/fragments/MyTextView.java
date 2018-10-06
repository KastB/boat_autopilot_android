package de.kast.android.autopilot.fragments;

import android.view.View;
import android.widget.TextView;

import java.util.HashMap;

public class MyTextView {
    public int mId;
    public String mFormatString;
    public String mDataKey;

    protected TextView mTextView;

    MyTextView(int id, String formatString, String dataKey) {
        mId = id;
        mFormatString = formatString;
        mDataKey = dataKey;
    }

    void onViewCreated(View view) {
        mTextView = view.findViewById(mId);
    }

    void setText(HashMap<String, Double> data) {
        mTextView.setText(String.format(mFormatString, data.get(mDataKey)));
    }
}
