import java.io.*;
import java.util.*;

/*
 * Helper functions to load parking data, graph, waitlist and save back.
 */

public class FileUtil {

    public static List<ParkingLot> loadParkingLots(String filepath) throws Exception {
        List<ParkingLot> list = new ArrayList<>();
        File f = new File(filepath);
        if (!f.exists()) {
            System.out.println("Warning: parking file not found: " + filepath);
            return list;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                // id,location,total,available,rating,lat,lon
                if (p.length < 7) continue;
                String id = p[0].trim();
                String loc = p[1].trim().toUpperCase();
                int total = Integer.parseInt(p[2].trim());
                int avail = Integer.parseInt(p[3].trim());
                double rating = Double.parseDouble(p[4].trim());
                double lat = Double.parseDouble(p[5].trim());
                double lon = Double.parseDouble(p[6].trim());
                double costPerHour = 0.0; // default cost
                boolean secure = false;
                boolean covered = false;
                boolean evCharging = false;
                boolean valet = false;

                list.add(new ParkingLot(id, loc, total, avail, rating, 
                        costPerHour, secure, covered, evCharging, valet,
                        lat, lon));

            }
        }
        return list;
    }

    // graph file: Source,Dest,DistanceKM
    public static Map<String, List<Dijkstra.Edge>> loadGraph(String filepath) throws Exception {
        Map<String, List<Dijkstra.Edge>> map = new HashMap<>();
        File f = new File(filepath);
        if (!f.exists()) return map;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String a = p[0].trim().toUpperCase();
                String b = p[1].trim().toUpperCase();
                double d = Double.parseDouble(p[2].trim());
                map.putIfAbsent(a, new ArrayList<>());
                map.putIfAbsent(b, new ArrayList<>());
                map.get(a).add(new Dijkstra.Edge(b, d));
                map.get(b).add(new Dijkstra.Edge(a, d));
            }
        }
        return map;
    }

    // waitlist lines: vehicleNumber,timestamp
    public static Queue<String> loadWaitlist(String filepath) throws Exception {
        Queue<String> q = new LinkedList<>();
        File f = new File(filepath);
        if (!f.exists()) return q;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                q.add(p[0].trim().toUpperCase());
            }
        }
        return q;
    }

    public static void saveParkingData(String filepath, List<ParkingLot> lots) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath, false))) {
            for (ParkingLot pl : lots) {
                bw.write(String.format("%s,%s,%d,%d,%.1f,%.6f,%.6f\n",
                        pl.id, pl.locationName, pl.totalSlots, pl.availableSlots, pl.rating, pl.lat, pl.lon));
            }
        }
    }

    public static void saveWaitlist(String filepath, Queue<String> q) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath, false))) {
            for (String v : q) bw.write(v + "," + (System.currentTimeMillis()/1000) + "\n");
        }
    }

    public static void appendLog(String filepath, String line) throws Exception {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(filepath, true))) {
            bw.write(line + "\n");
        }
    }
}
