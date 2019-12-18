package g53sqm.chat.client;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public class ClientSplashGUI {

    ClientSplashController controller;
    GridPane gridPane;
    TextField serverIpField;
    TextField serverPortField;
    TextField usernameField;
    Button connect;
    Text actionTarget;

    public ClientSplashGUI(ClientSplashController controller){

        this.controller = controller;
        init();
    }

    private void init(){

        gridPane = new GridPane();
        gridPane.setPrefSize(500,500);
        gridPane.setPadding(new Insets(25));
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setHgap(10);
        gridPane.setVgap(10);

        Label serverIp = new Label("Server IP");
        gridPane.add(serverIp,0,0);
        serverIpField = new TextField();
        gridPane.add(serverIpField,1,0);

        Label serverPort = new Label("Server Port");
        gridPane.add(serverPort,0,1);
        serverPortField = new TextField();
        gridPane.add(serverPortField,1,1);

        Label username = new Label("Username");
        gridPane.add(username,0,2);
        usernameField = new TextField();
        gridPane.add(usernameField,1,2);

        connect = new Button("Connect");
        connect.setOnAction(event -> {
           controller.validate();
        });

        HBox container = new HBox(10);
        container.setAlignment(Pos.BOTTOM_RIGHT);
        container.getChildren().add(connect);
        gridPane.add(container,1,3);

        actionTarget = new Text();
        gridPane.add(actionTarget,0,4,2,1);
    }


    public Scene getScene() {

        return new Scene(gridPane);
    }

}
