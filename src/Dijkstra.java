import java.util.*;

/*
 * Dijkstra with path reconstruction.
 * Edge is a simple public inner-type used by FileUtil and ParkingManager.
 */
public class Dijkstra {

    public static class Edge {
        public String to;
        public double weight;
        public Edge(String to, double weight) { this.to = to; this.weight = weight; }
    }

    private Map<String, List<Edge>> graph;

    public Dijkstra(Map<String, List<Edge>> graph) {
        this.graph = graph;
    }

    public static class Result {
        public Map<String, Double> dist;
        public Map<String, String> prev;
        public Result(Map<String, Double> dist, Map<String, String> prev) { this.dist = dist; this.prev = prev; }
    }

    public Result shortestPath(String start) {
        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String node : graph.keySet()) dist.put(node, Double.MAX_VALUE);
        if (!dist.containsKey(start)) dist.put(start, 0.0); // in case start is a temporary node like USER
        dist.put(start, 0.0);

        PriorityQueue<Map.Entry<String, Double>> pq = new PriorityQueue<>(Map.Entry.comparingByValue());
        pq.offer(new AbstractMap.SimpleEntry<>(start, 0.0));

        while (!pq.isEmpty()) {
            Map.Entry<String, Double> e = pq.poll();
            String u = e.getKey();
            double d = e.getValue();
            if (d > dist.getOrDefault(u, Double.MAX_VALUE)) continue;
            for (Edge edge : graph.getOrDefault(u, Collections.emptyList())) {
                double nd = dist.get(u) + edge.weight;
                if (nd < dist.getOrDefault(edge.to, Double.MAX_VALUE)) {
                    dist.put(edge.to, nd);
                    prev.put(edge.to, u);
                    pq.offer(new AbstractMap.SimpleEntry<>(edge.to, nd));
                }
            }
        }
        return new Result(dist, prev);
    }

    public static List<String> reconstructPath(String start, String end, Map<String, String> prev) {
        LinkedList<String> path = new LinkedList<>();
        String cur = end;
        while (cur != null) {
            path.addFirst(cur);
            cur = prev.get(cur);
        }
        if (path.isEmpty() || !path.getFirst().equals(start)) return Collections.emptyList();
        return path;
    }
}
