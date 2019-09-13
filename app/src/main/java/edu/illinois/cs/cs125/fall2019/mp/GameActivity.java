package edu.illinois.cs.cs125.fall2019.mp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents the game activity, where the user plays the game and sees its state.
 */
public final class GameActivity extends AppCompatActivity {

    /** The tag for Log calls - this makes it easier to tell what component messages come from. */
    private static final String TAG = "GameActivity";

    /** The radial location accuracy required to send a location update. */
    private static final float REQUIRED_LOCATION_ACCURACY = 28f;

    /** How close the user has to be (in meters) to a target to capture it. */
    private static final int PROXIMITY_THRESHOLD = 20;

    /** Hue of the markers showing captured target locations.
     * Note that this is ONLY the hue; markers don't allow specifying the RGB color like other map elements do. */
    private static final float CAPTURED_MARKER_HUE = BitmapDescriptorFactory.HUE_GREEN;

    /** Color of other map elements related to the player's progress (e.g. lines connecting captured targets). */
    private static final int PLAYER_COLOR = Color.GREEN;

    /** The handler for location updates sent by the location listener service. */
    private BroadcastReceiver locationUpdateReceiver;

    /** A reference to the map control. */
    private GoogleMap map;

    /** Whether the user's location has been found and used to center the map. */
    private boolean centeredMap;

    /** Whether permission has been granted to access the phone's exact location. */
    private boolean hasLocationPermission;

    /** The predefined targets' latitudes. */
    private double[] targetLats;

    /** The predefined targets' longitudes. */
    private double[] targetLngs;

    /** The sequence of target indexes captured by the player (-1 if slot not used yet). */
    private int[] path;

    /** List of the markers that have been added by the placeMarker function. */
    private List<Marker> markers = new ArrayList<>();

    /**
     * Called by the Android system when the activity is created. Performs initial setup.
     * @param savedInstanceState saved state from the last terminated instance (unused)
     */
    @Override
    @SuppressWarnings("ConstantConditions")
    protected void onCreate(final Bundle savedInstanceState) {
        Log.i(TAG, "Creating");
        // The "super" call is required for all activities
        super.onCreate(savedInstanceState);
        // Create the UI from the activity_game.xml layout file (in src/main/res/layout)
        setContentView(R.layout.activity_game);

        // Create the coordinate and path arrays
        targetLats = DefaultTargets.getLatitudes(this);
        targetLngs = DefaultTargets.getLongitudes(this);
        path = new int[targetLats.length];
        Arrays.fill(path, -1); // No targets visited initially

        // Find the Google Maps UI component ("fragment")
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.gameMap);
        // Interestingly, the UI component itself doesn't have methods to manipulate the map
        // We need to get a GoogleMap instance from it and use that
        mapFragment.getMapAsync(theMap -> {
            // NONLINEAR CONTROL FLOW: Code in this block is called later, after onCreate ends
            // It's a "callback" - it will be called eventually when the map is ready

            // Save the map so it can be manipulated later
            map = theMap;
            // Configure it
            setUpMap();
            Log.i(TAG, "getMapAsync completed");
        });
        Log.i(TAG, "getMapAsync started");

        // Set up a receiver for location-update messages from the service (LocationListenerService)
        locationUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                // NONLINEAR CONTROL FLOW: This method is called every time a broadcast is received
                // It receives the application context (unused here) and an Intent (the broadcast data)

                Log.i(TAG, "Received location update from service");
                // Android Intents represent action plans or notifications
                // They can contain data - the ones from our service contain a Location
                Location location = intent.getParcelableExtra(LocationListenerService.UPDATE_DATA_ID);

