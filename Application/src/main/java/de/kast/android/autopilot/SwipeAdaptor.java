package de.kast.android.autopilot;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;

/**
 * Created by bernd on 30.06.17.
 */

public class SwipeAdaptor extends FragmentStatePagerAdapter {
    SwipeAdaptor(android.support.v4.app.FragmentManager fm) {
        super(fm);
    }

    @Override
    public Fragment getItem(int position) {
        Fragment frag;
        if (position == 0)
            frag = new AutopilotFragment();
        else if (position == 1)
            frag = new GraphFragment();
        else if (position == 2)
            frag = new DebugFragment();
        else
            frag = new MapFragment();
        return frag;
    }

    @Override
    public int getCount() {
        return 4;
    }
}
