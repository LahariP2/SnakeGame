package edu.illinois.cs.cs125.fall2019.mp;
import android.graphics.Color;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.PolylineOptions;


public class AreaDivider {

    /** Latitude of the northern boundary. */
    private double north;

    /** Longitude of the eastern boundary. */
    private double east;

    /** Latitude of the southern boundary. */
    private double south;

    /** Longitude of the western boundary. */
    private double west;

    /** The length of each side of a cell in meters. */
    private double cellSize;

    AreaDivider(final double setNorth, final double setEast, final double setSouth,
                final double setWest, final double setCellSize) {
        this.north = setNorth;
        this.east = setEast;
        this.south = setSouth;
        this.west = setWest;
        this.cellSize = setCellSize;
    }

    /**
     *
     * @param x This variable is the cell's X coordinate
     * @param y This variable is the cell's Y coordinate
     * @return This function returns the cell boundaries
     */
    public LatLngBounds getCellBounds(final int x, final int y) {
        double xDist = (east - west) / getXCells();
        double yDist = (north - south) / getYCells();

        double x1Bounds = west + (xDist * x);
        double y1Bounds = south + (yDist * y);

        double x2Bounds = (xDist * (x + 1)) + west;
        double y2Bounds = (yDist * (y + 1)) + south;

        LatLng one = new LatLng(y1Bounds, x1Bounds);
        LatLng two = new LatLng(y2Bounds, x2Bounds);

        //System.out.println("CellBounds " + new LatLngBounds(one, two));
        return new LatLngBounds(one, two);
    }

    /**
     * @return This function returns the number of cells in the horizontal direction
     * between the east and west boundaries
     */
    public int getXCells() {
        double distance = LatLngUtils.distance(south, east, south, west);
        //System.out.println("Distance x " + distance);
        int n = (int) Math.ceil((distance / cellSize));
        //System.out.println("Num " + n);
        return n;


        /*distance += distance % cellSize;
        System.out.println("Distance added after " + distance);
        int num = (int) (distance / cellSize);
        System.out.println("CellSize " + cellSize);*/
        //System.out.println("Num " + num);
        //return num;
    }

    /**
     *
     * @param location The location of a point with a specific Latitude and Longitude
     * @return This functionr eturns the x coordinate of the point's cell
     */
    public int getXCoordinate(final LatLng location) {
        double distance = LatLngUtils.distance(south, east, south, west);
        double actualSize = distance / getXCells();
        double ptDist = LatLngUtils.distance(location.latitude, west, location.latitude, location.longitude);
        return (int) Math.floor(ptDist / actualSize);
    }

    /**
     *
     * @return This function returns the number of cells in the vertical direction between the north and outh boundaries
     */
    public int getYCells() {
        double distance = LatLngUtils.distance(north, east, south, east);
        //System.out.println("Distance y " + distance);
        //distance += distance % cellSize;
        //System.out.println("Distance added after " + distance);
        int n = (int) Math.ceil((distance / cellSize));
        //System.out.println("Num " + num);
        return n;
    }

    /**
     *
     * @param location The location of a point with a specific longitude and latitude
     * @return This function returns the y coordinate of the cell the point if located in
     */
    public int getYCoordinate(final LatLng location) {
        double distance = LatLngUtils.distance(north, east, south, east);
        double actualSize = distance / getYCells();
        double ptDist = LatLngUtils.distance(location.latitude, location.longitude, south, location.longitude);
        return (int) Math.floor(ptDist / actualSize);
    }

    /**
     * @param map This function takes in a Google Map to draw lines on
     */
    public void renderGrid(final GoogleMap map) {

        final int lineThickness = 12;

        double distanceY = north - south;
        double actualYSize = distanceY / getYCells();

        //System.out.println("distance total " + distanceY + "actual Y size  " + actualYSize);

        //System.out.println(getXCells() + "     " + getYCells());

        for (int i = 0; i < getYCells() + 1; i++) {

            //System.out.println("i first is " + i);

            double startLat = south + (i * (actualYSize));
            double startLng = west;
            double endLat = south + (i * (actualYSize));
            double endLng = east;

            //System.out.println(startLat + " " + startLng + " " + endLat + "  " + endLng);

            LatLng start = new LatLng(startLat, startLng);
            LatLng end = new LatLng(endLat, endLng);
            PolylineOptions fill;
            fill = new PolylineOptions().add(start, end).color(Color.BLACK).width(lineThickness).zIndex(1);
            map.addPolyline(fill);

        }

        double distanceX = east - west; //LatLngUtils.distance(south, west, south, east);
        double actualXSize = distanceX / getXCells();

        //System.out.println("actual X size  " + actualXSize);

        for (int j = 0; j < getXCells() + 1; j++) {

            //System.out.println("i second is " + j);

            double startLat = north;
            double startLng = west + (j * actualXSize);
            double endLat = south;
            double endLng = west + (j * actualXSize);

            //System.out.println(startLat + " " + startLng + " " + endLat + "  " + endLng);

            LatLng start = new LatLng(startLat, startLng);
            LatLng end = new LatLng(endLat, endLng);
            PolylineOptions fill;
            fill = new PolylineOptions() .add(start, end).color(Color.BLACK).width(lineThickness).zIndex(1);
            map.addPolyline(fill);
        }
    }
}
