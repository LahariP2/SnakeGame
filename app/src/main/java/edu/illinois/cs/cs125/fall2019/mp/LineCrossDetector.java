package edu.illinois.cs.cs125.fall2019.mp;

/**
 * Holds a method to determine whether two lines cross.
 * <p>
 * The implementation given here works. You do not need to change the logic, but there are some style
 * problems that you do need to correct.
 * <p>
 * This file will be revisited in Checkpoint 3.
 */
public class LineCrossDetector {

    /**
     * Determines whether two lines cross.
     * <p>
     * <i>Crossing</i> is not always the same as <i>intersecting</i>. Lines that share a tip
     * intersect but do not cross for purposes of this function. However, a line that has an endpoint
     * on the <i>middle</i> of another line must be considered to cross that line (to prevent
     * circumventing the snake rule).
     * <p>
     * For simplicity, longitude and latitude are treated as X and Y, respectively, on a 2D coordinate plane.
     * This ignores the roundness of the earth, but it's undetectable at reasonable scales of the game.
     * <p>
     * All parameters are assumed to be valid: both lines have positive length.
     * @param firstStartLat the latitude of the start of one line
     * @param firstStartLng the longitude of the start of that line
     * @param firstEndLat the latitude of the end of that line
     * @param firstEndLng the longitude of the end of that line
     * @param secondStartLat the latitude of the start of another line
     * @param secondStartLng the longitude of the start of that other line
     * @param secondEndLat the latitude of the end of that other line
     * @param secondEndLng the longitude of the end of that other line
     * @return whether the two lines cross
     */
    public static boolean linesCross(final double firstStartLat, final double firstStartLng,
                                     final double firstEndLat, final double firstEndLng,
                                     final double secondStartLat, final double secondStartLng,
                                     final double secondEndLat, final double secondEndLng) {
        if(LatLngUtils.same(firstStartLat, firstStartLng, secondStartLat, secondStartLng)
                || LatLngUtils.same(firstStartLat, firstStartLng, secondEndLat, secondEndLng)
                || LatLngUtils.same(firstEndLat, firstEndLng, secondStartLat, secondStartLng)
                || LatLngUtils.same(firstEndLat, firstEndLng, secondEndLat, secondEndLng)){
            // The lines are just sharing endpoints, not crossing each other
            return false;
        }

        // A line is vertical (purely north-south) if its longitude is constant
        boolean firstVertical = LatLngUtils.same(firstStartLng, firstEndLng);
        boolean secondVertical = LatLngUtils.same(secondStartLng, secondEndLng);
        if (firstVertical && secondVertical) {
            // They're parallel vertical lines
            return false;
        } else if (firstVertical) {
            return lineCrossesVertical(firstStartLat, firstEndLat, firstStartLng,
                    secondStartLat, secondStartLng, secondEndLat, secondEndLng);
        } else if (secondVertical) {
            return lineCrossesVertical(secondStartLat, secondEndLat, secondStartLng,
                    firstStartLat, firstStartLng, firstEndLat, firstEndLng);
        }

        // At this point, neither line is vertical
        double firstSlope = lineSlope(firstStartLat, firstStartLng, firstEndLat, firstEndLng);
        double secondSlope = lineSlope(secondStartLat, secondStartLng, secondEndLat, secondEndLng);
        if (LatLngUtils.same(firstSlope, secondSlope)) {
            // They're parallel
            return false;
        }

        // At this point, the lines are non-parallel (would intersect if infinitely extended)
        double firstIntercept = firstStartLat - firstSlope * firstStartLng;
        double secondIntercept = secondStartLat - secondSlope * secondStartLng;
        double intersectionX = -(firstIntercept - secondIntercept) / (firstSlope - secondSlope);
        if (LatLngUtils.same(intersectionX, firstStartLng) || LatLngUtils.same(intersectionX, firstEndLng) || LatLngUtils.same(intersectionX, secondStartLng) || LatLngUtils.same(intersectionX, secondEndLng)) {
            // Endpoint of one line is in the middle of the other line
            return true;
        }
        boolean onFirst = intersectionX > Math.min(firstStartLng, firstEndLng)
                && intersectionX < Math.max(firstStartLng, firstEndLng);
        boolean onSecond = intersectionX > Math.min(secondStartLng, secondEndLng)
                && intersectionX < Math.max(secondStartLng, secondEndLng);
        return onFirst && onSecond;
    }

    /**
     * Determines if a non-vertical line crosses a vertical line.
     * @param verticalStartLat the latitude of one endpoint of the vertical line
     * @param verticalEndLat the latitude of the other endpoint of the vertical line
     * @param verticalLng the longitude of the vertical line
     * @param lineStartLat the latitude of one endpoint of the non-vertical line
     * @param lineStartLng the longitude of that endpoint
     * @param lineEndLat the latitude of the other endpoint of the line
     * @param lineEndLng the longitude of that other endpoin
     * @return whether the lines cross
     */
    private static boolean lineCrossesVertical(final double verticalStartLat, final double verticalEndLat,
                                               double verticalLng,
                                               final double lineStartLat, final double lineStartLng,
                                               final double lineEndLat, final double lineEndLng) {
        if (Math.max(lineStartLng, lineEndLng) < verticalLng
                || Math.min(lineStartLng, lineEndLng) > verticalLng) {
            // The non-vertical line is completely off to the side of the vertical line
            return false;
        }
        double slope = lineSlope(lineStartLat, lineStartLng, lineEndLat, lineEndLng);
        double yAtVert = slope * (verticalLng - lineStartLng) + lineStartLat;
        if (LatLngUtils.same(yAtVert, verticalStartLat) || LatLngUtils.same(yAtVert, verticalEndLat)) {
            // Ends on the middle of the non-vertical line
            return true;
        }
        // See if the intersection of the lines is between the endpoints of the vertical line segment
        return yAtVert > Math.min(verticalStartLat, verticalEndLat)
                && yAtVert < Math.max(verticalStartLat, verticalEndLat);
    }

    /**
     * Determines the slope of a non-vertical line.
     * @param startLat the latitude of one endpoint of the line
     * @param startLng the longitude of that endpoint
     * @param endLat the latitude of the other endpoint of the line
     * @param endLng the longitude of that other endpoint
     * @return the slope, treating longitude as X and latitude as Y
     */
    private static double lineSlope(final double startLat, final double startLng,
                                    final double endLat, final double endLng) {
        return (endLat - startLat) / (endLng - startLng);
    }

}
