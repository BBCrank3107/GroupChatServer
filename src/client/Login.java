package client;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class Login extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label usernameLabel = new Label("Username:");
        TextField usernameField = new TextField();
        
        Label ipLabel = new Label("Server IP:");
        TextField ipField = new TextField();
        
        Button connectButton = new Button("Connect");
        connectButton.setOnAction(e -> {
            String username = usernameField.getText();
            String serverIP = ipField.getText();
            
            // Chuyển sang giao diện chat
            GroupChat chat = new GroupChat(username, serverIP);
            chat.start(new Stage());
            primaryStage.close();
        });

        VBox vbox = new VBox(10, usernameLabel, usernameField, ipLabel, ipField, connectButton);
        vbox.setAlignment(Pos.CENTER);

        Scene scene = new Scene(vbox, 300, 200);
        primaryStage.setTitle("Login");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
