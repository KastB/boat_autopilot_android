package de.kast.android.autopilot.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import de.kast.android.autopilot.R;

public class TnBLightFragment extends TextAndButtonsFragment {
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
        R.id.rudder_position
    };
    public TnBLightFragment() {
        super(mTextViewIds, mButtonIds);
    }

    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_tnb_light, container, false);
    }

    @Override
    public String getFragmentName() {
        return "TnBLightFragment";
    }
}
