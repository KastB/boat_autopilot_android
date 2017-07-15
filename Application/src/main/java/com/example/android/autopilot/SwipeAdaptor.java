package com.example.android.autopilot;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

/**
 * Created by bernd on 30.06.17.
 */

public class SwipeAdaptor extends FragmentStatePagerAdapter {
    SwipeAdaptor(android.support.v4.app.FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        if(position == 0)
            return new AutopilotFragment();
        else //if (position == 1)
            return new DebugFragment();

    }

    @Override
    public int getCount() {
        return 2;
    }
}
