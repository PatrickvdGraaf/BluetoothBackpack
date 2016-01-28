package graaf.nl.bluetoothbackpack.controllers;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Patrick van de Graaf on 28-1-2016.
 */
public interface LocationListener {
    void onHelpRequestChanged(ArrayList<LatLng> requests);
}
