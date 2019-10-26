package edu.illinois.cs.cs125.fall2019.mp;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

/**
 * Target Class.
 */
public class Target {

    /**
     * Map.
     */
    private GoogleMap map;

    /**
     *
     *  Position LatLng object.
     */
    private LatLng position;

    /**
     * Team ID.
     */
    private int team;

    /**
     *  Suppose position is a LatLng variable.
     */
    private MarkerOptions options;

    /**
     * HUE color.
     */
    private static float hue = BitmapDescriptorFactory.HUE_VIOLET;
    /**
     * Marker.
     */
    private Marker marker;

    // Creates a target in a target-mode game by placing an appropriately colored marker on the map.

    /**
     *
     * @param setMap Map passed in to initialize the existing map.
     * @param setPosition Position passed in.
     * @param setTeam Team passed in.
     */
    Target(final com.google.android.gms.maps.GoogleMap setMap,
           final com.google.android.gms.maps.model.LatLng setPosition, final int setTeam) {
        map = setMap;
        position = setPosition;
        team = setTeam;

        options = new MarkerOptions().position(position);
        marker = map.addMarker(options);

        // Suppose hue is a hue value like the constants defined on BitmapDescriptorFactory
        BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(hue);
        marker.setIcon(icon);


    }

    /**
     *
     * @return LatLng object of position
     */
    public LatLng getPosition() {
        return position;
    }

    /**
     *
     * @return team id
     */
    public int getTeam() {
        return team;
    }

    /**
     *
     * @param t team id
     */
    public void setTeam(final int t) {
        team = t;
        hue = BitmapDescriptorFactory.HUE_BLUE;
        BitmapDescriptor icon = BitmapDescriptorFactory.defaultMarker(hue);
        marker.setIcon(icon);
    }


}

