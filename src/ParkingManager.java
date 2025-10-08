import java.util.*;

public class ParkingManager {
    private List<ParkingLot> lots;
    private Map<String, List<Dijkstra.Edge>> graph;
    private Map<String, String> parkedVehicles;
    private WaitlistManager waitlist;
    private LogManager logger;
    private Random random;
    private Map<String, Map<String, Double>> distanceCache;

    // Facility information storage
    private Map<String, ParkingFacilities> facilityData;

    // Class to store facility information
    private static class ParkingFacilities {
        public double costPerHour;
        public boolean hasSecurity;
        public boolean hasEVCharging;
        public boolean hasCoveredParking;
        public boolean hasValet;
        public int facilityScore;
        
        public ParkingFacilities(double costPerHour, boolean hasSecurity, 
                               boolean hasEVCharging, boolean hasCoveredParking,
                               boolean hasValet, int facilityScore) {
            this.costPerHour = costPerHour;
            this.hasSecurity = hasSecurity;
            this.hasEVCharging = hasEVCharging;
            this.hasCoveredParking = hasCoveredParking;
            this.hasValet = hasValet;
            this.facilityScore = facilityScore;
        }
    }

    public ParkingManager(List<ParkingLot> lots, Map<String, List<Dijkstra.Edge>> graph,
                          WaitlistManager waitlist, LogManager logger) {
        this.lots = lots != null ? lots : new ArrayList<>();
        this.graph = graph != null ? graph : new HashMap<>();
        this.waitlist = waitlist;
        this.logger = logger;
        this.random = new Random();
        this.distanceCache = new HashMap<>();
        this.facilityData = new HashMap<>();
        this.parkedVehicles = new HashMap<>(); // Initialize parkedVehicles
        
        // Initialize facility data for each parking lot
        initializeFacilityData();
    }

    // Initialize facility data for parking lots
    private void initializeFacilityData() {
        // Sample facility data - you can load this from a file or database
        facilityData.put("1", new ParkingFacilities(10.0, true, false, true, false, 7));
        facilityData.put("2", new ParkingFacilities(15.0, true, true, true, true, 9));
        facilityData.put("3", new ParkingFacilities(8.0, false, false, false, false, 4));
        facilityData.put("4", new ParkingFacilities(12.0, true, false, true, false, 6));
        facilityData.put("5", new ParkingFacilities(20.0, true, true, true, true, 10));
        
        // Add more lots as needed
        for (ParkingLot lot : lots) {
            if (!facilityData.containsKey(lot.id)) {
                // Default facility data for lots not in our sample
                facilityData.put(lot.id, new ParkingFacilities(
                    10.0 + random.nextDouble() * 10, // $10-20 per hour
                    random.nextBoolean(),            // Random security
                    random.nextBoolean(),            // Random EV charging
                    random.nextBoolean(),            // Random covered parking
                    random.nextBoolean(),            // Random valet
                    5 + random.nextInt(6)           // Score 5-10
                ));
            }
        }
    }

