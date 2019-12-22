import g53sqm.chat.Server;
import g53sqm.chat.client.Client;
import javafx.stage.Stage;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

import static org.junit.Assert.*;

public class TestClient {
    private Stage primaryStage;
    private Server test_server;
    private int test_port_no;
    private Thread test_server_thread;

    public class test_runnable implements Runnable{
        public void run() {

            System.out.println("Test server thread started");
            test_server.listen();
        }
    }

    private String userReceiveMessage(Socket user){
        String text = "";
        try{
            InputStreamReader is_reader =new InputStreamReader(user.getInputStream());
            BufferedReader b_reader = new BufferedReader(is_reader);
            text = b_reader.readLine();
        }catch(IOException e) {
            e.printStackTrace();
        }

        return text;
    }

    private Socket createMockUsers(String username){
        Socket user = null;
        try{
            user = new Socket("localhost",test_port_no);
            userEnterCommandAndText(user, "IDEN " + username);
        }catch (IOException e) {
            Assert.fail("Mock user setup failed");
        }
        return user;
    }

    private void userEnterCommandAndText(Socket user, String text){
        try{
            PrintWriter user_out = new PrintWriter(user.getOutputStream(), true);
            user_out.println(text);
            Thread.sleep(1000);
        }catch(IOException|InterruptedException ie) {
            Assert.fail("Failed to send command and text");
        }
    }

    @Before
    public void startServer(){

        test_server = new Server(0);
        test_port_no = test_server.getPortNo();
        Runnable runnable = new TestClient.test_runnable();
        test_server_thread = new Thread(runnable);

        // Set thread as Daemon to automatically terminate when JVM terminates
        test_server_thread.setDaemon(true);
        test_server_thread.start();


        // Sleep for 1 second for thread execution
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void random(){
        Socket user1 = createMockUsers("client");
        userEnterCommandAndText(user1,"HAIL test data");
    }

    @Test
    public void clientLogIn_and_SendBroadCast(){
        //listening socket to mock what another client will receive
        Socket listeningSocket = createMockUsers("socket1");
        userReceiveMessage(listeningSocket);//absorb welcome message

        //create client
        Client client = new Client("localhost", test_port_no);
        client.sendMessage("IDEN dan");
        client.sendMessage("HAIL data");

        String expected = "Broadcast from dan: data";
        String actual = userReceiveMessage(listeningSocket);
        assertEquals(expected,actual);
    }

    @Test
    public void clientNotSuccesfullyConnected(){
        Client client = new Client("localhost", 3);
        boolean status = client.getConnectionStatus();

        assertFalse(status);
    }

    @Test
    public void clientSuccesfullyConnected(){
        Client client = new Client("localhost", test_port_no);
        boolean status = client.getConnectionStatus();

        assertFalse(status);
    }
}