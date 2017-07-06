package com.example.android.autopilot;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

/**
 * Created by bernd on 30.06.17.
 */

public class SwipeAdaptor extends FragmentPagerAdapter {
    List<Fragment> mFragments;
    SwipeAdaptor(android.support.v4.app.FragmentManager fm, List fragments) {
        super(fm);
        mFragments = fragments;
    }

    @Override
    public Fragment getItem(int position) {
        return mFragments.get(position);
    }

    @Override
    public int getCount() {
        return mFragments.size();
    }
}
