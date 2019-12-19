package g53sqm.chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class ClientUI extends Application {

    private Stage stage;
    private GridPane gridPane;
    private TextArea chat;
    private TextArea input;
    private Client client;
    private HBox btmContainer;
    private Button send;
    private ClientUI cUI;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        stage = primaryStage;

        // setup all components of the application
        setupGUI();

        //set up the scene
        Scene scene = new Scene(gridPane);
        stage.setScene(scene);
        stage.setMinHeight(500);
        stage.setMinWidth(500);
        stage.setTitle("Client");
        stage.show();

        cUI = this;
        connectServer(cUI);
    }

    private void setupGUI(){

        // base layout of the the chat window
        gridPane = new GridPane();
        gridPane.setPrefSize(400,400);
        gridPane.setPadding(new Insets(10,10,10,10));
        gridPane.setVgap(8);
        gridPane.setHgap(8);

        //chat text screen
        chat = new TextArea();
        chat.setWrapText(true);
        chat.setEditable(false);
        chat.setMouseTransparent(false);
        chat.setFocusTraversable(false);

        // container for the input and send button
        btmContainer = new HBox();

        // input text area properties
        input = new TextArea();
        input.setPrefRowCount(4);
        input.setWrapText(true);
        //Wait until connection is successful
        input.setEditable(false);
        input.setDisable(true);

        input.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode().equals(KeyCode.ENTER))
                {

                    doSend();
                    input.clear();
                }
            }
        });



        //send button properties
        send = new Button();
        send.setText("Send");
        send.setMinSize(50,35);
        send.setOnAction(event -> {
            doSend();
        });
        send.setDisable(true);

        //set margins and combine all components into the container
        HBox.setMargin(input,new Insets(5));
        HBox.setMargin(send,new Insets(5));

        //input area responsive
        HBox.setHgrow(input, Priority.ALWAYS); // so that the input area is responsive
        //constant button size
        HBox.setHgrow(send,Priority.NEVER);
        btmContainer.getChildren().addAll(input,send);

        //set horizontal resizing for columns
        ColumnConstraints colCons1 = new ColumnConstraints();
        colCons1.setHgrow(Priority.ALWAYS); // allow the main chat area to be responsive
        gridPane.getColumnConstraints().addAll(colCons1);

        //set vertical resizing for rows
        RowConstraints rowCons1 = new RowConstraints();
        rowCons1.setVgrow(Priority.ALWAYS);
        gridPane.getRowConstraints().add(rowCons1);

        //add elements into gridPane
        gridPane.add(chat, 0,0,1,1);
        gridPane.add(btmContainer,0,1,2,1);

    }

    private void connectServer(ClientUI cUI){

        client = new Client("127.0.0.1", 9001, cUI);

        // after connecting
        Platform.runLater(()->{
            input.setEditable(true);
            input.setDisable(false);
            send.setDisable(false);

            // send QUIT when UI is closed
            stage.setOnCloseRequest(event -> {
                client.sendMessage("QUIT");
                client.closeConnection();
            });
        });

    }

    private void doSend(){

        String msg = input.getText();

        if(msg.trim().length() == 0){
            return ;
        }

        client.sendMessage(msg.trim());

        if(msg.equals("QUIT")){
            stage.close();
            client.closeConnection();
        }
    }

    // add server response to chat area
    public void appendChat(String msg){
        Platform.runLater(()->chat.appendText(msg + '\n'));
    }
}

