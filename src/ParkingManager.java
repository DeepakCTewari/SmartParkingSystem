import java.util.*;

public class ParkingManager {
    private List<ParkingLot> lots;
    private Map<String, List<Dijkstra.Edge>> graph;
    private Map<String, String> parkedVehicles; // vehicleNumber -> parkingId (String)
    private WaitlistManager waitlist;
    private LogManager logger;

    public ParkingManager(List<ParkingLot> lots, Map<String, List<Dijkstra.Edge>> graph,
                          WaitlistManager waitlist, LogManager logger) {
        this.lots = lots;
        this.graph = graph;
        this.waitlist = waitlist;
        this.logger = logger;
        this.parkedVehicles = new HashMap<>();
    }

    // Get available parking lots
    public List<ParkingLot> getAvailableParkingLots() {
        List<ParkingLot> available = new ArrayList<>();
        for (ParkingLot lot : lots) {
            if (lot.availableSlots > 0) {
                available.add(lot);
            }
        }
        return available;
    }

    // ✅ Reserve at specific lot (fixed to use String lotId)
    public boolean reserveVehicleAtLot(String vehicleNumber, String lotId) {
        vehicleNumber = vehicleNumber.toUpperCase();

        if (parkedVehicles.containsKey(vehicleNumber)) {
            System.out.println("❌ Vehicle already parked at lot: " + parkedVehicles.get(vehicleNumber));
            return false;
        }

        for (ParkingLot lot : lots) {
            if (lot.id.equals(lotId) && lot.availableSlots > 0) {
                lot.availableSlots--;
                parkedVehicles.put(vehicleNumber, lotId);
                logger.log("PARK", vehicleNumber + " at lot " + lotId);
                System.out.println("✅ Vehicle " + vehicleNumber + " parked at lot " + lotId);
                return true;
            }
        }

        System.out.println("❌ Lot " + lotId + " not available");
        waitlist.add(vehicleNumber);
        return false;
    }

    // Find nearest parking (exclude current location)
    public ParkingLot recommendNearestFromLocation(String userLocation) {
        List<ParkingLot> available = getAvailableParkingLots();
        ParkingLot nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (ParkingLot lot : available) {
            // Skip if same location
            if (lot.locationName.equalsIgnoreCase(userLocation)) {
                continue;
            }

            double distance = getDistance(userLocation, lot.locationName);
            if (distance < minDistance && distance > 0) {
                minDistance = distance;
                nearest = lot;
            }
        }

        return nearest;
    }

    // Get directions
    public List<String> getDirections(String from, String to) {
        if (from.equalsIgnoreCase(to)) {
            return Arrays.asList(from, "You are already at destination");
        }

        try {
            Dijkstra dj = new Dijkstra(graph);
            Dijkstra.Result res = dj.shortestPath(from.toUpperCase());
            return Dijkstra.reconstructPath(from.toUpperCase(), to.toUpperCase(), res.prev);
        } catch (Exception e) {
            return Arrays.asList(from, "Route not available", to);
        }
    }

    // Get distance
    public double getDistance(String from, String to) {
        if (from.equalsIgnoreCase(to)) return 0.0;

        try {
            Dijkstra dj = new Dijkstra(graph);
            Dijkstra.Result res = dj.shortestPath(from.toUpperCase());
            Double dist = res.dist.get(to.toUpperCase());
            return (dist != null && !dist.isInfinite()) ? dist : 5.0; // Default 5km if no route
        } catch (Exception e) {
            return 5.0; // Default distance
        }
    }

    // Reserve vehicle at any available lot
    public boolean reserveVehicle(String vehicleNumber) {
        vehicleNumber = vehicleNumber.toUpperCase();

        if (parkedVehicles.containsKey(vehicleNumber)) {
            System.out.println("❌ Vehicle already parked at lot: " + parkedVehicles.get(vehicleNumber));
            return false;
        }

        // Find first available lot
        for (ParkingLot lot : lots) {
            if (lot.availableSlots > 0) {
                lot.availableSlots--;
                parkedVehicles.put(vehicleNumber, lot.id);
                logger.log("PARK", vehicleNumber + " at " + lot.id);
                System.out.println("✅ Vehicle " + vehicleNumber + " parked at lot " + lot.id);
                return true;
            }
        }

        System.out.println("❌ No slots available - added to waitlist");
        waitlist.add(vehicleNumber);
        return false;
    }

    // Free vehicle
    public boolean freeByVehicle(String vehicleNumber) {
        vehicleNumber = vehicleNumber.toUpperCase();

        if (!parkedVehicles.containsKey(vehicleNumber)) {
            System.out.println("❌ Vehicle not found");
            return false;
        }

        String lotId = parkedVehicles.remove(vehicleNumber);
        ParkingLot lot = getParkingById(lotId);

        if (lot != null) {
            lot.availableSlots++;
            logger.log("FREE", vehicleNumber + " from " + lotId);
            System.out.println("✅ Vehicle " + vehicleNumber + " freed from lot " + lotId);

            // Assign next waitlisted vehicle if any
            if (!waitlist.isEmpty()) {
                String nextVehicle = waitlist.pop();
                reserveVehicle(nextVehicle);
            }
            return true;
        }

        return false;
    }

    // Emergency free slot
    public void emergencyFreeSlot(Scanner sc) {
        System.out.print("Enter parking lot ID to free: ");
        String lotId = sc.nextLine().trim();

        ParkingLot lot = getParkingById(lotId);
        if (lot == null) {
            System.out.println("❌ Invalid lot ID");
            return;
        }

        // Remove all vehicles from this lot
        int freedCount = 0;
        Iterator<Map.Entry<String, String>> it = parkedVehicles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getValue().equals(lotId)) {
                it.remove();
                freedCount++;
                logger.log("EMERGENCY_FREE", entry.getKey() + " from " + lotId);
            }
        }

        // Free all slots
        int previousSlots = lot.availableSlots;
        lot.availableSlots = lot.totalSlots;
        int actuallyFreed = lot.totalSlots - previousSlots;

        System.out.println("✅ Freed " + freedCount + " vehicles and " + actuallyFreed + " slots from lot " + lotId);

        // Assign waitlisted vehicles
        while (!waitlist.isEmpty() && lot.availableSlots > 0) {
            String nextVehicle = waitlist.pop();
            reserveVehicleAtLot(nextVehicle, lotId); // ✅ Fixed here
        }
    }

    // Get parking by ID
    public ParkingLot getParkingById(String id) {
        for (ParkingLot lot : lots) {
            if (lot.id.equals(id)) return lot;
        }
        return null;
    }

    // Print status
    public void printParkingStatus() {
        System.out.println("\n--- Parking Status ---");
        for (ParkingLot lot : lots) {
            System.out.println(lot);
        }
    }

    // Search vehicle
    public int searchVehicle(String vehicleNumber) {
        vehicleNumber = vehicleNumber.toUpperCase();
        return parkedVehicles.containsKey(vehicleNumber) ? 1 : -1;
    }

    // Persist data
    public void persistParkingData(String filepath) {
        try {
            FileUtil.saveParkingData(filepath, lots);
        } catch (Exception e) {
            System.out.println("❌ Error saving data: " + e.getMessage());
        }
    }
}
