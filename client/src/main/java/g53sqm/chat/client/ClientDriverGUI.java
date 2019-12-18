package g53sqm.chat.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;

public class ClientDriverGUI extends Application {

    private Client client;
    private ClientSplashGUI splash;
    private ClientSplashController splashController;
    private ClientChatGUI chat;
    private ClientChatController chatController;
    private Stage primaryStage;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        splashController = new ClientSplashController();
        splashController.setDriver(this);
        splash = new ClientSplashGUI(splashController);
        splashController.setView(splash);

        primaryStage.setScene(splash.getScene());
        primaryStage.setMinWidth(525);
        primaryStage.setMinHeight(525);
        primaryStage.show();

        chatController = new ClientChatController();
        chatController.setDriver(this);
        chat = new ClientChatGUI(chatController);
        chatController.setView(chat);

    }


    public static void main(String[] args){
        launch(args);
    }

    public void transition(boolean transition){
        if(transition){
            Platform.runLater(()->{
                chatController.setClient(splashController.getClient());

                primaryStage.setScene(chat.getScene());
            });
        }
        else{
            chatController.clean(); // clean failed login attempts from the chat area

            splashController.getClient().sendMessage("QUIT");
            splashController.getClient().closeConnection();

        }

    }

    public void appendChat(String msg){
        chatController.appendChat(msg);
    }


    public void setSplashErrorMessage(String msg){
        splashController.setErrorMessage(msg);
    }

    public Stage getPrimaryStage(){
        return primaryStage;
    }

    public String getUsername(){
        return splashController.getUsername();
    }

}
