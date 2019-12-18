package g53sqm.chat.client;

import javafx.application.Platform;

public class ClientChatController {

    private ClientChatGUI chat;
    private Client client;
    private ClientDriverGUI driver;

    public void setView(ClientChatGUI chat){
        this.chat = chat;
    }

    public void setClient(Client client){
        this.client = client;
    }

    public void setDriver(ClientDriverGUI driver){
        this.driver = driver;
    }

    public void doSend(){
        String msg = chat.input.getText();

        Platform.runLater(()->chat.input.clear()); // clear the text area;

        if(msg.trim().length() == 0){
            return ;
        }

        client.sendMessage(msg);

        if(msg.equals("QUIT")){
            client.closeConnection();
            driver.getPrimaryStage().close();
        }
    }


    // add server response to the chat area
    public void appendChat(String msg){
        Platform.runLater(()->chat.chat.appendText(msg + '\n'));
    }

    public void clean(){
        Platform.runLater(()->chat.chat.clear());
    }
}
