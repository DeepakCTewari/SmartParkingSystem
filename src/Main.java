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
        System.out.println("\n=== 🅿️ SMART PARKING SYSTEM 🚗 ===\n");

        boolean running = true;
        while (running) {
            System.out.println("\n🔐 MAIN MENU");
            System.out.println("1️⃣ Login");
            System.out.println("2️⃣ Register new User/Admin");
            System.out.println("3️⃣ System Statistics");
            System.out.println("4️⃣ Exit");
            System.out.print("Enter choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> login(sc, userManager, manager, waitlist, logger);
                case "2" -> register(sc, userManager);
                case "3" -> manager.printStatistics();
                case "4" -> {
                    System.out.println("👋 Exiting Smart Parking System.");
                    manager.persistParkingData(PARKING_FILE);
                    waitlist.persist();
                    running = false;
                }
                default -> System.out.println("❌ Invalid choice. Please enter 1, 2, 3, or 4.");
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
            System.out.println("❌ Invalid credentials. Try again.");
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

            if (!type.equals("ADMIN") && !type.equals("USER")) {
                System.out.println("❌ Invalid type. Must be ADMIN or USER.");
                return;
            }

            if (userManager.addUser(uname, pass, type)) {
                System.out.println("✅ User added successfully!");
            } else {
                System.out.println("❌ User already exists.");
            }
        } catch (Exception ex) {
            System.out.println("❌ Error adding user: " + ex.getMessage());
        }
    }

    private static void userPanel(Scanner sc, ParkingManager manager, WaitlistManager waitlist,
                                  LogManager logger) {
        while (true) {
            System.out.println("\n--- 🧑‍💻 USER MENU ---");
            System.out.println("1️⃣ Search Vehicle");
            System.out.println("2️⃣ Smart Parking Recommendation");
            System.out.println("3️⃣ Reserve Vehicle");
            System.out.println("4️⃣ Free Vehicle");
            System.out.println("5️⃣ Get Directions");
            System.out.println("6️⃣ View Parking Status");
            System.out.println("7️⃣ Logout");
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

                case "2" -> smartParkingRecommendation(sc, manager, logger);

                case "3" -> {
                    System.out.print("Enter vehicle number to reserve: ");
                    String vnBook = sc.nextLine().trim().toUpperCase();
                    if (vnBook.isEmpty()) {
                        System.out.println("❌ Vehicle number cannot be empty.");
                        break;
                    }
                    
                    System.out.println("\n🅿️ Available Parking Lots:");
                    List<ParkingLot> availableLots = manager.getAvailableParkingLots();
                    if (availableLots.isEmpty()) {
                        System.out.println("❌ No available parking lots.");
                        break;
                    }
                    
                    // Show available lots with details
                    for (int i = 0; i < availableLots.size(); i++) {
                        ParkingLot lot = availableLots.get(i);
                        System.out.printf("%d. %s | %s | Available: %d/%d | Rating: %.1f%n",
                            i + 1, lot.id, lot.locationName, lot.availableSlots, 
                            lot.totalSlots, lot.rating);
                    }
                    
                    System.out.print("Enter parking lot ID to reserve at (or press Enter for auto-assign): ");
                    String lotId = sc.nextLine().trim();
                    
                    boolean ok;
                    if (lotId.isEmpty()) {
                        ok = manager.reserveVehicle(vnBook);
                    } else {
                        ok = manager.reserveVehicleAtLot(vnBook, lotId);
                    }
                    
                    System.out.println(ok ? "✅ Vehicle reserved successfully." : "❌ No available slots. Added to waitlist.");
                    manager.persistParkingData(PARKING_FILE);
                }

                case "4" -> {
                    System.out.print("Enter vehicle number to free: ");
                    String vnCancel = sc.nextLine().trim().toUpperCase();
                    boolean okFree = manager.freeByVehicle(vnCancel);
                    System.out.println(okFree ? "✅ Vehicle freed successfully." : "❌ Vehicle not found or already free.");
                    manager.persistParkingData(PARKING_FILE);
                }

                case "5" -> getDirections(sc, manager);

                case "6" -> manager.printParkingStatus();

                case "7" -> {
                    System.out.println("👋 Logging out from User panel.");
                    return;
                }

                default -> System.out.println("❌ Invalid choice. Enter 1-7.");
            }
        }
    }

    private static void smartParkingRecommendation(Scanner sc, ParkingManager manager, LogManager logger) {
        System.out.println("📍 Available locations: " + GeoDB.availableNames());
        System.out.print("Enter your current location: ");
        String currentLoc = sc.nextLine().trim().toUpperCase();

        if (!GeoDB.availableNames().contains(currentLoc)) {
            System.out.println("❌ Invalid location. Available: " + GeoDB.availableNames());
            return;
        }

        System.out.println("\n🎯 Finding best parking options...");
        ParkingManager.RouteDetails routeDetails = manager.getDetailedRoute(currentLoc, "ANY_PARKING");
        
        ParkingLot recommendedLot = manager.recommendNearestFromLocation(currentLoc);
        
        if (recommendedLot == null) {
            System.out.println("❌ No suitable parking found.");
            return;
        }

        // Show detailed route to the recommended parking
        System.out.print("\nDo you want detailed directions to this parking? (Y/N): ");
        if (sc.nextLine().trim().equalsIgnoreCase("Y")) {
            List<String> directions = manager.getDirections(currentLoc, recommendedLot.locationName);
            System.out.println("\n" + "═".repeat(60));
            for (String direction : directions) {
                System.out.println(direction);
            }
            System.out.println("═".repeat(60));
        }

        System.out.print("\nDo you want to reserve a slot here? (Y/N): ");
        if (sc.nextLine().trim().equalsIgnoreCase("Y")) {
            System.out.print("Enter vehicle number: ");
            String vnBook = sc.nextLine().trim().toUpperCase();
            if (!vnBook.isEmpty() && manager.reserveVehicleAtLot(vnBook, recommendedLot.id)) {
                System.out.println("✅ Parking booked successfully at " + recommendedLot.locationName);
                manager.persistParkingData(PARKING_FILE);
                
                // Show confirmation with route
                System.out.print("Show route to parking? (Y/N): ");
                if (sc.nextLine().trim().equalsIgnoreCase("Y")) {
                    List<String> finalRoute = manager.getDirections(currentLoc, recommendedLot.locationName);
                    System.out.println("\n" + "🚗 YOUR ROUTE TO PARKING:");
                    System.out.println("═".repeat(50));
                    for (String step : finalRoute) {
                        System.out.println(step);
                    }
                }
            } else {
                System.out.println("❌ Failed to book parking. Slot may be taken.");
            }
        }

        logger.log("SMART_RECOMMEND", "User@" + currentLoc + " -> " + recommendedLot.locationName);
    }

    private static void getDirections(Scanner sc, ParkingManager manager) {
        System.out.println("📍 Available locations: " + GeoDB.availableNames());
        System.out.print("Enter starting location: ");
        String from = sc.nextLine().trim().toUpperCase();
        System.out.print("Enter destination: ");
        String to = sc.nextLine().trim().toUpperCase();
        manager.printShortestPathNodes(from, to);

        if (!GeoDB.availableNames().contains(from) || !GeoDB.availableNames().contains(to)) {
            System.out.println("❌ Invalid location(s). Available: " + GeoDB.availableNames());
            return;
        }

        System.out.println("\n" + "═".repeat(60));
        List<String> directions = manager.getDirections(from, to);
        for (String direction : directions) {
            System.out.println(direction);
        }
        System.out.println("═".repeat(60));
        
        // Also show detailed route using RouteDetails
        System.out.print("\nShow detailed route analysis? (Y/N): ");
        if (sc.nextLine().trim().equalsIgnoreCase("Y")) {
            ParkingManager.RouteDetails routeDetails = manager.getDetailedRoute(from, to);
            if (!routeDetails.hasError()) {
                routeDetails.printDetailedRoute();
            } else {
                System.out.println("❌ " + routeDetails.getError());
            }
        }
    }

    private static void adminPanel(Scanner sc, ParkingManager manager, WaitlistManager waitlist,
                                   LogManager logger, UserManager userManager) {
        while (true) {
            System.out.println("\n--- 🛠️ ADMIN MENU ---");
            System.out.println("1️⃣ Add Vehicle");
            System.out.println("2️⃣ Free Vehicle");
            System.out.println("3️⃣ Display Parking Status");
            System.out.println("4️⃣ Emergency Free Slot");
            System.out.println("5️⃣ View Waitlist");
            System.out.println("6️⃣ View Logs (last 50 lines)");
            System.out.println("7️⃣ Add New User/Admin");
            System.out.println("8️⃣ System Statistics");
            System.out.println("9️⃣ Clear Distance Cache");
            System.out.println("🔟 Get Directions (Admin)");
            System.out.println("⏸️ Logout");
            System.out.print("Choice: ");
            String choice = sc.nextLine().trim();

            switch (choice) {
                case "1" -> {
                    System.out.print("Enter vehicle number: ");
                    String vnAdd = sc.nextLine().trim().toUpperCase();
                    
                    System.out.print("Enter specific parking lot ID (or press Enter for auto-assign): ");
                    String lotId = sc.nextLine().trim();
                    
                    boolean added;
                    if (lotId.isEmpty()) {
                        added = manager.reserveVehicle(vnAdd);
                    } else {
                        added = manager.reserveVehicleAtLot(vnAdd, lotId);
                    }
                    
                    System.out.println(added ? "✅ Vehicle added successfully." : "❌ No available slots.");
                    manager.persistParkingData(PARKING_FILE);
                }
                
                case "2" -> {
                    System.out.print("Enter vehicle number to free: ");
                    String vnFree = sc.nextLine().trim().toUpperCase();
                    boolean freed = manager.freeByVehicle(vnFree);
                    System.out.println(freed ? "✅ Vehicle freed successfully." : "❌ Vehicle not found.");
                    manager.persistParkingData(PARKING_FILE);
                }
                
                case "3" -> manager.printParkingStatus();
                
                case "4" -> manager.emergencyFreeSlot(sc);
                
                case "5" -> {
                    System.out.println("\n--- 📋 WAITLIST STATUS ---");
                    System.out.println("Waitlisted vehicles: " + (waitlist.isEmpty() ? "None" : waitlist.snapshot()));
                    System.out.println("Waitlist size: " + waitlist.snapshot().size());
                }
                
                case "6" -> {
                    System.out.println("\n--- 📊 RECENT LOGS ---");
                    logger.printLastLines(50);
                }
                
                case "7" -> register(sc, userManager);
                
                case "8" -> manager.printStatistics();
                
                case "9" -> {
                    manager.clearCache();
                    System.out.println("✅ Distance cache cleared.");
                }
                
                case "10" -> getDirections(sc, manager);
                
                case "0" -> {
                    System.out.println("👋 Logging out from Admin panel.");
                    return;
                }
                
                default -> System.out.println("❌ Invalid choice. Enter 1-10 or 0 to logout.");
            }
        }
    }
}