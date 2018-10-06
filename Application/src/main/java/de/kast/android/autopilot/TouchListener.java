package de.kast.android.autopilot;

import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

class TouchListener implements View.OnTouchListener {
    private GestureDetector mGestureDetector;
    private MyFragment mFragment;

    TouchListener(MyFragment fragment) {
        mFragment = fragment;
        mGestureDetector = new GestureDetector(fragment.getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                mFragment.doubleClicked();
                return super.onDoubleTap(e);
            }
        });

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mGestureDetector.onTouchEvent(event);
        return true;
    }
}
