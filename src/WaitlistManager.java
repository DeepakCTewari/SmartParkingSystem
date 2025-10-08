import java.util.*;

/*
 * Simple queue-based waitlist with persistence file path.
 */
public class WaitlistManager {
    private Queue<String> queue;
    private String filepath;

    public WaitlistManager(String filepath) {
        this.filepath = filepath;
        try { this.queue = FileUtil.loadWaitlist(filepath); }
        catch (Exception ex) { this.queue = new LinkedList<>(); }
    }

    public void add(String vehicleNumber) {
        queue.add(vehicleNumber.toUpperCase());
        persist();
    }
    

    public String pop() {
        String v = queue.poll();
        persist();
        return v;
    }

    public boolean isEmpty() { return queue.isEmpty(); }

    public void persist() {
        try { FileUtil.saveWaitlist(filepath, queue); }
        catch (Exception ex) { System.out.println("Error saving waitlist: " + ex.getMessage()); }
    }

    public List<String> snapshot() {
        return new ArrayList<>(queue);
    }
}
