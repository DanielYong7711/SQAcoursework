package g53sqm.chat.client;

import javafx.application.Platform;
import javafx.scene.paint.Color;

public class ClientSplashController {

    private ClientDriverGUI driver;
    private ClientSplashGUI splash;
    private Client client;

    private String serverIp;
    private int serverPort;
    private String username;

    public void setView(ClientSplashGUI splash){
        this.splash = splash;
    }

    public void setDriver(ClientDriverGUI driver){
        this.driver = driver;
    }

    public void validate(){

        // disable button to prevent button spam
        Platform.runLater(()->splash.connect.setDisable(true));

        // reset action target error message
        splash.actionTarget.setText("");

        serverIp = splash.serverIpField.getText();
        username = splash.usernameField.getText();

        if(serverIp.length() == 0){
            setErrorMessage("Server IP cannot be empty!");
            Platform.runLater(()->splash.connect.setDisable(false));
            return;
        }

        try{
            serverPort  = Integer.parseInt(splash.serverPortField.getText());
        }
        catch(NumberFormatException e){
            setErrorMessage("Invalid server port number!");
            Platform.runLater(()->splash.connect.setDisable(false));
            return;
        }

        if(username.length() == 0){
            setErrorMessage("Username cannot be empty");
            Platform.runLater(()->splash.connect.setDisable(false));
            return;
        }

        client = new Client(serverIp,serverPort,driver);

        if(!client.getConnectionStatus()){
            setErrorMessage("Cannot connect to server!");
            driver.getPrimaryStage().setOnCloseRequest(event -> client.closeConnection());
            Platform.runLater(()->splash.connect.setDisable(false));
            return;
        }
        else {
            driver.getPrimaryStage().setOnCloseRequest(event -> {
                client.sendMessage("QUIT");
                client.closeConnection();
            });

            client.sendMessage("IDEN " + username);
        }

        Platform.runLater(()->splash.connect.setDisable(false));

    }

    public void setErrorMessage(String error){
        splash.actionTarget.setText(error);
        splash.actionTarget.setFill(Color.FIREBRICK);
    }

    public String getUsername(){
        return username;
    }

    public Client getClient() {
        return client;
    }

}
