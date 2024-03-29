import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class server {
    private static final int PORT = 12345;//the portnumber of the server
    private static Set<PrintWriter> clientWriters = Collections.synchronizedSet(new HashSet<>());//stores the PrintWriter objects representing the clienst. also sychronises the threasd
    private static Map<PrintWriter, String> clientNames = Collections.synchronizedMap(new HashMap<>());//client name and printwriter mapping
    private static PrintWriter adminWriter; // Admin's PrintWriter
    private static String adminName; // Admin's name
    private static ExecutorService pool = Executors.newFixedThreadPool(20);
//this method starst the server
    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {//this shows the server is running and its lsietning
            System.out.println("Server is running on port " + PORT);
            while (true) {// this listens for clients
                Socket socket = serverSocket.accept();
                pool.execute(new Handler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();//this prints the stack trace if a  IOException occurs
        }
    }

    private static void log(String message) {//this logs messages with timestamps
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
        LocalDateTime now = LocalDateTime.now();
        System.out.println(dtf.format(now) + ": " + message);
    }

    private static synchronized void setAdmin(PrintWriter adminWriter, String adminName) {//sets up the admin
        server.adminWriter = adminWriter;
        server.adminName = adminName;
        log(adminName + " is now the admin.");
    }

    private static synchronized void notifyNewClient(PrintWriter writer) {//for clients when they first join
        writer.println("Welcome to the chat! The admin is: " + adminName);
        writer.println("You are connected to port: " + PORT);
    }

    // Inner class - Handler
    private static class Handler implements Runnable {
        private Socket socket;
        private PrintWriter writer;
        private BufferedReader reader;
        private String name;

        public Handler(Socket socket) {//this is to intialise the handler with client socket
            this.socket = socket;
        }

        private void sendMemberDetails(PrintWriter writer) {//this method sends the lsit of memebrs to clients who request it
            StringBuilder memberList = new StringBuilder("Members in the chat:\n");
            for (String member : clientNames.values()) {//iterates and appends client names to a list
                memberList.append("- ").append(member).append("\n");
            }
            writer.println(memberList.toString());//send the message to the clients
        }

        private void broadcast(String message) {// this broadcasts messages in the chat
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) { //syncronies the client data
                    writer.println(message);
                }
            }
        }

        private void broadcastLeave(String name) { // this is a braodcasting method for when cliets leave it will message in the chat
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(name + " has left the chat!");//the mesage other clienst will see
                }
            }
        }


        @Override
        public void run() {
            try {
                reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                writer = new PrintWriter(socket.getOutputStream(), true);

                String name;
                while (true) {
                    //enter their name
                    name = reader.readLine().trim();
                    // Check if the name is valid
                    if (name.isEmpty() || clientNames.containsValue(name)) {
                        // Notify the client that the name is invalid or already taken
                        writer.println("Name is invalid or already taken. Please choose another name.");
                    } else {
                        // Name is valid, break out of the loop
                        break;
                    }
                }

                // Notify the client that the name is accepted
                writer.println("Name accepted.");

                // Add the client's name to the chat room
                clientNames.put(writer, name);
                clientWriters.add(writer);
                log(name + " joined the chat.");

                // If the client is the first one, assign them as the admin
                if (clientNames.size() == 1) {
                    setAdmin(writer, name);
                }

                // Notify other clients about the new member
                notifyNewClient(writer);

                // Handle client communication
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.equals("leave")) { // Check if the client typed "leave"
                        // Clean up resources and remove the client from the chat room
                        clientNames.remove(writer);
                        clientWriters.remove(writer);
                        log(name + " left the chat.");
                        broadcastLeave(name);
                        if (name.equals(adminName)) {
                            // Admin left, need to assign new admin
                            if (!clientNames.isEmpty()) {
                                setAdmin(clientWriters.iterator().next(), clientNames.get(clientWriters.iterator().next()));
                            } else {
                                adminWriter = null;
                                adminName = null;
                            }
                        }
                        break; // Exit the loop when the client leaves
                    } else if (line.startsWith("members")) {
                        // Handle member details request
                        sendMemberDetails(writer);
                    }else if (line.equals("private")) {
                        //could not implement
                        broadcast("Feature unavailable");
                    } else {
                        // Broadcast message to all clients
                        broadcast(name + ": " + line);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                // Clean up resources and remove the client from the chat room
                if (name != null) {
                    clientNames.remove(writer);
                    clientWriters.remove(writer);
                    log(name + " left the chat.");
                    if (name.equals(adminName)) {
                        // Admin left, need to assign new admin
                        if (!clientNames.isEmpty()) {
                            setAdmin(clientWriters.iterator().next(), clientNames.get(clientWriters.iterator().next()));
                        } else {
                            adminWriter = null;
                            adminName = null;
                        }
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}