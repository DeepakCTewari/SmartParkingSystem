import java.util.*;

public class Main {
    private static final String DATA_DIR = "../data";  
    private static final String PARKING_FILE = DATA_DIR + "/parking_data.txt";
    private static final String GRAPH_FILE = DATA_DIR + "/city_graph.txt";
    private static final String LOCATION_FILE = DATA_DIR + "/location_db.txt";
    private static final String WAITLIST_FILE = DATA_DIR + "/waitlist.txt";
    private static final String LOG_FILE = DATA_DIR + "/vehicle_log.txt";
    private static final String USER_FILE = DATA_DIR + "/users.txt";

    public static void main(String[] args) throws Exception {
        GeoDB.load(LOCATION_FILE);
        System.out.println("DEBUG: Locations loaded -> " + GeoDB.availableNames());

        List<ParkingLot> lots = FileUtil.loadParkingLots(PARKING_FILE);
        Map<String, List<Dijkstra.Edge>> graph = FileUtil.loadGraph(GRAPH_FILE);
        WaitlistManager waitlist = new WaitlistManager(WAITLIST_FILE);
        LogManager logger = new LogManager(LOG_FILE);
        ParkingManager manager = new ParkingManager(lots, graph, waitlist, logger);
        UserManager userManager = new UserManager(USER_FILE);

        Scanner sc = new Scanner(System.in);
        System.out.println("=== Smart Parking Recommendation & Navigation System ===\n");

        boolean running = true;
        while (running) {
            System.out.println("\n1. Login");
            System.out.println("2. Register new User/Admin");
            System.out.println("3. Exit");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> login(sc, userManager, manager, waitlist, logger);
                case "2" -> register(sc, userManager);
                case "3" -> {
                    System.out.println("Exiting Smart Parking System.");
                    manager.persistParkingData(PARKING_FILE);
                    waitlist.persist();
                    running = false;
                }
                default -> System.out.println("❌ Invalid choice.");
            }
        }
        sc.close();
    }

    private static void login(Scanner sc, UserManager userManager, ParkingManager manager,
                              WaitlistManager waitlist, LogManager logger) {
        System.out.print("Enter username: ");
        String uname = sc.nextLine().trim();
        System.out.print("Enter password: ");
        String pass = sc.nextLine().trim();

        UserManager.User u = userManager.login(uname, pass);
        if (u != null) {
            System.out.println("✅ Login successful as " + u.type);
            if (u.type.equals("ADMIN")) adminPanel(sc, manager, waitlist, logger, userManager);
            else userPanel(sc, manager, waitlist, logger);
        } else {
            System.out.println("❌ Invalid credentials.");
        }
    }

    private static void register(Scanner sc, UserManager userManager) {
        try {
            System.out.print("Enter new username: ");
            String uname = sc.nextLine().trim();
            System.out.print("Enter password: ");
            String pass = sc.nextLine().trim();
            System.out.print("Enter type (ADMIN/USER): ");
            String type = sc.nextLine().trim().toUpperCase();
            if (type.equals("ADMIN") || type.equals("USER")) {
                if (userManager.addUser(uname, pass, type)) System.out.println("✅ User added!");
                else System.out.println("❌ User already exists.");
            } else {
                System.out.println("❌ Invalid type. Must be ADMIN or USER.");
            }
        } catch (Exception ex) {
            System.out.println("Error adding user: " + ex.getMessage());
        }
    }

    private static void userPanel(Scanner sc, ParkingManager manager, WaitlistManager waitlist,
                                  LogManager logger) {
        while (true) {
            System.out.println("\n--- User Menu ---");
            System.out.println("1. Search Vehicle");
            System.out.println("2. Recommend Parking");
            System.out.println("3. Reserve Vehicle");
            System.out.println("4. Free Vehicle");
            System.out.println("5. Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter vehicle number to search: ");
                    String vnSearch = sc.nextLine().trim().toUpperCase();
                    int slot = manager.searchVehicle(vnSearch);
                    if (slot == -1) System.out.println("❌ Vehicle not found.");
                    else System.out.println("🚗 Vehicle found at slot: " + slot);
                }

                case "2" -> {
                    System.out.println("Available locations: " + GeoDB.availableNames());
                    System.out.print("Enter current location name: ");
                    String currentLoc = sc.nextLine().trim().toUpperCase();

                    // Validate location
                    if (!GeoDB.availableNames().contains(currentLoc)) {
                        System.out.println("❌ Invalid location. Available: " + GeoDB.availableNames());
                        break;
                    }

                    // Get available parking lots (excluding current location)
                    List<ParkingLot> availableLots = manager.getAvailableParkingLots();
                    List<ParkingLot> filteredLots = new ArrayList<>();
                    
                    for (ParkingLot lot : availableLots) {
                        if (!lot.locationName.equals(currentLoc) && lot.availableSlots > 0) {
                            filteredLots.add(lot);
                        }
                    }

                    if (filteredLots.isEmpty()) {
                        System.out.println("❌ No available parking lots found at different locations.");
                        break;
                    }

                    // Find nearest parking
                    ParkingLot nearestLot = null;
                    double minDistance = Double.MAX_VALUE;
                    
                    for (ParkingLot lot : filteredLots) {
                        double distance = manager.getDistance(currentLoc, lot.locationName);
                        if (distance < minDistance && distance > 0) {
                            minDistance = distance;
                            nearestLot = lot;
                        }
                    }

                    if (nearestLot != null) {
                        System.out.println("\n📍 Your current location: " + currentLoc);
                        System.out.println("🎯 Recommended Parking: " + nearestLot);
                        System.out.printf("📏 Distance: %.2f km%n", minDistance);
                        System.out.printf("⏱️ Estimated travel time (30 km/h avg): %.1f min%n", (minDistance / 30.0) * 60);

                        // Directions
                        System.out.print("\nDo you want directions to this parking? (Y/N): ");
                        String ans = sc.nextLine().trim().toUpperCase();
                        if (ans.equals("Y")) {
                            List<String> path = manager.getDirections(currentLoc, nearestLot.locationName);
                            if (path != null && !path.isEmpty()) {
                                System.out.println("\n🗺️ Route from " + currentLoc + " to " + nearestLot.locationName + ":");
                                for (int i = 0; i < path.size(); i++) {
                                    System.out.println("📍 " + path.get(i));
                                    if (i < path.size() - 1) {
                                        System.out.println("  ↓");
                                    }
                                }
                            } else {
                                System.out.println("❌ No route found.");
                            }
                        }

                        // Reservation
                        System.out.print("\nDo you want to reserve a slot at this parking? (Y/N): ");
                        String bookAns = sc.nextLine().trim().toUpperCase();
                        if (bookAns.equals("Y")) {
                            System.out.print("Enter vehicle number to reserve: ");
                            String vnBook = sc.nextLine().trim().toUpperCase();
                            if (!vnBook.isEmpty()) {
                                boolean ok = manager.reserveVehicleAtLot(vnBook, nearestLot.id);
                                if (ok) {
                                    System.out.println("✅ Parking booked successfully at " + nearestLot.locationName);
                                    manager.persistParkingData(PARKING_FILE);
                                } else {
                                    System.out.println("❌ Failed to book parking. Slot may be taken.");
                                }
                            } else {
                                System.out.println("❌ Vehicle number cannot be empty.");
                            }
                        }
                        logger.log("RECOMMEND", "User@" + currentLoc + " -> " + nearestLot.locationName + " | Distance: " + minDistance + "km");
                    } else {
                        System.out.println("❌ No suitable parking found.");
                    }
                }

                case "3" -> {
                    System.out.print("Enter vehicle number to reserve: ");
                    String vnBook = sc.nextLine().trim().toUpperCase();
                    boolean ok = manager.reserveVehicle(vnBook);
                    if (ok) {
                        System.out.println("✅ Vehicle reserved successfully.");
                        manager.persistParkingData(PARKING_FILE);
                    } else {
                        System.out.println("❌ Failed to reserve vehicle. No available slots.");
                    }
                }

                case "4" -> {
                    System.out.print("Enter vehicle number to free: ");
                    String vnCancel = sc.nextLine().trim().toUpperCase();
                    boolean okFree = manager.freeByVehicle(vnCancel);
                    if (okFree) {
                        System.out.println("✅ Vehicle freed successfully.");
                        manager.persistParkingData(PARKING_FILE);
                    } else {
                        System.out.println("❌ Vehicle not found or already free.");
                    }
                }

                case "5" -> {
                    System.out.println("Logging out from User panel.");
                    return;
                }

                default -> System.out.println("❌ Invalid choice.");
            }
        }
    }

    private static void adminPanel(Scanner sc, ParkingManager manager, WaitlistManager waitlist,
                                   LogManager logger, UserManager userManager) {
        while (true) {
            System.out.println("\n--- Admin Menu ---");
            System.out.println("1. Add Vehicle");
            System.out.println("2. Free Vehicle");
            System.out.println("3. Display Parking Status");
            System.out.println("4. Emergency Free Slot");
            System.out.println("5. View Waitlist");
            System.out.println("6. View Logs (last 50 lines)");
            System.out.println("7. Add New User/Admin");
            System.out.println("8. Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter vehicle number: ");
                    String vnAdd = sc.nextLine().trim().toUpperCase();
                    boolean added = manager.reserveVehicle(vnAdd);
                    if (added) {
                        System.out.println("✅ Vehicle added successfully.");
                        manager.persistParkingData(PARKING_FILE);
                    } else {
                        System.out.println("❌ Failed to add vehicle. No available slots.");
                    }
                }
                case "2" -> {
                    System.out.print("Enter vehicle number to free: ");
                    String vnFree = sc.nextLine().trim().toUpperCase();
                    boolean freed = manager.freeByVehicle(vnFree);
                    if (freed) {
                        System.out.println("✅ Vehicle freed successfully.");
                        manager.persistParkingData(PARKING_FILE);
                    } else {
                        System.out.println("❌ Vehicle not found or already free.");
                    }
                }
                case "3" -> manager.printParkingStatus();
                case "4" -> manager.emergencyFreeSlot(sc);
                case "5" -> System.out.println("Waitlist snapshot: " + waitlist.snapshot());
                case "6" -> logger.printLastLines(50);
                case "7" -> register(sc, userManager);
                case "8" -> {
                    System.out.println("Logging out from Admin panel.");
                    return;
                }
                default -> System.out.println("❌ Invalid choice.");
            }
        }
    }
}