import java.io.RandomAccessFile;

public class LogManager {
    private String filepath;

    public LogManager(String filepath) { 
        this.filepath = filepath; 
    }

    public void log(String event, String details) {
        try {
            String time = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            FileUtil.appendLog(filepath, String.format("%s | %s | %s", time, event, details));
        } catch (Exception ex) {
            System.out.println("Log error: " + ex.getMessage());
        }
    }

    // Print last N lines
    public void printLastLines(int n) {
        try (RandomAccessFile raf = new RandomAccessFile(filepath, "r")) {
            long fileLength = raf.length();
            long pointer = fileLength - 1;
            int lines = 0;
            StringBuilder sb = new StringBuilder();

            while (pointer >= 0 && lines < n) {
                raf.seek(pointer);
                char c = (char) raf.read();
                if (c == '\n') lines++;
                sb.append(c);
                pointer--;
            }

            // reverse the string to correct order
            sb.reverse();
            String[] outputLines = sb.toString().split("\n");
            int start = Math.max(0, outputLines.length - n);
            for (int i = start; i < outputLines.length; i++) {
                System.out.println(outputLines[i]);
            }

        } catch (Exception e) {
            System.out.println("No logs or error reading logs: " + e.getMessage());
        }
    }
}
