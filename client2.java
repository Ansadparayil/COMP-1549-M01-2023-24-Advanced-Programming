import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client2 {
    // Properties
    private static final String SERVER_ADDRESS = "localhost"; // Change to server's IP address
    private static final int PORT = 12345;
    private static String name;
    private static String coordinator;
    private static PrintWriter writer;
    private static BufferedReader reader;
    // Main method
    public static void main(String[] args) {
        try (Socket socket = new Socket(SERVER_ADDRESS, PORT)) {
            System.out.println("Connected to server.");
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            // Perform name submission
            name = getName();
            writer.println(name);

            // Start message sending thread
            startMessageSendingThread();

            // Handle server communication
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Utility methods
    private static String getName() {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your name: ");
        return scanner.nextLine();
    }

    private static void startMessageSendingThread() {
        new Thread(() -> {
            try {
                BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
                String input;
                while ((input = consoleReader.readLine()) != null) {
                    // Send user input as message to the server
                    writer.println(input);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
