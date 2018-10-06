package de.kast.android.autopilot.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;

import de.kast.android.autopilot.service.AutopilotService;
import de.kast.android.autopilot.R;


/**
 * Created by bernd on 30.06.17.
 */

public class DebugFragment extends MyFragment {

    /**
     * Array adapter for the conversation thread
     */
    protected ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    protected StringBuffer mOutStringBuffer;

    protected ListView mConversationView;
    protected EditText mOutEditText;
    protected Button mSendButton;
    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    protected TextView.OnEditorActionListener mWriteListener
            = new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            return true;
        }
    };

    @Override
    public View createView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_debug, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // connect #2#
        mConversationView = (ListView) view.findViewById(R.id.in);
        mOutEditText = (EditText) view.findViewById(R.id.edit_text_out);
        mSendButton = (Button) view.findViewById(R.id.button_send);
    }

    protected void setup() {
        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<String>(getActivity(), R.layout.message);
        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Init #3#
        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Send a message using content of the edit text widget
                View view = getView();
                if (null != view) {
                    TextView textView = (TextView) view.findViewById(R.id.edit_text_out);
                    String message = textView.getText().toString() + "\r\n";
                    sendMessage(message);
                }
            }
        });

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    protected void sendMessage(String message) {
        AutopilotService.sendMessage(message);

        if (message.length() > 0) {
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    @Override
    public void setData(String rawMessage, HashMap<String, Double> data, ArrayList<HashMap<String, Double>> history) {
        this.mConversationArrayAdapter.add(rawMessage);
    }

    @Override
    public String getFragmentName() {
        return "DebugFragment";
    }

}
