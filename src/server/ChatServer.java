package server;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChatServer {
    private static List<ClientHandler> connectedClients = new ArrayList<>();
    
    public static List<ClientHandler> getConnectedClients() {
        return connectedClients;
    }

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(12345)) {
            System.out.println("Server is running...");
            new Thread(ChatServer::watchForFileChanges).start(); // Khởi động luồng theo dõi thay đổi file
            while (true) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                connectedClients.add(clientHandler);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Theo dõi thay đổi trong thư mục "messages" và thông báo cho các client liên quan
    private static void watchForFileChanges() {
        try {
            WatchService watchService = FileSystems.getDefault().newWatchService();
            Path path = Paths.get("messages");
            path.register(watchService, StandardWatchEventKinds.ENTRY_MODIFY);

            while (true) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                        String fileName = event.context().toString();
                        System.out.println("File modified: " + fileName);
                    }
                }
                key.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler extends Thread {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String username;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("Received request: " + request);

                if (request.startsWith("LOGIN|")) {
                    username = request.split("\\|")[1];
                    handleLogin(username);
                } else if (request.startsWith("GET_USER_LIST|")) {
                    String username = request.split("\\|")[1];
                    handleGetUserList(username);
                } else if (request.startsWith("PRIVATE|")) {
                    String[] parts = request.split("\\|");
                    String user1 = parts[1];
                    String user2 = parts[2];
                    String message = parts[3];
                    handlePrivateMessage(user1, user2, message);
                } else if (request.startsWith("GET_MESSAGES|")) {
                    String[] parts = request.split("\\|");
                    String user1 = parts[1];
                    String user2 = parts[2];
                    handleGetMessages(user1, user2);
                } else if (request.startsWith("GROUP_MESSAGE|")) {
                    String[] parts = request.split("\\|");
                    String user = parts[1];
                    String message = parts[2];
                    handleGroupMessage(user, message);
                } else if (request.startsWith("JOIN_GROUP|")) {
                    username = request.split("\\|")[1]; // Gán username
                    notifyGroupJoin(username); // Thông báo người dùng đã tham gia
                } else if (request.startsWith("LEAVE_GROUP|")) { // Phương thức mới cho việc rời nhóm
                	username = request.split("\\|")[1];
                    notifyGroupLeave(username); // Thông báo khi người dùng rời nhóm
                    break; // Kết thúc vòng lặp khi người dùng rời nhóm
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    private void notifyGroupJoin(String username) {
        String message = username + " đã tham gia đoạn chat.";
        for (ClientHandler client : ChatServer.getConnectedClients()) {
            client.sendMessage(message);
        }
    }
    
    private void notifyGroupLeave(String username) {
        String message = username + " đã rời nhóm.";
        for (ClientHandler client : ChatServer.getConnectedClients()) {
            client.sendMessage(message);
        }
    }

    // Gửi tin nhắn về client
    public void sendMessage(String message) {
        out.println(message);
        out.flush();
    }

    private void handleLogin(String username) {
        this.username = username;  // Đảm bảo username được gán vào đối tượng
        File userFolder = new File("messages/" + username);
        if (userFolder.exists() && userFolder.isDirectory()) {
            out.println("LOGIN_SUCCESS");
        } else {
            out.println("LOGIN_FAILED");
        }
    }

    private void handleGetUserList(String username) {
        File messageFolder = new File("messages");
        File[] folders = messageFolder.listFiles(File::isDirectory);

        StringBuilder userList = new StringBuilder();
        if (folders != null) {
            for (File folder : folders) {
                if (!folder.getName().equals(username)) {
                    userList.append(folder.getName()).append(",");
                }
            }
        }

        out.println(userList.toString());
    }

    private void handlePrivateMessage(String user1, String user2, String message) {
        try {
            String[] users = {user1, user2};
            Arrays.sort(users);
            String filename = users[0] + "_" + users[1] + ".txt";

            System.out.println("Received message from " + user1 + " to " + user2 + ": " + message);

            File messageDir = new File("messages");
            if (!messageDir.exists()) {
                messageDir.mkdirs();
            }

            File messageFile = new File(messageDir, filename);
            if (!messageFile.exists()) {
                messageFile.createNewFile();
            }

            // Ghi tin nhắn vào file
            FileWriter fileWriter = new FileWriter(messageFile, true);
            fileWriter.write(user1 + ": " + message + "\n");
            fileWriter.close();

            System.out.println("Message saved in " + filename);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleGetMessages(String user1, String user2) {
        try {
            String[] users = {user1, user2};
            Arrays.sort(users);
            String filename = users[0] + "_" + users[1] + ".txt";

            File messageFile = new File("messages/" + filename);
            if (messageFile.exists()) {
                BufferedReader fileReader = new BufferedReader(new FileReader(messageFile));
                String line;
                StringBuilder messages = new StringBuilder();
                while ((line = fileReader.readLine()) != null) {
                    messages.append(line).append("\n");
                }
                fileReader.close();
                out.println(messages.toString());
            } else {
                out.println("");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        out.flush();
    }

    private void handleGroupMessage(String username, String message) {
        try {
            File groupMessageFile = new File("messages/groupChat.txt");
            if (!groupMessageFile.exists()) {
                groupMessageFile.createNewFile();
            }

            // Ghi tin nhắn vào file
            FileWriter fileWriter = new FileWriter(groupMessageFile, true);
            fileWriter.write(username + ": " + message + "\n");
            fileWriter.close();

            // Gửi lại tin nhắn cho tất cả client
            for (ClientHandler client : ChatServer.getConnectedClients()) {
                client.sendMessage(username + ": " + message);
            }

            System.out.println("Group message saved and sent: " + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
