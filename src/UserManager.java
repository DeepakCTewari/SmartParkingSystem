import java.io.*;
import java.util.*;

public class UserManager {
    private Map<String, User> users = new HashMap<>();
    private String filepath;

    // Constructor
    public UserManager(String filepath) throws Exception {
        this.filepath = filepath;
        loadUsers();
    }

    // User class
    public static class User {
        public String username, password, type;

        public User(String username, String password, String type) {
            this.username = username;
            this.password = password;
            this.type = type;
        }
    }

    // Load users from file
    private void loadUsers() throws Exception {
        File f = new File(filepath);

        // Ensure parent directories exist
        File parentDir = f.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs(); // create missing directories
        }

        // Create file if it doesn't exist
        if (!f.exists()) f.createNewFile();

        // Read existing users
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    users.put(parts[0], new User(parts[0], parts[1], parts[2]));
                }
            }
        }
    }

    // Save users to file
    public void saveUsers() throws Exception {
        try (PrintWriter pw = new PrintWriter(new FileWriter(filepath))) {
            for (User u : users.values()) {
                pw.println(u.username + "," + u.password + "," + u.type);
            }
        }
    }

    // Login user
    public User login(String username, String password) {
        if (users.containsKey(username)) {
            User u = users.get(username);
            if (u.password.equals(password)) return u;
        }
        return null;
    }

    // Add new user
    public boolean addUser(String username, String password, String type) throws Exception {
        if (users.containsKey(username)) return false;
        users.put(username, new User(username, password, type));
        saveUsers();
        return true;
    }

    // List all usernames
    public Set<String> listUsers() {
        return users.keySet();
    }
}
