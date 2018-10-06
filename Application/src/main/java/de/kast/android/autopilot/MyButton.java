package de.kast.android.autopilot;

import android.view.View;
import android.widget.Button;

import java.util.HashMap;

public class MyButton {
    public int mId;
    public String mMessage;
    boolean mDefaultEnabled;
    String mDisabledKey;
    int mDisabledValue;

    Button mButton;
    View.OnClickListener mOnClickListener;

    MyButton(int id, View.OnClickListener onClickListener) {
        this(id, "", true, "", 0);
        mOnClickListener = onClickListener;
    }

    MyButton(int id, String message) {
        this(id, message, true, "", 0);
    }

    MyButton(int id, String message, boolean default_enabled) {
        this(id, message, default_enabled, "", 0);
    }

    MyButton(int id, String message, boolean default_enabled, String disabled_key, int disabled_value) {
        mId = id;
        mMessage = message;
        mDefaultEnabled = default_enabled;
        mDisabledKey = disabled_key;
        mDisabledValue = disabled_value;
        mOnClickListener = null;
    }

    void onViewCreated(View view) {
        mButton = view.findViewById(mId);
        mButton.setEnabled(mDefaultEnabled);
    }
    void setup() {
        if (mOnClickListener == null) {
            mButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AutopilotService.sendMessage(mMessage);
                }
            });
        }
        else {
            mButton.setOnClickListener(mOnClickListener);
        }

    }

    void updateEnabled(HashMap<String, Double> data) {
        if (!mDisabledKey.equals("")) {
            if (data.get(mDisabledKey).intValue() == mDisabledValue) {
                mButton.setEnabled(false);
            } else {
                mButton.setEnabled(true);
            }
        }

    }
}
