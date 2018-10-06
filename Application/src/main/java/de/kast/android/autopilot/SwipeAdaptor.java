package de.kast.android.autopilot;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentStatePagerAdapter;

import de.kast.android.autopilot.fragments.DebugFragment;
import de.kast.android.autopilot.fragments.GraphFragment;
import de.kast.android.autopilot.fragments.MapFragment;
import de.kast.android.autopilot.fragments.TextAndButtonsFragment;
import de.kast.android.autopilot.fragments.TnBFullFragment;
import de.kast.android.autopilot.fragments.TnBLightFragment;

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
            frag = new TnBFullFragment();
        else if (position == 1)
            frag = new TnBLightFragment();
        else if (position == 2)
            frag = new GraphFragment();
        else if (position == 3)
            frag = new DebugFragment();
        else
            frag = new MapFragment();
        return frag;
    }

    @Override
    public int getCount() {
        return 5;
    }
}
