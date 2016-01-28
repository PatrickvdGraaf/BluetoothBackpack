package graaf.nl.bluetoothbackpack.controllers;

import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

/**
 * Created by Patrick van de Graaf on 26-1-2016.
 */
public class LocationController {
    private static LocationController locationController;
    private Location currentBestLocation;
    private LocationManager locationManager;

    private static final int TWO_MINUTES = 1000 * 60 * 2;

    private ArrayList<LatLng> dangerLocations;

    private graaf.nl.bluetoothbackpack.controllers.LocationListener locationListener;

    public static LocationController getInstance() {
        return locationController;
    }

    public static LocationController getInstance(Context context) {
        if (locationController == null) {
            locationController = new LocationController(context);
        }
        return locationController;
    }

    private LocationController(Context context) {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

// Define a listener that responds to location updates
        LocationListener locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                if (isBetterLocation(location, currentBestLocation)) {
                    currentBestLocation = location;
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };

        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public Location getCurrentLocation() {
        if (currentBestLocation != null) {
            return currentBestLocation;
        }
        return locationManager.getLastKnownLocation((LocationManager.NETWORK_PROVIDER));
    }


    public ArrayList<LatLng> getDangerLocations() {
        if (this.dangerLocations == null) {
            this.dangerLocations = new ArrayList<>();
        }
        return dangerLocations;
    }

    public void setDangerLocations(ArrayList<LatLng> dangerLocations) {
        if (this.dangerLocations == null) {
            this.dangerLocations = new ArrayList<>();
        }
        if (this.dangerLocations != dangerLocations) {
            this.dangerLocations = dangerLocations;
            if (locationListener != null) {
                locationListener.onHelpRequestChanged(this.dangerLocations);
            }
        }
    }

    public graaf.nl.bluetoothbackpack.controllers.LocationListener getLocationListener() {
        return locationListener;
    }

    public void setLocationListener(graaf.nl.bluetoothbackpack.controllers.LocationListener locationListener) {
        this.locationListener = locationListener;
    }
}
