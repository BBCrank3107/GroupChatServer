package client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;

public class GroupChat extends Application {

    private String username;
    private String serverIP;
    private PrintWriter out;
    private BufferedReader in;
    private TextArea chatArea;
    private ListView<String> memberList;

    public GroupChat(String username, String serverIP) {
        this.username = username;
        this.serverIP = serverIP;
    }

    @Override
    public void start(Stage primaryStage) {
        BorderPane root = new BorderPane();
        
        // Khu vực chat
        chatArea = new TextArea();
        chatArea.setEditable(false);
        root.setCenter(chatArea);

        // Khu vực nhập tin nhắn
        TextField messageField = new TextField();
        Button sendButton = new Button("Send");
        sendButton.setOnAction(e -> sendMessage(messageField.getText()));

        HBox inputBox = new HBox(10, messageField, sendButton);
        inputBox.setPadding(new Insets(10));
        root.setBottom(inputBox);

        // Khu vực danh sách thành viên
        memberList = new ListView<>();
        VBox memberBox = new VBox(new Label("Members"), memberList);
        memberBox.setPrefWidth(150);
        root.setRight(memberBox);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Group Chat - " + username);
        primaryStage.setScene(scene);
        primaryStage.show();

        connectToServer();
    }

    private void connectToServer() {
        try {
            Socket socket = new Socket(serverIP, 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Luồng để nhận tin nhắn từ server
            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                    	final String receivedMessage = message;
                        Platform.runLater(() -> chatArea.appendText(receivedMessage + "\n"));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            out.println(username + ": " + message);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}