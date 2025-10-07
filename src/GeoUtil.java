public class GeoUtil {
    // Haversine formula between lat/lon pairs
    public static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    // Try to compute distance between user's named location and parking lot.
    // If location DB has coordinates for userName, use that; otherwise fallback to plLat/plLon if userName matches a node lat/lon not available -> return a large number.
    public static double haversineFromName(String userName, String parkingNodeName, double plLat, double plLon) {
        double[] coords = GeoDB.get(userName);
        if (coords[0] == 0 && coords[1] == 0) {
            // user name not known; if parking node equals userName, distance 0; else return large
            if (userName.equalsIgnoreCase(parkingNodeName)) return 0.0;
            return 1e6;
        }
        return haversine(coords[0], coords[1], plLat, plLon);
    }
}
