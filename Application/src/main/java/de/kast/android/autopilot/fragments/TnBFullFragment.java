package de.kast.android.autopilot.fragments;

import de.kast.android.autopilot.R;

public class TnBFullFragment extends TextAndButtonsFragment {
    final static int[] mButtonIds = {
        R.id.button_unlock,
        R.id.button_init,
        R.id.button_reinit,
        R.id.button_goparking,
        R.id.button_stop,
        R.id.button_compass,
        R.id.button_wind,
        R.id.button_d10,
        R.id.button_d,
        R.id.button_i,
        R.id.button_i10
    };
    final static int[] mTextViewIds = {
        R.id.goal,
        R.id.error,
        R.id.true_wind_speed,
        R.id.apparent_wind_angle,
        R.id.gps_speed,
        R.id.yaw,
        R.id.depth,
        R.id.water_temp,
        R.id.rudder_position,
        R.id.integral,
        R.id.roll,
        R.id.pitch,
        R.id.trip_milage,
        R.id.total_milage
    };
    public TnBFullFragment() {
        super(mTextViewIds, mButtonIds);
    }
}