                // If the location is usable, call updateLocation
                if (map != null && location != null && location.hasAccuracy()
                        && location.getAccuracy() < REQUIRED_LOCATION_ACCURACY) {
                    Log.i(TAG, "Using location update");
                    // Center the map on this location if the user's location hasn't been previously found
                    ensureCenteredMap(location);
                    // Call updateLocation to update the game state using the player's coordinates
                    updateLocation(location.getLatitude(), location.getLongitude());
                }
            }
        };
        // Register (activate) it
        LocalBroadcastManager.getInstance(this).registerReceiver(locationUpdateReceiver,
                new IntentFilter(LocationListenerService.UPDATE_ACTION)); // Only listen for messages from the service

        // Android only allows location access to apps that asked for it and had the request approved by the user
        // See if we need to make a request
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // If permission isn't already granted, start a request
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, 0);
            Log.i(TAG, "Asked for location permission");
            // The result will be delivered to the onRequestPermissionsResult function
        } else {
            Log.i(TAG, "Already had location permission");
            // If we have the location permission, start the location listener service
            hasLocationPermission = true;
            startLocationWatching();
        }
    }

    /**
     * Sets up the Google map with initial UI.
     * <p>
     * You need to add logic to this function to add the objectives to the map.
     */
    @SuppressWarnings("MissingPermission")
    private void setUpMap() {
        Log.i(TAG, "Entered setUpMap");
        if (hasLocationPermission) {
            // Can only enable the blue My Location dot if the location permission is granted
            map.setMyLocationEnabled(true);
            Log.i(TAG, "setUpMap enabled My Location");
        }

        // Disable some extra UI that gets in the way
        map.getUiSettings().setIndoorLevelPickerEnabled(false);
        map.getUiSettings().setMapToolbarEnabled(false);

        // Use the provided placeMarker function to add a marker at every target's location
        // HINT: onCreate initializes the relevant arrays (targetLats, targetLngs, path) for you
    }

    /**
     * Called when a high-confidence location update is available.
     * <p>
     * You need to implement this function to make the game work.
     * @param latitude the phone's current latitude
     * @param longitude the phone's current longitude
     */
    @VisibleForTesting // Actually just visible for documentation - not called directly by test suites
    public void updateLocation(final double latitude, final double longitude) {
        // This function is responsible for updating the game state and map according to the user's movements

        // HINT: To operate on the game state, use the three methods you implemented in TargetVisitChecker
        // You can call them by prefixing their names with "TargetVisitChecker." e.g. TargetVisitChecker.visitTarget
        // The arrays to operate on are targetLats, targetLngs, and path

        // When the player gets within the PROXIMITY_THRESHOLD of a target, it should be captured and turned green
        // Sequential captures should create green connecting lines on the map
        // HINT: Use the provided changeMarkerColor and addLine functions to manipulate the map
        // HINT: Use the provided color constants near the top of this file as arguments to those functions
    }

    /**
     * Adds a colored line to the Google map.
     * @param startLat the latitude of one endpoint of the line
     * @param startLng the longitude of that endpoint
     * @param endLat the latitude of the other endpoint of the line
     * @param endLng the longitude of that other endpoint
     * @param color the color to fill the line with
     */
    @VisibleForTesting
    public void addLine(final double startLat, final double startLng,
                        final double endLat, final double endLng, final int color) {
        // Convert the loose coordinates to a Google Maps LatLng object
        LatLng start = new LatLng(startLat, startLng);
        LatLng end = new LatLng(endLat, endLng);

        // Configure and add a colored line
        final int lineThickness = 12;
        PolylineOptions fill = new PolylineOptions().add(start, end).color(color).width(lineThickness).zIndex(1);
        map.addPolyline(fill);

        // Polylines don't have a way to set borders, so we create a wider black line under the colored one to fake it
        final int borderThickness = 3;
        PolylineOptions border = new PolylineOptions().add(start, end).width(lineThickness + borderThickness);
        map.addPolyline(border);
    }

    /**
     * Places a marker on the map at the specified coordinates.
     * @param latitude the marker's latitude
     * @param longitude the marker's longitude
     */
    @VisibleForTesting // For documentation
    public void placeMarker(final double latitude, final double longitude) {
        // Convert the loose coordinates to a Google Maps LatLng object
        LatLng position = new LatLng(latitude, longitude);

        // Create a MarkerOptions object to specify where we want the marker
        MarkerOptions options = new MarkerOptions().position(position);

        // Add it to the map - Google Maps gives us the created Marker
        Marker marker = map.addMarker(options);

        // Keep track of the new marker so changeMarkerColor can adjust it later
        markers.add(marker);
    }

    /**
     * Changes the hue of the marker at the specified position.
     * The marker should have been previously added by placeMarker.
     * @param latitude the marker's latitude
     * @param longitude the marker's longitude
     * @param hue the new hue, e.g. a constant from BitmapDescriptorFactory
     */
    @VisibleForTesting
    public void changeMarkerColor(final double latitude, final double longitude, final float hue) {
        // Convert the loose coordinates to a Google Maps LatLng object
        LatLng position = new LatLng(latitude, longitude);

        // Try to find the existing marker (one with the same coordinates)
        for (Marker marker : markers) {
            if (LatLngUtils.same(position, marker.getPosition())) {
                // Create a new icon with the desired hue
                BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(hue);

                // Change the marker's icon
                marker.setIcon(icon);
                return;
            }
        }

        // Didn't find the existing marker
        Log.w(TAG, "No existing marker near " + latitude + ", " + longitude);
    }

    /**
     * Centers the map on the user's location if the map hasn't been centered yet.
     * @param location the current location
     */
    private void ensureCenteredMap(final Location location) {
        if (location != null && !centeredMap) {
            final float defaultMapZoom = 18f;
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(location.getLatitude(), location.getLongitude()), defaultMapZoom));
            centeredMap = true;
            Log.i(TAG, "Centered map");
        }
    }

    /**
     * Starts watching for location changes if possible under the current permissions.
     */
    @SuppressWarnings("MissingPermission")
    private void startLocationWatching() {
        Log.i(TAG, "Starting location watching");
        // Make sure the location permission has been granted
        if (!hasLocationPermission) {
            Log.w(TAG, "startLocationWatching: Missing permission");
            return;
        }
        // Make sure the My Location blue dot on the map is enabled
        if (map != null) {
            map.setMyLocationEnabled(true);
            Log.i(TAG, "startLocationWatching enabled My Location");
        }
        // Start the location listener service, which will notify this activity of movements
        ContextCompat.startForegroundService(this, new Intent(this, LocationListenerService.class));
        // Keep the screen on even if not touched in a while
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Stops watching for location changes.
     */
    private void stopLocationWatching() {
        Log.i(TAG, "Stopping location watching");
        // Stop the location listener service
        stopService(new Intent(this, LocationListenerService.class));
        // Allow the screen to turn off after a moment of inactivity
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * Called by the Android system when the activity is stopped and cannot be returned to.
     */
    @Override
    protected void onDestroy() {
        // The "super" call is required for all activities
        super.onDestroy();

        // Location is only needed while playing a game - stop the service to save power
        stopLocationWatching();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(locationUpdateReceiver);
        Log.i(TAG, "Destroyed");
    }

    /**
     * Called by the Android system when a permissions request receives a response from the user.
     * @param requestCode the ID of the request (always 0 in our case)
     * @param permissions the affected permissions' names
     * @param grantResults whether each permission was granted (corresponds to the permissions array)
     */
    @Override
    @SuppressLint("MissingPermission")
    public void onRequestPermissionsResult(final int requestCode, final @NonNull String[] permissions,
                                           final @NonNull int[] grantResults) {
        Log.i(TAG, "Permission request result received");
        // The "super" call is required so that the notification will be delivered to fragments
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Check whether the request was approved by the user
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.i(TAG, "Permission granted by the user");
            // Got the location permission for the first time
            hasLocationPermission = true;
            // Enable the My Location blue dot on the map
            if (map != null) {
                Log.i(TAG, "onRequestPermissionsResult enabled My Location");
                map.setMyLocationEnabled(true);
            }
            // Start the location listener service
            startLocationWatching();
        }
    }

}
