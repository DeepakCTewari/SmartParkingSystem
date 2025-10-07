public class ParkingLot {
    public String id;
    public String locationName; // Node name in graph (e.g., MGROAD)
    public int totalSlots;
    public int availableSlots;
    public double rating;
    public double lat, lon;

    public ParkingLot(String id, String locationName, int totalSlots, int availableSlots, double rating, double lat, double lon) {
        this.id = id;
        this.locationName = locationName.toUpperCase();
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
        this.rating = rating;
        this.lat = lat;
        this.lon = lon;
    }

    public boolean isAvailable() {
        return availableSlots > 0;
    }

    public void occupyOne() {
        if (availableSlots > 0) availableSlots--;
    }

    public void freeOne() {
        if (availableSlots < totalSlots) availableSlots++;
    }

    // Emergency: free all slots in this parking lot
    public void forceFreeAll() {
        availableSlots = totalSlots;
    }

    @Override
    public String toString() {
        return id + " | " + locationName + " | Available: " + availableSlots + "/" + totalSlots + " | Rating: " + rating;
    }
}
