import java.util.*;

public class Dijkstra {
    public static class Edge {
        public String to;
        public double weight;
        
        public Edge(String to, double weight) {
            this.to = to;
            this.weight = weight;
        }
        
        @Override
        public String toString() {
            return "-> " + to + " (" + weight + "km)";
        }
    }

    public static class Result {
        public Map<String, Double> dist = new HashMap<>();
        public Map<String, String> prev = new HashMap<>();
        
        public Result() {
            dist = new HashMap<>();
            prev = new HashMap<>();
        }
    }

    private Map<String, List<Edge>> graph;

    public Dijkstra(Map<String, List<Edge>> graph) {
        this.graph = graph != null ? graph : new HashMap<>();
    }

    public Result shortestPath(String start) {
        Result result = new Result();
        if (graph.isEmpty() || !graph.containsKey(start)) {
            return result;
        }

        PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingDouble(n -> n.dist));
        Set<String> visited = new HashSet<>();

        // Initialize distances
        for (String node : graph.keySet()) {
            result.dist.put(node, Double.MAX_VALUE);
        }
        result.dist.put(start, 0.0);
        pq.offer(new Node(start, 0.0));

        while (!pq.isEmpty()) {
            Node current = pq.poll();
            if (visited.contains(current.id)) continue;
            visited.add(current.id);

            List<Edge> edges = graph.get(current.id);
            if (edges == null) continue;

            for (Edge edge : edges) {
                if (!visited.contains(edge.to)) {
                    double newDist = result.dist.get(current.id) + edge.weight;
                    Double currentDist = result.dist.get(edge.to);
                    if (currentDist == null || newDist < currentDist) {
                        result.dist.put(edge.to, newDist);
                        result.prev.put(edge.to, current.id);
                        pq.offer(new Node(edge.to, newDist));
                    }
                }
            }
        }
        return result;
    }

    // Fixed: Proper path reconstruction with all intermediate nodes
    public static List<String> reconstructPath(String from, String to, Map<String, String> prev) {
        List<String> path = new ArrayList<>();
        
        if (from == null || to == null || prev == null) {
            return path;
        }

        // If start and end are the same
        if (from.equals(to)) {
            path.add(from);
            return path;
        }

        // If no path exists
        if (!prev.containsKey(to)) {
            return path;
        }

        // Build path backwards from destination to start
        Stack<String> stack = new Stack<>();
        String current = to;
        
        while (current != null) {
            stack.push(current);
            current = prev.get(current);
            if (current != null && current.equals(from)) {
                stack.push(current);
                break;
            }
        }

        // If we didn't reach the start node, no valid path
        if (stack.isEmpty() || !stack.peek().equals(from)) {
            return new ArrayList<>();
        }

        // Reverse the path to get start-to-end order
        while (!stack.isEmpty()) {
            path.add(stack.pop());
        }

        return path;
    }

    private static class Node {
        String id;
        double dist;

        Node(String id, double dist) {
            this.id = id;
            this.dist = dist;
        }
    }
}