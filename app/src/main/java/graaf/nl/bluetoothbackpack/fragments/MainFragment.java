package graaf.nl.bluetoothbackpack.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import graaf.nl.bluetoothbackpack.R;
import graaf.nl.bluetoothbackpack.controllers.BluetoothController;
import graaf.nl.bluetoothbackpack.controllers.LocationController;


public class MainFragment extends Fragment {
    private EditText inputText;
    private Button sendButton;
    private TextView locationText;

    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        inputText = (EditText) view.findViewById(R.id.textInput);
        sendButton = (Button) view.findViewById(R.id.sendButton);
        locationText = (TextView) view.findViewById(R.id.locationTv);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothController.getInstance(getActivity()).write(inputText.getText().toString());
            }
        });

        String text = LocationController.getInstance(getActivity()).getCurrentLocation().getLatitude() + ", " + LocationController.getInstance(getActivity()).getCurrentLocation().getLongitude();
        locationText.setText(text);

        return view;
    }
}
