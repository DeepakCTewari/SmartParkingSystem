import java.io.*;
import java.util.*;

/*
 * Simple location name -> lat/lon DB loader from data/location_db.txt
 * Format: Name,Lat,Lon
 */
public class GeoDB {
    private static Map<String, double[]> db = new HashMap<>();

    public static void load(String filepath) throws Exception {
        db.clear();
        File f = new File(filepath);
        if (!f.exists()) return;
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] p = line.split(",");
                if (p.length < 3) continue;
                String name = p[0].toUpperCase();
                double lat = Double.parseDouble(p[1]);
                double lon = Double.parseDouble(p[2]);
                db.put(name, new double[]{lat, lon});
            }
        }
    }

    public static double[] get(String name) {
        if (name == null) return new double[]{0,0};
        return db.getOrDefault(name.toUpperCase(), new double[]{0,0});
    }

    public static Set<String> availableNames() {
        return db.keySet();
    }
}
