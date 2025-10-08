public class ParkingLot {
    public String id;
    public String locationName; // Node name in graph (e.g., MGROAD)
    public int totalSlots;
    public int availableSlots;
    public double rating;
    public double costPerHour;   // NEW: parking cost
    public boolean secure;       // NEW: security
    public boolean covered;      // NEW: covered parking
    public boolean evCharging;   // NEW: EV charging
    public boolean valet;        // NEW: valet service
    public double lat, lon;

    public ParkingLot(String id, String locationName, int totalSlots, int availableSlots, 
                      double rating, double costPerHour, boolean secure, boolean covered, 
                      boolean evCharging, boolean valet, double lat, double lon) {
        this.id = id;
        this.locationName = locationName.toUpperCase();
        this.totalSlots = totalSlots;
        this.availableSlots = availableSlots;
        this.rating = rating;
        this.costPerHour = costPerHour;
        this.secure = secure;
        this.covered = covered;
        this.evCharging = evCharging;
        this.valet = valet;
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

    public void forceFreeAll() {
        availableSlots = totalSlots;
    }

    public double facilityScore() {
        int score = 0;
        if (secure) score++;
        if (covered) score++;
        if (evCharging) score++;
        if (valet) score++;
        return score / 4.0;
    }

    public String amenities() {
        StringBuilder sb = new StringBuilder();
        if (secure) sb.append("Security ");
        if (evCharging) sb.append("EV-Charging ");
        if (covered) sb.append("Covered ");
        if (valet) sb.append("Valet ");
        return sb.toString().trim();
    }

    @Override
    public String toString() {
        return id + " | " + locationName + " | Available: " + availableSlots + "/" + totalSlots 
               + " | Rating: " + rating + " | Cost: $" + costPerHour + "/hr";
    }
}
