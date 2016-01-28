package graaf.nl.bluetoothbackpack.activities;

import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

import graaf.nl.bluetoothbackpack.R;
import graaf.nl.bluetoothbackpack.controllers.LocationController;
import graaf.nl.bluetoothbackpack.controllers.LocationListener;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, LocationListener {

    private GoogleMap mMap;
    private ArrayList<LatLng> requests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);

        LocationController.getInstance(this).setLocationListener(this);
        requests = LocationController.getInstance().getDangerLocations();

        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMyLocationEnabled(true);
        Location location = LocationController.getInstance(this).getCurrentLocation();
        if (location != null) {
            LatLng myLocation = new LatLng(location.getLatitude(),
                    location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(myLocation, 12));
        }

        onHelpRequestChanged(requests);
    }

    @Override
    public void onHelpRequestChanged(ArrayList<LatLng> requests) {
        this.requests = requests;
        for (LatLng latLng : requests) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(latLng.latitude + 0.01f, latLng.longitude + 0.01f))
                    .title("Help Me!"));
        }
    }
}
