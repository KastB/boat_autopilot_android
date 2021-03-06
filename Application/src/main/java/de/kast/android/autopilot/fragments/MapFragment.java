/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.kast.android.autopilot.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;
import java.util.HashMap;

import de.kast.android.autopilot.R;

public class MapFragment extends MyFragment {
    // Layout Views
    WebView mapWebView;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_map, container, false);
        mapWebView = view.findViewById(R.id.webview_map);
        String text = "10.0.0.1:2948";
        try {
            SharedPreferences sharedPref = getActivity().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE);
            text = sharedPref.getString("last_tcp_server", text);
        } catch (NullPointerException e) {
        }
        String ip = text.substring(0, text.indexOf(":"));

        mapWebView.loadUrl("http://" + ip + ":8080/guacamole");
        // Enable Javascript
        WebSettings webSettings = mapWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);

        // Force links and redirects to open in the WebView instead of in a browser
        mapWebView.setWebViewClient(new WebViewClient());
        return view;

    }

    @Override
    public void onDestroy() {
        mapWebView.destroy();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        mapWebView.onPause();
        super.onPause();
    }

    @Override
    public void onResume() {
        mapWebView.onResume();
        super.onResume();
    }

    @Override
    void setup() {

    }

    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mapWebView = getView().findViewById(R.id.webview_map);
    }

    @Override
    public void setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history) {

    }

    @Override
    public String getFragmentName() {
        return "MapFragment";
    }


}