    // Enhanced: Get directions with all intermediate nodes and actual distances
    public List<String> getDirections(String from, String to) {
        if (from == null || to == null) {
            return Arrays.asList("❌ Invalid locations provided");
        }
        
        if (from.equalsIgnoreCase(to)) {
            return Arrays.asList("📍 " + from, "🎯 You are already at your destination!");
        }

        try {
            Dijkstra dj = new Dijkstra(graph);
            Dijkstra.Result res = dj.shortestPath(from.toUpperCase());
            
            // Check if destination is reachable
            Double totalDistance = res.dist.get(to.toUpperCase());
            if (totalDistance == null || Double.isInfinite(totalDistance)) {
                return Arrays.asList("📍 " + from, "❌ No route available to " + to, "🎯 " + to);
            }

            List<String> path = Dijkstra.reconstructPath(from.toUpperCase(), to.toUpperCase(), res.prev);
            
            if (path != null && !path.isEmpty()) {
                return enhancePathDisplayWithActualDistances(path, res.dist, graph);
            } else {
                return Arrays.asList("📍 " + from, "❌ No valid path found to " + to, "🎯 " + to);
            }
        } catch (Exception e) {
            System.out.println("⚠️ Route calculation error: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Arrays.asList("📍 " + from, "❌ Route calculation failed", "🎯 " + to);
    }

    // Enhanced path display with ALL nodes and ACTUAL distances between them
    private List<String> enhancePathDisplayWithActualDistances(List<String> path, 
                                                             Map<String, Double> distances,
                                                             Map<String, List<Dijkstra.Edge>> graph) {
        List<String> enhancedPath = new ArrayList<>();
        
        enhancedPath.add("🗺️  DETAILED ROUTE FROM " + path.get(0) + " TO " + path.get(path.size() - 1));
        enhancedPath.add("══════════════════════════════════════════════════════════════");
        
        double totalDistance = distances.get(path.get(path.size() - 1));
        double accumulatedDistance = 0.0;
        
        // Display starting point
        enhancedPath.add("📍 START: " + path.get(0));
        
        // Display all intermediate nodes with actual distances
        for (int i = 1; i < path.size(); i++) {
            String currentNode = path.get(i);
            String previousNode = path.get(i - 1);
            
            // Calculate actual distance between consecutive nodes
            double segmentDistance = getActualSegmentDistance(previousNode, currentNode, graph, distances);
            accumulatedDistance += segmentDistance;
            
            String directionArrow = getDirectionArrow(i, path.size());
            String nodeType = getNodeType(i, path.size());
            
            enhancedPath.add(String.format("   %s %5.1f km", directionArrow, segmentDistance));
            
            if (i == path.size() - 1) {
                enhancedPath.add("🎯 DESTINATION: " + currentNode);
            } else {
                enhancedPath.add(String.format("📍 %s %s", currentNode, nodeType));
            }
        }
        
        enhancedPath.add("══════════════════════════════════════════════════════════════");
        enhancedPath.add("📊 ROUTE SUMMARY:");
        enhancedPath.add("   📏 Total Distance: " + String.format("%.1f", totalDistance) + " km");
        enhancedPath.add("   🛣️  Total Waypoints: " + (path.size() - 2));
        enhancedPath.add("   ⏱️  Estimated Time: " + String.format("%.0f", (totalDistance / 40.0) * 60) + " min");
        enhancedPath.add("   🚗 Average Speed: 40 km/h");
        enhancedPath.add("   📍 Total Nodes: " + path.size());
        
        // Add turn-by-turn instructions
        enhancedPath.addAll(generateTurnByTurnInstructions(path, graph));
        
        return enhancedPath;
    }

    // Get actual distance between two consecutive nodes in the path
    private double getActualSegmentDistance(String from, String to, 
                                          Map<String, List<Dijkstra.Edge>> graph,
                                          Map<String, Double> distances) {
        // First try to find the direct edge in the graph
        if (graph.containsKey(from)) {
            for (Dijkstra.Edge edge : graph.get(from)) {
                if (edge.to.equals(to)) {
                    return edge.weight;
                }
            }
        }
        
        // Fallback: calculate distance from Dijkstra results
        if (distances.containsKey(from) && distances.containsKey(to)) {
            double fromDist = distances.get(from);
            double toDist = distances.get(to);
            return Math.abs(toDist - fromDist);
        }
        
        // Final fallback: use cached distance
        return getCachedDistance(from, to);
    }

    // Generate turn-by-turn instructions
    private List<String> generateTurnByTurnInstructions(List<String> path, Map<String, List<Dijkstra.Edge>> graph) {
        List<String> instructions = new ArrayList<>();
        instructions.add("🔄 TURN-BY-TURN DIRECTIONS:");
        
        for (int i = 0; i < path.size() - 1; i++) {
            String current = path.get(i);
            String next = path.get(i + 1);
            
            if (i == 0) {
                instructions.add("   🚦 Start at " + current);
            }
            
            if (i < path.size() - 2) {
                String afterNext = path.get(i + 2);
                instructions.add("   → Continue from " + current + " to " + next);
                
                // Add intersection information if available
                if (graph.containsKey(next) && graph.get(next).size() > 2) {
                    instructions.add("     ⚠️ Intersection ahead - stay on route to " + afterNext);
                }
            } else if (i == path.size() - 2) {
                instructions.add("   🏁 Arrive at destination: " + path.get(path.size() - 1));
            }
        }
        
        return instructions;
    }

    // Helper method to get direction arrows
    private String getDirectionArrow(int currentIndex, int totalSize) {
        if (currentIndex == totalSize - 1) return "🏁";
        if (currentIndex % 3 == 0) return "↘️";
        if (currentIndex % 3 == 1) return "➡️";
        return "↙️";
    }

    // Helper method to get node type description
    private String getNodeType(int index, int totalSize) {
        if (index == 0) return "[Start]";
        if (index == totalSize - 1) return "[Destination]";
        
        String[] types = {"[Intersection]", "[Landmark]", "[Checkpoint]", "[Waypoint]"};
        return types[index % types.length];
    }

    // Alternative method that returns detailed route information as object
    public RouteDetails getDetailedRoute(String from, String to) {
        RouteDetails routeDetails = new RouteDetails();
        
        try {
            Dijkstra dj = new Dijkstra(graph);
            Dijkstra.Result res = dj.shortestPath(from.toUpperCase());
            
            Double totalDistance = res.dist.get(to.toUpperCase());
            if (totalDistance == null || Double.isInfinite(totalDistance)) {
                routeDetails.setError("No route available");
                return routeDetails;
            }

            List<String> path = Dijkstra.reconstructPath(from.toUpperCase(), to.toUpperCase(), res.prev);
            
            if (path != null && !path.isEmpty()) {
                routeDetails.setPath(path);
                routeDetails.setTotalDistance(totalDistance);
                routeDetails.setSegmentDistances(calculateSegmentDistances(path, graph, res.dist));
                routeDetails.setEstimatedTime((totalDistance / 40.0) * 60); // 40 km/h average
            }
        } catch (Exception e) {
            routeDetails.setError("Route calculation failed: " + e.getMessage());
        }
        
        return routeDetails;
    }

    // Calculate actual distances for each segment
    private List<Double> calculateSegmentDistances(List<String> path, 
                                                 Map<String, List<Dijkstra.Edge>> graph,
                                                 Map<String, Double> distances) {
        List<Double> segmentDistances = new ArrayList<>();
        
        for (int i = 0; i < path.size() - 1; i++) {
            String from = path.get(i);
            String to = path.get(i + 1);
            double distance = getActualSegmentDistance(from, to, graph, distances);
            segmentDistances.add(distance);
        }
        
        return segmentDistances;
    }

    // Enhanced: Get distance with better error handling
    public double getDistance(String from, String to) {
        if (from.equalsIgnoreCase(to)) return 0.0;
        
        try {
            Dijkstra dj = new Dijkstra(graph);
            Dijkstra.Result res = dj.shortestPath(from.toUpperCase());
            Double dist = res.dist.get(to.toUpperCase());
            
            if (dist != null && !dist.isInfinite()) {
                return dist;
            }
        } catch (Exception e) {
            System.out.println("⚠️ Distance calculation error: " + e.getMessage());
        }
        
        // Fallback: calculate approximate distance
        return calculateFallbackDistance(from, to);
    }

    // Enhanced recommendation considering multiple factors
    public ParkingLot recommendNearestFromLocation(String userLocation) {
        List<ParkingLot> availableLots = getAvailableParkingLots(userLocation);
        
        if (availableLots.isEmpty()) {
            System.out.println("🔍 No available parking lots found");
            return null;
        }

        System.out.println("🎯 Found " + availableLots.size() + " available parking lots");

        // Calculate comprehensive scores considering all factors
        List<ParkingScore> scoredLots = calculateComprehensiveScores(userLocation, availableLots);
        
        if (scoredLots.isEmpty()) {
            return null;
        }

        // Display top candidates with all factors
        displayTopCandidatesWithDetails(scoredLots);

        // Select using weighted probability from top candidates
        ParkingLot selected = selectWeightedParking(scoredLots);
        
        // Display why this parking was selected
        displaySelectionReason(selected, scoredLots);
        
        return selected;
    }

    // Get facility information for a parking lot
    private ParkingFacilities getFacilities(ParkingLot lot) {
        return facilityData.getOrDefault(lot.id, 
            new ParkingFacilities(10.0, false, false, false, false, 5));
    }

    // Get facility string for display
    private String getFacilityString(ParkingLot lot) {
        ParkingFacilities facilities = getFacilities(lot);
        StringBuilder facilityStr = new StringBuilder();
        if (facilities.hasSecurity) facilityStr.append("🔒");
        if (facilities.hasEVCharging) facilityStr.append("⚡");
        if (facilities.hasCoveredParking) facilityStr.append("🏢");
        if (facilities.hasValet) facilityStr.append("🚗");
        return facilityStr.toString();
    }

    // Comprehensive scoring considering all factors
    private List<ParkingScore> calculateComprehensiveScores(String userLocation, List<ParkingLot> availableLots) {
        List<ParkingScore> scoredLots = new ArrayList<>();
        
        // First, calculate all distances
        Map<ParkingLot, Double> distances = new HashMap<>();
        for (ParkingLot lot : availableLots) {
            double distance = getCachedDistance(userLocation, lot.locationName);
            if (distance >= 0) {
                distances.put(lot, distance);
            }
        }
        
        if (distances.isEmpty()) return scoredLots;
        
        // Calculate normalization factors
        double maxDistance = distances.values().stream().max(Double::compare).get();
        double minDistance = distances.values().stream().min(Double::compare).get();
        double distanceRange = maxDistance - minDistance;
        
        // Find cost range for normalization
        double maxCost = availableLots.stream()
            .mapToDouble(lot -> getFacilities(lot).costPerHour)
            .max().orElse(20.0);
        double minCost = availableLots.stream()
            .mapToDouble(lot -> getFacilities(lot).costPerHour)
            .min().orElse(5.0);
        double costRange = maxCost - minCost;
        
        for (ParkingLot lot : availableLots) {
            Double distance = distances.get(lot);
            if (distance == null) continue;

            // Normalize factors to 0-1 scale (higher is better)
            double normalizedDistance = distanceRange > 0 ? 
                1.0 - ((distance - minDistance) / distanceRange) : 0.5;
            
            double normalizedAvailability = lot.availableSlots / (double) lot.totalSlots;
            double normalizedRating = (lot.rating - 1.0) / 4.0; // Convert 1-5 to 0-1
            
            // Cost factor (lower cost is better)
            double cost = getFacilities(lot).costPerHour;
            double normalizedCost = costRange > 0 ? 
                1.0 - ((cost - minCost) / costRange) : 0.5;
            
            // Facility factor
            double facilityFactor = calculateFacilityScore(lot);
            
            // Calculate final weighted score (higher is better)
            double finalScore = 
                normalizedDistance * 0.25 +        // 25% to distance
                normalizedAvailability * 0.20 +    // 20% to availability
                normalizedRating * 0.15 +          // 15% to user rating
                normalizedCost * 0.20 +            // 20% to cost
                facilityFactor * 0.20;             // 20% to facilities
            
            scoredLots.add(new ParkingScore(lot, finalScore, distance, normalizedCost, facilityFactor));
        }
        
        // Sort by score (descending - higher scores are better)
        scoredLots.sort((a, b) -> Double.compare(b.score, a.score));
        
        return scoredLots;
    }

    // Calculate facility score based on available amenities
    private double calculateFacilityScore(ParkingLot lot) {
        ParkingFacilities facilities = getFacilities(lot);
        double score = 0.0;
        
        // Base facility score
        score += facilities.facilityScore * 0.05; // 0-0.5 points
        
        // Bonus points for specific amenities
        if (facilities.hasSecurity) score += 0.15;
        if (facilities.hasEVCharging) score += 0.15;
        if (facilities.hasCoveredParking) score += 0.10;
        if (facilities.hasValet) score += 0.10;
        
        return Math.min(score, 1.0); // Cap at 1.0
    }

    // Display detailed information about top candidates
    private void displayTopCandidatesWithDetails(List<ParkingScore> scoredLots) {
        System.out.println("\n🏆 TOP PARKING CANDIDATES (Ranked by Overall Score)");
        System.out.println("=================================================================");
        
        for (int i = 0; i < Math.min(5, scoredLots.size()); i++) {
            ParkingScore ps = scoredLots.get(i);
            ParkingFacilities facilities = getFacilities(ps.lot);
            String facilityStr = getFacilityString(ps.lot);
            
            System.out.printf("%d. %s | %s | Available: %d/%d | Rating: %.1f | Cost: $%.2f/hr %s%n", 
                i + 1, ps.lot.id, ps.lot.locationName, ps.lot.availableSlots, 
                ps.lot.totalSlots, ps.lot.rating, facilities.costPerHour, facilityStr);
            
            System.out.printf("   📊 Overall Score: %.3f | 📏 Distance: %.1f km%n", 
                            ps.score, ps.distance);
            System.out.printf("   💰 Cost Factor: %.1f/1.0 | 🏆 Facility Score: %.1f/1.0%n",
                            ps.costFactor, ps.facilityFactor);
            
            // Display amenities
            StringBuilder amenities = new StringBuilder("   🎯 Amenities: ");
            if (facilities.hasSecurity) amenities.append("Security ");
            if (facilities.hasEVCharging) amenities.append("EV-Charging ");
            if (facilities.hasCoveredParking) amenities.append("Covered ");
            if (facilities.hasValet) amenities.append("Valet ");
            System.out.println(amenities.toString());
            System.out.println();
        }
    }

    // Weighted selection from top candidates
    private ParkingLot selectWeightedParking(List<ParkingScore> scoredLots) {
        int candidateCount = Math.min(3, scoredLots.size());
        List<ParkingScore> candidates = scoredLots.subList(0, candidateCount);
        
        // Use exponential weights (better scores get higher weights)
        double[] weights = new double[candidateCount];
        double totalWeight = 0;
        
        for (int i = 0; i < candidateCount; i++) {
            weights[i] = Math.exp(candidates.get(i).score * 3); // Exponential weighting
            totalWeight += weights[i];
        }
        
        // Display selection probabilities
        System.out.println("⚖️ SELECTION PROBABILITIES:");
        for (int i = 0; i < candidateCount; i++) {
            double probability = (weights[i] / totalWeight) * 100;
            System.out.printf("   %s: %.1f%% chance%n", 
                            candidates.get(i).lot.locationName, probability);
        }
        
        // Select based on weights
        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        
        for (int i = 0; i < candidateCount; i++) {
            cumulativeWeight += weights[i];
            if (randomValue <= cumulativeWeight) {
                return candidates.get(i).lot;
            }
        }
        
        return candidates.get(0).lot;
    }

    // Display why a particular parking was selected
    private void displaySelectionReason(ParkingLot selected, List<ParkingScore> scoredLots) {
        ParkingScore selectedScore = scoredLots.stream()
            .filter(ps -> ps.lot == selected)
            .findFirst()
            .orElse(null);
            
        if (selectedScore == null) return;
        
        ParkingFacilities facilities = getFacilities(selected);
        
        System.out.println("\n✅ SELECTED: " + selected.locationName);
        System.out.println("📋 SELECTION REASONS:");
        System.out.printf("   📊 Overall Score: %.3f/1.0%n", selectedScore.score);
        System.out.printf("   📏 Distance: %.1f km%n", selectedScore.distance);
        System.out.printf("   🅿️ Availability: %d/%d slots%n", 
                         selected.availableSlots, selected.totalSlots);
        System.out.printf("   ⭐ User Rating: %.1f/5.0%n", selected.rating);
        System.out.printf("   💰 Cost: $%.2f per hour%n", facilities.costPerHour);
        System.out.printf("   🏆 Facilities: %.1f/1.0%n", selectedScore.facilityFactor);
        
        StringBuilder reasons = new StringBuilder("   🎯 Key Features: ");
        if (facilities.hasSecurity) reasons.append("Secure ");
        if (facilities.hasEVCharging) reasons.append("EV-Friendly ");
        if (facilities.hasCoveredParking) reasons.append("Covered ");
        if (facilities.hasValet) reasons.append("Valet-Service ");
        System.out.println(reasons.toString());
        
        logger.log("SMART_RECOMMEND", String.format(
            "User selected %s | Score: %.3f | Distance: %.1fkm | Cost: $%.2f | Facilities: %.1f",
            selected.locationName, selectedScore.score, selectedScore.distance,
            facilities.costPerHour, selectedScore.facilityFactor
        ));
    }

    // Fallback distance calculation when no route is found
    private double calculateFallbackDistance(String from, String to) {
        // Simple fallback based on string comparison
        int baseDistance = Math.abs(from.hashCode() - to.hashCode()) % 10 + 2;
        return baseDistance;
    }

    // Enhanced ParkingScore class with additional factors
    private static class ParkingScore {
        ParkingLot lot;
        double score;           // Overall score (0-1, higher is better)
        double distance;        // Distance in km
        double costFactor;      // Cost factor (0-1, higher means cheaper)
        double facilityFactor;  // Facility factor (0-1, higher means better facilities)
        
        ParkingScore(ParkingLot lot, double score, double distance, 
                    double costFactor, double facilityFactor) {
            this.lot = lot;
            this.score = score;
            this.distance = distance;
            this.costFactor = costFactor;
            this.facilityFactor = facilityFactor;
        }
    }

    // Route details class for comprehensive route information
    public static class RouteDetails {
        private List<String> path;
        private List<Double> segmentDistances;
        private double totalDistance;
        private double estimatedTime;
        private String error;
        
        public RouteDetails() {
            this.path = new ArrayList<>();
            this.segmentDistances = new ArrayList<>();
        }
        
        // Getters and setters
        public List<String> getPath() { return path; }
        public void setPath(List<String> path) { this.path = path; }
        
        public List<Double> getSegmentDistances() { return segmentDistances; }
        public void setSegmentDistances(List<Double> segmentDistances) { this.segmentDistances = segmentDistances; }
        
        public double getTotalDistance() { return totalDistance; }
        public void setTotalDistance(double totalDistance) { this.totalDistance = totalDistance; }
        
        public double getEstimatedTime() { return estimatedTime; }
        public void setEstimatedTime(double estimatedTime) { this.estimatedTime = estimatedTime; }
        
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        
        public boolean hasError() { return error != null && !error.isEmpty(); }
        
        public void printDetailedRoute() {
            if (hasError()) {
                System.out.println("❌ " + error);
                return;
            }
            
            System.out.println("🗺️  COMPREHENSIVE ROUTE DETAILS");
            System.out.println("══════════════════════════════════════════════════════════════");
            System.out.printf("📍 START: %s\n", path.get(0));
            System.out.printf("🎯 DESTINATION: %s\n", path.get(path.size() - 1));
            System.out.printf("📏 TOTAL DISTANCE: %.1f km\n", totalDistance);
            System.out.printf("⏱️  ESTIMATED TIME: %.0f minutes\n", estimatedTime);
            System.out.println("══════════════════════════════════════════════════════════════");
            
            double accumulated = 0;
            for (int i = 0; i < path.size() - 1; i++) {
                double segment = segmentDistances.get(i);
                accumulated += segment;
                
                if (i == 0) {
                    System.out.printf("🚦 START: %s\n", path.get(i));
                }
                
                System.out.printf("   ↓ %.1f km\n", segment);
                
                if (i == path.size() - 2) {
                    System.out.printf("🏁 ARRIVE: %s\n", path.get(i + 1));
                } else {
                    System.out.printf("📍 %s [Waypoint %d]\n", path.get(i + 1), i + 1);
                }
            }
            
            System.out.println("══════════════════════════════════════════════════════════════");
            System.out.printf("📊 SUMMARY: %d nodes, %.1f km total, %.0f min estimated\n", 
                            path.size(), totalDistance, estimatedTime);
        }
    }

    // Get available parking lots with filtering options
    public List<ParkingLot> getAvailableParkingLots(String excludeLocation) {
        List<ParkingLot> available = new ArrayList<>();
        for (ParkingLot lot : lots) {
            if (lot.availableSlots > 0 && 
                (excludeLocation == null || !lot.locationName.equalsIgnoreCase(excludeLocation))) {
                available.add(lot);
            }
        }
        return available;
    }
    
    public List<ParkingLot> getAvailableParkingLots() {
        return getAvailableParkingLots(null);
    }

    // Reserve at specific lot
    public boolean reserveVehicleAtLot(String vehicleNumber, String lotId) {
        vehicleNumber = vehicleNumber.toUpperCase();

        if (parkedVehicles.containsKey(vehicleNumber)) {
            String currentLot = parkedVehicles.get(vehicleNumber);
            System.out.println("❌ Vehicle already parked at lot: " + currentLot);
            logger.log("RESERVE_FAIL", vehicleNumber + " - Already at lot " + currentLot);
            return false;
        }

        ParkingLot targetLot = getParkingById(lotId);
        if (targetLot == null) {
            System.out.println("❌ Parking lot " + lotId + " not found");
            return false;
        }

        if (targetLot.availableSlots > 0) {
            targetLot.availableSlots--;
            parkedVehicles.put(vehicleNumber, lotId);
            logger.log("PARK", vehicleNumber + " at lot " + lotId + " | Slots left: " + targetLot.availableSlots);
            
            // Display parking cost and facilities
            ParkingFacilities facilities = getFacilities(targetLot);
            System.out.println("✅ Vehicle " + vehicleNumber + " parked at " + targetLot.locationName);
            System.out.println("   💰 Parking cost: $" + facilities.costPerHour + " per hour");
            if (facilities.hasSecurity) System.out.println("   🔒 Secure parking available");
            if (facilities.hasEVCharging) System.out.println("   ⚡ EV charging available");
            if (facilities.hasCoveredParking) System.out.println("   🏢 Covered parking available");
            if (facilities.hasValet) System.out.println("   🚗 Valet service available");
            
            return true;
        }

        System.out.println("❌ Lot " + lotId + " is full - added to waitlist");
        waitlist.add(vehicleNumber);
        logger.log("WAITLIST_ADD", vehicleNumber + " for lot " + lotId);
        return false;
    }

    // ✅ Prints the nodes in the shortest path from start to destination
public void printShortestPathNodes(String start, String destination) {
    if (!graph.containsKey(start) || !graph.containsKey(destination)) {
        System.out.println("❌ Invalid start or destination node.");
        return;
    }

    try {
        // Run Dijkstra
        Dijkstra dijkstra = new Dijkstra(graph);
        Dijkstra.Result result = dijkstra.shortestPath(start);

        // Reconstruct shortest path
        List<String> path = Dijkstra.reconstructPath(start, destination, result.prev);

        if (path == null || path.isEmpty()) {
            System.out.println("❌ No path found from " + start + " to " + destination);
            return;
        }

        // Print all nodes (in order)
        System.out.println("\n🚗 Shortest Path from " + start + " to " + destination + ":");
        System.out.println(String.join(" → ", path));

        // Optionally show total distance
        System.out.printf("📏 Total Distance: %.2f km%n", result.dist.get(destination));

    } catch (Exception e) {
        System.out.println("⚠️ Error finding shortest path: " + e.getMessage());
    }
}


    // Cached distance calculation
    private double getCachedDistance(String from, String to) {
        if (from.equalsIgnoreCase(to)) return 0.0;
        
        if (distanceCache.containsKey(from) && distanceCache.get(from).containsKey(to)) {
            return distanceCache.get(from).get(to);
        }
        if (distanceCache.containsKey(to) && distanceCache.get(to).containsKey(from)) {
            return distanceCache.get(to).get(from);
        }
        
        double distance = calculateDistance(from, to);
        
        distanceCache.computeIfAbsent(from, k -> new HashMap<>()).put(to, distance);
        distanceCache.computeIfAbsent(to, k -> new HashMap<>()).put(from, distance);
        
        return distance;
    }

    // Distance calculation with error handling
    private double calculateDistance(String from, String to) {
        try {
            Dijkstra dj = new Dijkstra(graph);
            Dijkstra.Result res = dj.shortestPath(from.toUpperCase());
            Double dist = res.dist.get(to.toUpperCase());
            return (dist != null && !dist.isInfinite()) ? dist : -1.0;
        } catch (Exception e) {
            System.out.println("⚠️ Distance calculation error: " + e.getMessage());
            return -1.0;
        }
    }

    // Reserve vehicle with automatic waitlist assignment
    public boolean reserveVehicle(String vehicleNumber) {
        vehicleNumber = vehicleNumber.toUpperCase();

        if (parkedVehicles.containsKey(vehicleNumber)) {
            System.out.println("❌ Vehicle already parked at lot: " + parkedVehicles.get(vehicleNumber));
            return false;
        }

        ParkingLot bestLot = findBestAvailableLot();
        if (bestLot != null) {
            return reserveVehicleAtLot(vehicleNumber, bestLot.id);
        }

        System.out.println("❌ No slots available - added to waitlist");
        waitlist.add(vehicleNumber);
        logger.log("WAITLIST_ADD", vehicleNumber);
        return false;
    }

    // Find best available lot considering multiple factors
    private ParkingLot findBestAvailableLot() {
        List<ParkingLot> available = getAvailableParkingLots();
        if (available.isEmpty()) return null;
        
        return available.stream()
                .max(Comparator.comparingDouble((ParkingLot lot) -> lot.availableSlots)
                        .thenComparingDouble(lot -> lot.rating))
                .orElse(null);
    }

    // Free vehicle with automatic waitlist processing
    public boolean freeByVehicle(String vehicleNumber) {
        vehicleNumber = vehicleNumber.toUpperCase();

        if (!parkedVehicles.containsKey(vehicleNumber)) {
            System.out.println("❌ Vehicle " + vehicleNumber + " not found in parking system");
            return false;
        }

        String lotId = parkedVehicles.remove(vehicleNumber);
        ParkingLot lot = getParkingById(lotId);

        if (lot != null) {
            int previousSlots = lot.availableSlots;
            lot.availableSlots++;
            int freedSlots = lot.availableSlots - previousSlots;
            
            logger.log("FREE", vehicleNumber + " from lot " + lotId + 
                      " | Freed slots: " + freedSlots + " | Total available: " + lot.availableSlots);
            System.out.println("✅ Vehicle " + vehicleNumber + " freed from lot " + lotId + 
                             " | Slots now: " + lot.availableSlots + "/" + lot.totalSlots);

            processWaitlist();
            return true;
        }

        System.out.println("❌ Could not find parking lot for vehicle: " + vehicleNumber);
        return false;
    }

    // Automatic waitlist processing
    private void processWaitlist() {
        int assignedCount = 0;
        while (!waitlist.isEmpty() && hasAvailableSlots()) {
            String nextVehicle = waitlist.pop();
            if (reserveVehicle(nextVehicle)) {
                assignedCount++;
                logger.log("WAITLIST_ASSIGN", "Auto-assigned " + nextVehicle);
            } else {
                break;
            }
        }
        
        if (assignedCount > 0) {
            System.out.println("🔄 Automatically assigned " + assignedCount + " vehicles from waitlist");
        }
    }

    // Emergency free slot with detailed reporting
    public void emergencyFreeSlot(Scanner sc) {
        System.out.print("Enter parking lot ID to emergency free: ");
        String lotId = sc.nextLine().trim();

        ParkingLot lot = getParkingById(lotId);
        if (lot == null) {
            System.out.println("❌ Invalid lot ID: " + lotId);
            return;
        }

        List<String> freedVehicles = new ArrayList<>();
        Iterator<Map.Entry<String, String>> it = parkedVehicles.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            if (entry.getValue().equals(lotId)) {
                freedVehicles.add(entry.getKey());
                it.remove();
                logger.log("EMERGENCY_FREE", entry.getKey() + " from lot " + lotId);
            }
        }

        int previousSlots = lot.availableSlots;
        lot.availableSlots = lot.totalSlots;
        int actuallyFreed = lot.totalSlots - previousSlots;

        System.out.println("🚨 EMERGENCY FREE COMPLETED:");
        System.out.println("✅ Freed " + freedVehicles.size() + " vehicles: " + freedVehicles);
        System.out.println("✅ Freed " + actuallyFreed + " slots from lot " + lotId);
        System.out.println("📍 Lot " + lotId + " now has " + lot.availableSlots + "/" + lot.totalSlots + " slots available");

        logger.log("EMERGENCY_FREE_SUMMARY", "Lot " + lotId + 
                  " | Vehicles freed: " + freedVehicles.size() + 
                  " | Slots freed: " + actuallyFreed);

        processWaitlist();
    }

    // Helper method to check if any slots are available
    private boolean hasAvailableSlots() {
        return lots.stream().anyMatch(lot -> lot.availableSlots > 0);
    }

    // Get parking by ID
    public ParkingLot getParkingById(String id) {
        for (ParkingLot lot : lots) {
            if (lot.id.equals(id)) return lot;
        }
        return null;
    }

    // Print comprehensive parking status
    public void printParkingStatus() {
        System.out.println("\n--- PARKING SYSTEM STATUS ---");
        
        int totalSlots = lots.stream().mapToInt(lot -> lot.totalSlots).sum();
        int availableSlots = lots.stream().mapToInt(lot -> lot.availableSlots).sum();
        int occupiedSlots = totalSlots - availableSlots;
        
        System.out.printf("Overall: %d/%d slots available (%.1f%% occupancy)%n", 
                         availableSlots, totalSlots, (occupiedSlots / (double) totalSlots) * 100);
        
        System.out.println("\n--- Individual Lots ---");
        for (ParkingLot lot : lots) {
            String status = lot.availableSlots > 0 ? "🟢" : "🔴";
            ParkingFacilities facilities = getFacilities(lot);
            String facilityStr = getFacilityString(lot);
            System.out.printf("%s %s | %s | Available: %d/%d | Rating: %.1f | Cost: $%.2f/hr %s%n",
                             status, lot.id, lot.locationName, 
                             lot.availableSlots, lot.totalSlots, lot.rating, 
                             facilities.costPerHour, facilityStr);
        }
        
        System.out.println("\n--- Parked Vehicles ---");
        if (parkedVehicles.isEmpty()) {
            System.out.println("No vehicles currently parked");
        } else {
            parkedVehicles.forEach((vehicle, lot) -> 
                System.out.println("🚗 " + vehicle + " → Lot " + lot));
        }
        
        System.out.println("\n--- Waitlist ---");
        System.out.println("Waitlisted vehicles: " + (waitlist.isEmpty() ? "0" : "Some vehicles"));
    }

    // Search vehicle with more details
    public int searchVehicle(String vehicleNumber) {
        vehicleNumber = vehicleNumber.toUpperCase();
        String lotId = parkedVehicles.get(vehicleNumber);
        
        if (lotId != null) {
            for (int i = 0; i < lots.size(); i++) {
                if (lots.get(i).id.equals(lotId)) {
                    ParkingLot lot = lots.get(i);
                    ParkingFacilities facilities = getFacilities(lot);
                    System.out.println("🚗 Vehicle " + vehicleNumber + " found:");
                    System.out.println("   📍 Lot: " + lot.id + " (" + lot.locationName + ")");
                    System.out.println("   ⭐ Rating: " + lot.rating);
                    System.out.println("   🅿️ Available slots: " + lot.availableSlots + "/" + lot.totalSlots);
                    System.out.println("   💰 Cost: $" + facilities.costPerHour + " per hour");
                    System.out.println("   🏆 Facility Score: " + facilities.facilityScore + "/10");
                    return i + 1;
                }
            }
        }
        
        System.out.println("❌ Vehicle " + vehicleNumber + " not found in parking system");
        return -1;
    }

    // Persist data with backup notification
    public void persistParkingData(String filepath) {
        try {
            FileUtil.saveParkingData(filepath, lots);
            System.out.println("💾 Parking data saved successfully");
            logger.log("DATA_SAVE", "Parking data persisted | Lots: " + lots.size());
        } catch (Exception e) {
            System.out.println("❌ Error saving parking data: " + e.getMessage());
            logger.log("DATA_SAVE_ERROR", e.getMessage());
        }
    }

    // Clear cache utility
    public void clearCache() {
        distanceCache.clear();
        System.out.println("🗑️ Distance cache cleared");
    }

    // Get system statistics
    public void printStatistics() {
        System.out.println("\n--- SYSTEM STATISTICS ---");
        System.out.println("Total parking lots: " + lots.size());
        System.out.println("Total parked vehicles: " + parkedVehicles.size());
        System.out.println("Waitlisted vehicles: " + (waitlist.isEmpty() ? "0" : "Some"));
        System.out.println("Distance cache entries: " + distanceCache.size());
        
        long availableLots = lots.stream().filter(lot -> lot.availableSlots > 0).count();
        System.out.println("Lots with available slots: " + availableLots);
        
        // Enhanced statistics
        long secureLots = facilityData.values().stream()
            .filter(facilities -> facilities.hasSecurity)
            .count();
        long evLots = facilityData.values().stream()
            .filter(facilities -> facilities.hasEVCharging)
            .count();
            
        System.out.println("Secure parking lots: " + secureLots);
        System.out.println("EV charging lots: " + evLots);
    }
}