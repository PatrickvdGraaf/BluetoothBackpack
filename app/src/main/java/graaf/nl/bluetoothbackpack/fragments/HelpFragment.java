package graaf.nl.bluetoothbackpack.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import at.markushi.ui.CircleButton;
import graaf.nl.bluetoothbackpack.R;
import graaf.nl.bluetoothbackpack.controllers.BluetoothController;

/**
 * A simple {@link Fragment} subclass.
 */
public class HelpFragment extends Fragment {

    private CircleButton circleButton;

    public HelpFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_help, container, false);
        circleButton = (CircleButton) view.findViewById(R.id.help_btn);
        circleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BluetoothController.getInstance(getActivity()).requestHelp();
            }
        });
        return view;
    }
}
